package org.hypixelskyblockmods.packbridge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.IoSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A per-pack, read-only view. Keeping the view inside each pack preserves stack priority. */
public final class BridgedPackResources extends AbstractPackResources {
    private static final Logger LOGGER = LoggerFactory.getLogger("PackBridge");
    private static final long CACHE_LIMIT = 16 * 1024 * 1024;
    private final PackResources delegate;
    private final byte[] metadata;
    private Map<Identifier, IoSupplier<InputStream>> adapted;
    private final LinkedHashMap<Identifier, byte[]> cache = new LinkedHashMap<>(16, 0.75f, true);
    private long cachedBytes;
    private boolean closed;

    private BridgedPackResources(PackResources delegate, byte[] metadata) {
        super(delegate.location());
        this.delegate = delegate;
        this.metadata = metadata;
    }

    public static PackResources wrap(PackResources original) {
        if (original == null || original instanceof BridgedPackResources
                || original.getNamespaces(PackType.CLIENT_RESOURCES).isEmpty()) return original;
        IoSupplier<InputStream> supplier = original.getRootResource(PACK_META);
        if (supplier == null) return original;
        try (InputStream input = supplier.get(); InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject pack = root.getAsJsonObject("pack");
            if (pack == null || !pack.has("pack_format")) return original;
            int format = pack.get("pack_format").getAsInt();
            if (format < 1 || format > 4) return original;
            var current = SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES);
            JsonArray supported = new JsonArray();
            supported.add(current.major());
            supported.add(current.minor());
            pack.remove("pack_format");
            pack.remove("supported_formats");
            pack.add("min_format", supported);
            pack.add("max_format", supported.deepCopy());
            // The old overlay format cannot express the current minor-version range.
            root.remove("overlays");
            return new BridgedPackResources(original, root.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException exception) {
            LOGGER.debug("Leaving unreadable pack {} unchanged", original.packId(), exception);
            return original;
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && elements[0].equals(PACK_META)) return () -> new ByteArrayInputStream(metadata);
        return delegate.getRootResource(elements);
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, Identifier id) {
        if (type == PackType.CLIENT_RESOURCES) {
            IoSupplier<InputStream> converted = adaptations().get(id);
            if (converted != null) return converted;
        }
        return delegate.getResource(type, id);
    }

