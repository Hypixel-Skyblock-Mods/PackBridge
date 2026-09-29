package org.hypixelskyblockmods.packbridge.smoke;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import org.hypixelskyblockmods.packbridge.BridgedPackResources;

/** Test-only Fabric entrypoint; never included in production JARs. */
public final class FabricSmoke implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        if (!Boolean.getBoolean("packbridge.smoke")) return;
        try {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            Path folder = Path.of("packbridge-smoke-fixture").toAbsolutePath();
            Files.createDirectories(folder.resolve("assets/minecraft/textures/items"));
            byte[] metadata = "{\"pack\":{\"pack_format\":1,\"description\":\"Fabric smoke\"}}".getBytes(StandardCharsets.UTF_8);
            Files.write(folder.resolve("pack.mcmeta"), metadata);
            Files.write(folder.resolve("assets/minecraft/textures/items/apple.png"), new byte[]{42});
            Path zip = folder.resolveSibling("packbridge-smoke-fixture.zip");
            try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
                output.putNextEntry(new ZipEntry("pack.mcmeta"));
                output.write(metadata);
                output.closeEntry();
                output.putNextEntry(new ZipEntry("assets/minecraft/textures/items/apple.png"));
                output.write(42);
                output.closeEntry();
            }
            for (Pack.ResourcesSupplier supplier : new Pack.ResourcesSupplier[]{
                    new PathPackResources.PathResourcesSupplier(folder),
                    new FilePackResources.FileResourcesSupplier(zip)}) {
                PackLocationInfo location = new PackLocationInfo("fixture", Component.literal("fixture"), PackSource.DEFAULT, Optional.empty());
                try (PackResources primary = supplier.openPrimary(location)) {
                    check(primary instanceof BridgedPackResources, "Primary supplier mixin was not applied");
                }
                Pack pack = Pack.readMetaAndCreate(location, supplier, PackType.CLIENT_RESOURCES,
                        new PackSelectionConfig(false, Pack.Position.TOP, false));
                check(pack != null, "Vanilla could not discover the adapted pack");
                check(pack.getCompatibility() == PackCompatibility.COMPATIBLE, "Adapted pack metadata is incompatible");
                try (PackResources full = pack.open()) {
                    check(full instanceof BridgedPackResources, "Full supplier mixin was not applied");
                    var apple = full.getResource(PackType.CLIENT_RESOURCES, Identifier.withDefaultNamespace("textures/item/apple.png"));
                    check(apple != null, "Adapted item texture missing");
                    try (InputStream input = apple.get()) { check(input.read() == 42, "Unexpected adapted texture"); }
                }
            }
            System.out.println("PACKBRIDGE FABRIC SMOKE PASSED: " + SharedConstants.getCurrentVersion().name());
            System.exit(0);
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
