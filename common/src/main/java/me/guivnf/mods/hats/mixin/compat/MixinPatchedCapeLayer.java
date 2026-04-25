package me.guivnf.mods.hats.mixin.compat;

import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "yesman.epicfight.client.renderer.patched.layer.PatchedCapeLayer", remap = false)
public class MixinPatchedCapeLayer
{
    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void hats$skipCapeSimulationInPreview(CallbackInfo ci)
    {
        if (EpicFightCompat.GUI_PREVIEW_ACTIVE) {
            ci.cancel();
        }
    }
}
