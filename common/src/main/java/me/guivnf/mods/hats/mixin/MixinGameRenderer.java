package me.guivnf.mods.hats.mixin;

import me.guivnf.mods.hats.client.event.ClientEventHandler;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer {
    @Inject(method = "getFov", at = @At("HEAD"), cancellable = true)
    private void hats$overrideFov(Camera camera, float partialTick, boolean useFOVSetting,
            CallbackInfoReturnable<Double> cir) {
        float progress = ClientEventHandler.hatMenuProgressPrev
                + (ClientEventHandler.hatMenuProgress - ClientEventHandler.hatMenuProgressPrev) * partialTick;

        if (progress <= 0f)
            return;

        cir.setReturnValue(100.0);
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void hats$resetFrameCounter(net.minecraft.client.DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        me.guivnf.mods.hats.client.render.LayerHat.resetFrameCounter();
    }
}
