package org.hypixelskyblockmods.packbridge.mixin;

import net.minecraft.server.packs.PackResources;
import org.hypixelskyblockmods.packbridge.BridgedPackResources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {
    "net.minecraft.server.packs.FilePackResources$FileResourcesSupplier",
    "net.minecraft.server.packs.PathPackResources$PathResourcesSupplier"
})
public abstract class PackSupplierMixin {
    @Inject(method = {"openPrimary", "openFull"}, at = @At("RETURN"), cancellable = true)
    private void packbridge$adapt(CallbackInfoReturnable<PackResources> callback) {
        callback.setReturnValue(BridgedPackResources.wrap(callback.getReturnValue()));
    }
}