    @Override
    public void listResources(PackType type, String namespace, String prefix, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) {
            delegate.listResources(type, namespace, prefix, output);
            return;
        }
        Map<Identifier, IoSupplier<InputStream>> resources = new LinkedHashMap<>();
        delegate.listResources(type, namespace, prefix, resources::put);
        // Match directory boundaries rather than also matching e.g. textures/item_extra.
        adaptations().forEach((id, supplier) -> {
            if (id.getNamespace().equals(namespace) && (prefix.isEmpty()
                    || id.getPath().startsWith(prefix.endsWith("/") ? prefix : prefix + "/"))) {
                resources.put(id, supplier);
            }
        });
        resources.forEach(output);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return delegate.getNamespaces(type);
    }

    private synchronized Map<Identifier, IoSupplier<InputStream>> adaptations() {
        if (closed) return Map.of();
        if (adapted != null) return adapted;
        Map<Identifier, IoSupplier<InputStream>> raw = new LinkedHashMap<>();
        for (String namespace : delegate.getNamespaces(PackType.CLIENT_RESOURCES)) {
            delegate.listResources(PackType.CLIENT_RESOURCES, namespace, "textures", raw::put);
            delegate.listResources(PackType.CLIENT_RESOURCES, namespace, "models", raw::put);
        }
        Map<Identifier, IoSupplier<InputStream>> result = new LinkedHashMap<>();
        // Sorting makes duplicate historic aliases resolve identically for zip and directory packs.
        raw.keySet().stream().sorted(java.util.Comparator.comparing(Identifier::toString)).forEach(source -> {
            String path = source.getPath();
            String modern = TextureNames.modernPath(path);
            if (path.startsWith("models/item/") || path.startsWith("models/block/")) {
                modern = TextureNames.modernPath(path.replaceFirst("models/", "textures/")).replaceFirst("textures/", "models/");
            }
            Identifier target = Identifier.fromNamespaceAndPath(source.getNamespace(), modern);
            IoSupplier<InputStream> supplier = raw.get(source);
            if (!target.equals(source) && !raw.containsKey(target)) result.putIfAbsent(target, supplier);
            if (path.startsWith("models/") && path.endsWith(".json")) {
                IoSupplier<InputStream> rewritten = generated(target, () -> rewriteModel(supplier));
                if (target.equals(source) || !raw.containsKey(target)) result.put(target, rewritten);
                result.put(source, generated(source, () -> rewriteModel(supplier)));
            }
        });
        for (String namespace : delegate.getNamespaces(PackType.CLIENT_RESOURCES)) {
            addSprites(namespace, raw, result);
            if (namespace.equals("minecraft")) addChests(namespace, raw, result);
        }
        adapted = Map.copyOf(result);
        return adapted;
    }

    private void addSprites(String namespace, Map<Identifier, IoSupplier<InputStream>> raw,
                            Map<Identifier, IoSupplier<InputStream>> result) {
        Map<String, Boolean> validSheets = new HashMap<>();
        SpriteSlices.ALL.forEach((path, slice) -> {
            Identifier target = Identifier.fromNamespaceAndPath(namespace, path);
            IoSupplier<InputStream> source = raw.get(Identifier.fromNamespaceAndPath(namespace, slice.sheet()));
            if (source == null || raw.containsKey(target)) return;
            boolean valid = validSheets.computeIfAbsent(slice.sheet(), ignored -> validImage(source, slice.sheetWidth(), slice.sheetHeight()));
            if (!valid) return;
            result.put(target, generated(target, () -> ImageTransforms.png(ImageTransforms.crop(ImageTransforms.read(source.get()), slice))));
            if (path.startsWith("textures/gui/sprites/widget/button")) {
                Identifier meta = Identifier.fromNamespaceAndPath(namespace, path + ".mcmeta");
                if (!raw.containsKey(meta)) result.put(meta, bytes("{\"gui\":{\"scaling\":{\"type\":\"nine_slice\",\"width\":200,\"height\":20,\"border\":2}}}"));
            }
        });
    }

    private void addChests(String namespace, Map<Identifier, IoSupplier<InputStream>> raw,
                           Map<Identifier, IoSupplier<InputStream>> result) {
        for (String kind : new String[]{"normal", "trapped", "christmas", "ender"}) {
            String base = "textures/entity/chest/" + kind;
            Identifier single = Identifier.fromNamespaceAndPath(namespace, base + ".png");
            IoSupplier<InputStream> singleSource = raw.get(single);
            boolean hasModernHalf = raw.containsKey(Identifier.fromNamespaceAndPath(namespace, base + "_left.png"))
                    || raw.containsKey(Identifier.fromNamespaceAndPath(namespace, base + "_right.png"));
            if (!hasModernHalf && singleSource != null && validImage(singleSource, 64, 64)) {
                result.put(single, generated(single, () -> ImageTransforms.png(ImageTransforms.chest(ImageTransforms.read(singleSource.get()), "single"))));
            }
            IoSupplier<InputStream> doubleSource = raw.get(Identifier.fromNamespaceAndPath(namespace, base + "_double.png"));
            if (doubleSource == null || !validImage(doubleSource, 128, 64)) continue;
            for (String half : new String[]{"left", "right"}) {
                Identifier target = Identifier.fromNamespaceAndPath(namespace, base + "_" + half + ".png");
                if (!raw.containsKey(target)) result.put(target, generated(target,
                        () -> ImageTransforms.png(ImageTransforms.chest(ImageTransforms.read(doubleSource.get()), half))));
            }
        }
    }

    private boolean validImage(IoSupplier<InputStream> supplier, int width, int height) {
        try {
            var image = ImageTransforms.read(supplier.get());
            int scale = image.getWidth() / width;
            return scale > 0 && image.getWidth() == scale * width && image.getHeight() == scale * height;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Skipping an unreadable conversion texture in {}", packId());
            return false;
        }
    }

    private byte[] rewriteModel(IoSupplier<InputStream> supplier) throws IOException {
        byte[] original;
        try (InputStream input = supplier.get()) { original = input.readAllBytes(); }
        try {
            JsonObject model = JsonParser.parseString(new String(original, StandardCharsets.UTF_8)).getAsJsonObject();
            if (model.has("parent") && model.get("parent").isJsonPrimitive()) {
                model.addProperty("parent", TextureNames.modernReference(model.get("parent").getAsString()));
            }
            JsonObject textures = model.getAsJsonObject("textures");
            if (textures != null) {
                for (var entry : textures.entrySet()) {
                    JsonElement value = entry.getValue();
                    if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
                        entry.setValue(new com.google.gson.JsonPrimitive(TextureNames.modernReference(value.getAsString())));
                    }
                }
            }
            return model.toString().getBytes(StandardCharsets.UTF_8);
        } catch (RuntimeException exception) {
            return original;
        }
    }

    private static IoSupplier<InputStream> bytes(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        return () -> new ByteArrayInputStream(data);
    }

    private IoSupplier<InputStream> generated(Identifier id, IoSupplier<byte[]> factory) {
        return () -> {
            synchronized (this) {
                if (closed) throw new IOException("Resource pack is closed");
                byte[] data = cache.get(id);
                if (data == null) {
                    data = factory.get();
                    if (data.length <= CACHE_LIMIT) {
                        while (cachedBytes + data.length > CACHE_LIMIT && !cache.isEmpty()) {
                            var entry = cache.entrySet().iterator();
                            cachedBytes -= entry.next().getValue().length;
                            entry.remove();
                        }
                        cache.put(id, data);
                        cachedBytes += data.length;
                    }
                }
                return new ByteArrayInputStream(data);
            }
        };
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        cache.clear();
        cachedBytes = 0;
        adapted = null;
        delegate.close();
    }
}
