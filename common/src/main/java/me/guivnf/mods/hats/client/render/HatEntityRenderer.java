package me.guivnf.mods.hats.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.guivnf.mods.hats.common.entity.HatEntity;
import me.guivnf.mods.hats.common.hat.HatPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class HatEntityRenderer extends EntityRenderer<HatEntity>
{
    public HatEntityRenderer(EntityRendererProvider.Context context)
    {
        super(context);
    }

    @Override
    public void render(HatEntity entity, float entityYaw, float partialTick,
            PoseStack poseStack, MultiBufferSource buffers, int light)
    {
        HatPart hat = entity.getHatPart();
        if (hat == null) return;

        float rotY = Mth.lerp(partialTick, entity.lastRotY, entity.rotY);
        float rotX = Mth.lerp(partialTick, entity.lastRotX, entity.rotX);

        rotX = Mth.clamp(rotX % 360f, -50f, 50f);

        poseStack.pushPose();
        poseStack.translate(0.0, 0.5, 0.0);

        poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
        poseStack.mulPose(Axis.ZP.rotationDegrees(rotX));
        poseStack.mulPose(Axis.XP.rotationDegrees(180f));

        hat.isShowing = true;
        HatRenderer.render(poseStack, buffers, light,
            net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
            hat, 0, 0, 0, 0, 0, 0, 1f);

        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(HatEntity entity)
    {
        return new ResourceLocation("hats", "textures/entity/hat.png");
    }
}
