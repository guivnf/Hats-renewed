package me.guivnf.mods.hats.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import me.guivnf.mods.hats.client.compat.GeckoLibCompat;
import me.guivnf.mods.hats.client.render.HatRenderer;
import me.guivnf.mods.hats.client.render.LayerHat;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementInfo;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.HashSet;
import java.util.Set;

@Mixin(EntityRenderDispatcher.class)
public class MixinEntityRenderDispatcher
{
    private static final Logger LOGGER = LoggerFactory.getLogger("HatsDebug");
    private static final Set<String> LOGGED_TYPES = new HashSet<>();
    private static long lastEfLogMs = 0L;

    private static final float EF_HEAD_CROWN_OFFSET = 7.75f / 16.0f;

    private static final float Z_FIGHT_INFLATE  = 1.012f;
    private static final float HEAD_HALF_OFFSET = 4.0f / 16.0f;

    @Inject(method = "render", at = @At("HEAD"))
    private void hats$resetFlag(Entity entity, double camX, double camY, double camZ,
            float yRot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, CallbackInfo ci)
    {
        LayerHat.HAT_RENDERED_THIS_ENTITY.set(false);
    }

    @Inject(method = "render",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/entity/EntityRenderer;render(Lnet/minecraft/world/entity/Entity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                     shift = At.Shift.AFTER))
    private void hats$fallbackRender(Entity entity, double camX, double camY, double camZ,
            float yRot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
            int packedLight, CallbackInfo ci)
    {
        if (LayerHat.HAT_RENDERED_THIS_ENTITY.get()) return;
        if (!(entity instanceof LivingEntity living)) return;

        HatPart hat = ClientHatCache.getEntityHat(entity.getUUID());
        if (hat == null || !hat.isShowing) return;

        if (living.isInvisible() && !HatsMod.getConfig().renderOnInvisible) return;

        HatPlacementInfo placement = HatPlacementRegistry.get(living);

        boolean firstLog = LOGGED_TYPES.add(entity.getClass().getName());

        float bodyYaw   = Mth.rotLerp(partialTick, living.yBodyRotO, living.yBodyRot);
        float headYaw   = Mth.rotLerp(partialTick, living.yHeadRotO, living.yHeadRot);
        float headPitch = Mth.lerp(partialTick, living.xRotO, living.getXRot());

        poseStack.pushPose();

        boolean isGecko      = GeckoLibCompat.PRESENT  && GeckoLibCompat.isGeckoLibEntity(living);
        boolean inGuiPreview = LayerHat.RENDERING_HAT_GUI_PREVIEW.get();
        boolean isEpicFight  = !inGuiPreview && EpicFightCompat.PRESENT && EpicFightCompat.isEpicFightEntity(living);

        float bbScale    = Mth.clamp(entity.getBbWidth() / 0.6f, 0.2f, 3.0f);
        float finalScale = isEpicFight ? placement.scale : placement.scale * bbScale;

        // if (firstLog) {
        //     LOGGER.info("[Hats] FALLBACK render for entity={} isGecko={} isEpicFight={} bodyYaw={} headYaw={} headPitch={}",
        //         entity.getClass().getSimpleName(), isGecko, isEpicFight, bodyYaw, headYaw, headPitch);
        // }

        if (isEpicFight) {
            org.joml.Matrix4f headMat = EpicFightCompat.getHeadTransform(living, partialTick);
            // long now = System.currentTimeMillis();
            // if (now - lastEfLogMs > 2000L) {
            //     lastEfLogMs = now;
            //     LOGGER.info("[Hats-EF] headMatNull={} bodyYaw={} headYaw={} headPitch={} bbHeight={}",
            //         headMat == null, bodyYaw, headYaw, headPitch, entity.getBbHeight());
            //     if (headMat != null) {
            //         LOGGER.info("[Hats-EF] headMat tx={} ty={} tz={}",
            //             headMat.m30(), headMat.m31(), headMat.m32());
            //         LOGGER.info("[Hats-EF] headMat col0=[{},{},{}] col1=[{},{},{}] col2=[{},{},{}]",
            //             headMat.m00(), headMat.m10(), headMat.m20(),
            //             headMat.m01(), headMat.m11(), headMat.m21(),
            //             headMat.m02(), headMat.m12(), headMat.m22());
            //     }
            // }
            if (headMat != null) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f));
                if (!EpicFightCompat.applyEFModelMatrix(poseStack, living, partialTick)) {
                    poseStack.mulPose(Axis.YP.rotationDegrees(-bodyYaw));
                }
                if (living.isCrouching()) poseStack.translate(0.0, 0.15, 0.0);
                poseStack.last().pose().mul(headMat);
                poseStack.last().normal().mul(new org.joml.Matrix3f(headMat));
                poseStack.scale(-1f, -1f, 1f);
                poseStack.translate(0.0, -EF_HEAD_CROWN_OFFSET, 0.0);
                poseStack.translate(0.0,  HEAD_HALF_OFFSET, 0.0);
                poseStack.scale(Z_FIGHT_INFLATE, Z_FIGHT_INFLATE, Z_FIGHT_INFLATE);
                poseStack.translate(0.0, -HEAD_HALF_OFFSET, 0.0);
            } else {
                poseStack.translate(0.0, entity.getBbHeight(), 0.0);
                poseStack.scale(-1f, -1f, 1f);
                poseStack.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
            }
        } else if (isGecko) {
            org.joml.Matrix4f headMatrix = GeckoLibCompat.getHeadMatrix(living);
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - bodyYaw));
            if (headMatrix != null) {
                poseStack.last().pose().mul(headMatrix);
            } else {
                poseStack.translate(0.0, living.getEyeHeight() + 0.1f, 0.0);
                poseStack.mulPose(Axis.YP.rotationDegrees(-(headYaw - bodyYaw)));
                poseStack.mulPose(Axis.XP.rotationDegrees(-headPitch));
            }
            poseStack.scale(-1f, -1f, 1f);
        } else {
            poseStack.translate(0.0, entity.getBbHeight(), 0.0);
            poseStack.scale(-1f, -1f, 1f);
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - 2f * bodyYaw + headYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(headPitch));
        }

        HatRenderer.render(poseStack, bufferSource, packedLight,
            OverlayTexture.pack(0, 10),
            hat, placement.offsetX, placement.offsetY, placement.offsetZ,
            placement.rotX, placement.rotY, placement.rotZ, finalScale);
        poseStack.popPose();
    }
}
