package org.hypixelskyblockmods.packbridge;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PackBridgeTest {
    @TempDir Path temp;

    @BeforeAll static void initialize() {
        SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    static PackLocationInfo location(String name) {
        return new PackLocationInfo(name, Component.literal(name), PackSource.DEFAULT, Optional.empty());
    }

    private Path folder(int format) throws Exception {
        Path folder = Files.createTempDirectory(temp, "pack-");
        Files.writeString(folder.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":" + format
                + ",\"description\":\"Fixture\"},\"language\":{\"test\":{\"name\":\"test\",\"region\":\"test\",\"bidirectional\":false}}}");
        Files.createDirectories(folder.resolve("assets/minecraft/textures/items"));
        return folder;
    }

    static void write(Path folder, String path, byte[] bytes) throws Exception {
        Path file = folder.resolve("assets/minecraft/" + path);
        Files.createDirectories(file.getParent());
        Files.write(file, bytes);
    }

    private PackResources open(Path folder) {
        return BridgedPackResources.wrap(new PathPackResources.PathResourcesSupplier(folder).openPrimary(location("fixture")));
    }

    static byte[] read(PackResources pack, String path) throws Exception {
        var supplier = pack.getResource(PackType.CLIENT_RESOURCES, Identifier.withDefaultNamespace(path));
        assertNotNull(supplier, path);
        try (InputStream input = supplier.get()) { return input.readAllBytes(); }
    }

    static BufferedImage coordinates(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) for (int x = 0; x < width; x++) image.setRGB(x, y, 0xff000000 | x << 12 | y);
        return image;
    }

    @Test void folderTexturesAndAnimationAreExposedToAtlasEnumeration() throws Exception {
        Path folder = folder(1);
        write(folder, "textures/items/gold_sword.png", new byte[]{1, 2, 3});
        write(folder, "textures/items/gold_sword.png.mcmeta", "{\"animation\":{\"frametime\":2}}".getBytes(StandardCharsets.UTF_8));
        write(folder, "textures/blocks/planks_oak.png", new byte[]{4});
        try (PackResources pack = open(folder)) {
            assertInstanceOf(BridgedPackResources.class, pack);
            assertArrayEquals(new byte[]{1, 2, 3}, read(pack, "textures/item/golden_sword.png"));
            assertArrayEquals(new byte[]{4}, read(pack, "textures/block/oak_planks.png"));
            assertTrue(new String(read(pack, "textures/item/golden_sword.png.mcmeta"), StandardCharsets.UTF_8).contains("frametime"));
            Map<Identifier, net.minecraft.server.packs.resources.IoSupplier<InputStream>> listed = new HashMap<>();
            pack.listResources(PackType.CLIENT_RESOURCES, "minecraft", "textures/item", listed::put);
            assertTrue(listed.containsKey(Identifier.withDefaultNamespace("textures/item/golden_sword.png")));
            listed.clear();
            pack.listResources(PackType.CLIENT_RESOURCES, "minecraft", "textures/item_extra", listed::put);
            assertTrue(listed.isEmpty());
        }
    }

    @Test void metadataUsesTheCurrentFormatAndKeepsTheDescription() throws Exception {
        try (PackResources pack = open(folder(1))) {
            var metadata = pack.getMetadataSection(PackMetadataSection.CLIENT_TYPE);
            assertNotNull(metadata);
            assertEquals("Fixture", metadata.description().getString());
            assertTrue(metadata.supportedFormats().isValueInRange(SharedConstants.getCurrentVersion().packVersion(PackType.CLIENT_RESOURCES)));
            try (InputStream input = pack.getRootResource("pack.mcmeta").get()) {
                assertTrue(JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject().has("language"));
            }
        }
    }

    @Test void zipPacksWorkAndOriginalFilesStayUntouched() throws Exception {
        Path zip = temp.resolve("fixture.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (var entry : Map.of("pack.mcmeta", "{\"pack\":{\"pack_format\":1,\"description\":\"Zip\"}}".getBytes(StandardCharsets.UTF_8),
                    "assets/minecraft/textures/items/apple.png", new byte[]{7}).entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        byte[] before = Files.readAllBytes(zip);
        try (PackResources pack = BridgedPackResources.wrap(new FilePackResources.FileResourcesSupplier(zip).openPrimary(location("zip")))) {
            assertArrayEquals(new byte[]{7}, read(pack, "textures/item/apple.png"));
        }
        assertArrayEquals(before, Files.readAllBytes(zip));
    }

    @Test void modernPathsWinOverOldAliasesWithinAPack() throws Exception {
        Path folder = folder(1);
        write(folder, "textures/items/gold_sword.png", new byte[]{1});
        write(folder, "textures/item/golden_sword.png", new byte[]{2});
        try (PackResources pack = open(folder)) {
            assertArrayEquals(new byte[]{2}, read(pack, "textures/item/golden_sword.png"));
        }
    }

    @Test void higherPackPriorityWinsEvenForAdaptedAssets() throws Exception {
        Path lower = folder(1), higher = folder(1);
        write(lower, "textures/items/apple.png", new byte[]{1});
        write(higher, "textures/items/apple.png", new byte[]{2});
        try (PackResources lowPack = open(lower); PackResources highPack = open(higher)) {
            var manager = new FallbackResourceManager(PackType.CLIENT_RESOURCES, "minecraft");
            manager.push(lowPack);
            manager.push(highPack);
            var resource = manager.getResource(Identifier.withDefaultNamespace("textures/item/apple.png")).orElseThrow();
            try (InputStream input = resource.open()) { assertArrayEquals(new byte[]{2}, input.readAllBytes()); }
        }
    }

    @Test void spriteSlicesPreserveHighResolutionPixelsAndModernOverrides() throws Exception {
        Path folder = folder(1);
        BufferedImage sheet = coordinates(512, 512);
        write(folder, "textures/gui/icons.png", ImageTransforms.png(sheet));
        write(folder, "textures/gui/sprites/hud/armor_full.png", new byte[]{8});
        try (PackResources pack = open(folder)) {
            BufferedImage heart = ImageIO.read(new ByteArrayInputStream(read(pack, "textures/gui/sprites/hud/heart/full.png")));
            assertEquals(18, heart.getWidth());
            assertEquals(sheet.getRGB(104, 0), heart.getRGB(0, 0));
            assertEquals(sheet.getRGB(121, 17), heart.getRGB(17, 17));
            assertArrayEquals(new byte[]{8}, read(pack, "textures/gui/sprites/hud/armor_full.png"));
        }
    }

    @Test void malformedSheetsAreSkippedAndModernPacksAreUnchanged() throws Exception {
        Path old = folder(1);
        write(old, "textures/gui/icons.png", new byte[]{0, 1});
        try (PackResources pack = open(old)) {
            assertNull(pack.getResource(PackType.CLIENT_RESOURCES, Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png")));
        }
        Path modern = folder(80);
        try (PackResources raw = new PathPackResources.PathResourcesSupplier(modern).openPrimary(location("modern"))) {
            assertSame(raw, BridgedPackResources.wrap(raw));
        }
    }

    @Test void chestFacesRotateAndSplitAtNativeAndHighResolution() throws Exception {
        for (int scale : new int[]{1, 2, 4}) {
            BufferedImage single = coordinates(64 * scale, 64 * scale);
            BufferedImage remapped = ImageTransforms.chest(single, "single");
            assertEquals(single.getRGB(14 * scale, 14 * scale - 1), remapped.getRGB(28 * scale, 0));
            assertEquals(single.getRGB(28 * scale - 1, 19 * scale - 1), remapped.getRGB(42 * scale, 14 * scale));
            BufferedImage combined = coordinates(128 * scale, 64 * scale);
            BufferedImage left = ImageTransforms.chest(combined, "left");
            BufferedImage right = ImageTransforms.chest(combined, "right");
            assertEquals(64 * scale, left.getWidth());
            assertEquals(combined.getRGB(59 * scale, 14 * scale - 1), left.getRGB(14 * scale, 0));
            assertEquals(combined.getRGB(44 * scale, 14 * scale - 1), right.getRGB(14 * scale, 0));
            assertEquals(combined.getRGB(73 * scale - 1, 43 * scale - 1), left.getRGB(14 * scale, 33 * scale));
        }
    }

    @Test void doubleChestsAreDiscoverableAndExplicitModernHalvesArePreserved() throws Exception {
        Path folder = folder(1);
        write(folder, "textures/entity/chest/normal_double.png", ImageTransforms.png(coordinates(128, 64)));
        write(folder, "textures/entity/chest/normal_left.png", new byte[]{9});
        try (PackResources pack = open(folder)) {
            assertArrayEquals(new byte[]{9}, read(pack, "textures/entity/chest/normal_left.png"));
            assertEquals(64, ImageIO.read(new ByteArrayInputStream(read(pack, "textures/entity/chest/normal_right.png"))).getWidth());
            Map<Identifier, net.minecraft.server.packs.resources.IoSupplier<InputStream>> listed = new HashMap<>();
            pack.listResources(PackType.CLIENT_RESOURCES, "minecraft", "textures/entity/chest", listed::put);
            assertTrue(listed.containsKey(Identifier.withDefaultNamespace("textures/entity/chest/normal_right.png")));
        }
    }

    @Test void modelsRewritePathsAndKeepDisplaySettings() throws Exception {
        Path folder = folder(1);
        write(folder, "models/item/gold_sword.json", "{\"parent\":\"builtin/generated\",\"textures\":{\"layer0\":\"minecraft:items/gold_sword\",\"particle\":\"#layer0\"},\"display\":{\"gui\":{\"scale\":[1,1,1]}}}".getBytes(StandardCharsets.UTF_8));
        try (PackResources pack = open(folder)) {
            var model = JsonParser.parseString(new String(read(pack, "models/item/golden_sword.json"), StandardCharsets.UTF_8)).getAsJsonObject();
            assertEquals("item/generated", model.get("parent").getAsString());
            assertEquals("minecraft:item/golden_sword", model.getAsJsonObject("textures").get("layer0").getAsString());
            assertEquals("#layer0", model.getAsJsonObject("textures").get("particle").getAsString());
            assertTrue(model.has("display"));
        }
    }

    @Test void reloadReopensTexturesAndClosedGeneratedStreamsCannotBeRead() throws Exception {
        Path folder = folder(1);
        write(folder, "textures/gui/icons.png", ImageTransforms.png(coordinates(256, 256)));
        PackResources first = open(folder);
        var supplier = first.getResource(PackType.CLIENT_RESOURCES, Identifier.withDefaultNamespace("textures/gui/sprites/hud/crosshair.png"));
        assertNotNull(supplier);
        first.close();
        assertThrows(java.io.IOException.class, supplier::get);
        BufferedImage replacement = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        replacement.setRGB(0, 0, 0xff123456);
        write(folder, "textures/gui/icons.png", ImageTransforms.png(replacement));
        try (PackResources second = open(folder)) {
            assertEquals(0xff123456, ImageIO.read(new ByteArrayInputStream(read(second, "textures/gui/sprites/hud/crosshair.png"))).getRGB(0, 0));
        }
    }
}
