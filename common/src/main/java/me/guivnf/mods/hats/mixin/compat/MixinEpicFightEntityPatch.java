package me.guivnf.mods.hats.mixin.compat;

import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch", remap = false)
public class MixinEpicFightEntityPatch
{
    @Inject(method = "overrideRender", at = @At("HEAD"), cancellable = true, remap = false)
    private void hats$guiPreviewDisableEF(CallbackInfoReturnable<Boolean> cir)
    {
        if (EpicFightCompat.GUI_PREVIEW_ACTIVE) {
            cir.setReturnValue(false);
        }
    }
}
