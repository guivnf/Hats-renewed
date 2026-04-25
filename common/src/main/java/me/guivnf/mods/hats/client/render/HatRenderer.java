package me.guivnf.mods.hats.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatModelData;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HatRenderer
{
    private static boolean forceNoCull = false;

    public static void setForceNoCull(boolean value) { forceNoCull = value; }

    public static void render(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay,
            HatPart part, float offsetX, float offsetY, float offsetZ,
            float rotX, float rotY, float rotZ, float scale)
    {
        HatDefinition def = HatRegistry.get(part.getRegistryKey());
        if (def == null || !part.isShowing) return;

        poseStack.pushPose();

        poseStack.translate(offsetX / 16f, offsetY / 16f, offsetZ / 16f);
        if (rotZ != 0f) poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotZ));
        if (rotY != 0f) poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotY));
        if (rotX != 0f) poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotX));
        if (scale != 1f) poseStack.scale(scale, scale, scale);

        float[] rgba = resolveColour(part);

        Set<String> hiddenParts = new HashSet<>();
        Map<String, String> boneTexOverrides = def.meta.boneTextureOverrides;
        Map<String, float[]> boneColorOverrides = def.meta.boneColorOverrides;

        for (HatPart accessory : part.accessories) {
            if (!accessory.isShowing) continue;
            HatDefinition accDef = HatRegistry.get(accessory.getRegistryKey());
            if (accDef != null) {
                hiddenParts.addAll(accDef.meta.hideParentParts);
            }
        }

        ResourceLocation baseTexture = HatTextureManager.getTexture(def, part);
        if (baseTexture != null) {
            HatModelData model = def.modelData;
            boolean cull = def.meta.cull != null
                ? def.meta.cull
                : model.format == HatModelData.Format.GEO_JSON;
            if (forceNoCull) cull = false;
            boolean noCull = !cull;

            if (boneTexOverrides.isEmpty() && boneColorOverrides.isEmpty()) {
                RenderType renderType = part.enchanted
                    ? RenderType.entityGlint()
                    : (noCull ? HatRenderTypes.hatTranslucent(baseTexture) : HatRenderTypes.hatTranslucentCull(baseTexture));
                VertexConsumer consumer = buffers.getBuffer(renderType);
                for (HatModelData.Bone root : model.roots) {
                    renderBone(root, model.format, poseStack, buffers, consumer,
                        baseTexture, part, light, overlay, rgba,
                        model.textureWidth, model.textureHeight, hiddenParts,
                        boneTexOverrides, boneColorOverrides, noCull);
                }
            } else {
                for (HatModelData.Bone root : model.roots) {
                    renderBone(root, model.format, poseStack, buffers, null,
                        baseTexture, part, light, overlay, rgba,
                        model.textureWidth, model.textureHeight, hiddenParts,
                        boneTexOverrides, boneColorOverrides, noCull);
                }
            }
        }

        for (HatPart accessory : part.accessories) {
            if (!accessory.isShowing) continue;
            render(poseStack, buffers, light, overlay, accessory, 0, 0, 0, 0, 0, 0, 1f);
        }

        poseStack.popPose();
    }

    private static void renderBone(HatModelData.Bone bone, HatModelData.Format format,
            PoseStack poseStack, MultiBufferSource buffers, VertexConsumer sharedConsumer,
            ResourceLocation baseTexture, HatPart part,
            int light, int overlay, float[] rgba,
            int texW, int texH, Set<String> hiddenParts,
            Map<String, String> boneTexOverrides, Map<String, float[]> boneColorOverrides,
            boolean noCull)
    {
        if (!bone.visible) return;
        if (hiddenParts.contains(bone.name)) return;

        poseStack.pushPose();

        float pivotX = bone.pivotX / 16f;
        float pivotY = bone.pivotY / 16f;
        float pivotZ = bone.pivotZ / 16f;

        if (format == HatModelData.Format.GEO_JSON) {
            pivotY = -pivotY;
        }

        poseStack.translate(pivotX, pivotY, pivotZ);

        float rotX = bone.rotX;
        float rotY = bone.rotY;
        float rotZ = bone.rotZ;

        if (format == HatModelData.Format.TBL) {
            poseStack.mulPose(Axis.ZP.rotation(rotZ));
            poseStack.mulPose(Axis.YP.rotation(rotY));
            poseStack.mulPose(Axis.XP.rotation(rotX));
        } else {
            poseStack.mulPose(Axis.ZP.rotationDegrees(rotZ));
            poseStack.mulPose(Axis.YP.rotationDegrees(rotY));
            poseStack.mulPose(Axis.XP.rotationDegrees(rotX));
        }

        if (!bone.cubes.isEmpty()) {
            float[] boneRgba = rgba;
            float[] colorOverride = boneColorOverrides.get(bone.name);
            if (colorOverride != null) {
                boneRgba = new float[]{
                    rgba[0] * colorOverride[0],
                    rgba[1] * colorOverride[1],
                    rgba[2] * colorOverride[2],
                    rgba[3] * colorOverride[3]
                };
            }

            VertexConsumer consumer = sharedConsumer;
            int boneTexW = (bone.texW > 0) ? bone.texW : texW;
            int boneTexH = (bone.texH > 0) ? bone.texH : texH;

            String texOverrideName = boneTexOverrides.get(bone.name);
            if (texOverrideName != null) {
                HatDefinition overrideDef = HatRegistry.get(texOverrideName);
                ResourceLocation overrideTex = overrideDef != null
                    ? HatTextureManager.getTexture(overrideDef, part)
                    : null;
                if (overrideTex != null) {
                    RenderType rt = part.enchanted
                        ? RenderType.entityGlint()
                        : (noCull ? HatRenderTypes.hatTranslucent(overrideTex) : HatRenderTypes.hatTranslucentCull(overrideTex));
                    consumer = buffers.getBuffer(rt);
                    boneTexW = overrideDef.modelData.textureWidth;
                    boneTexH = overrideDef.modelData.textureHeight;
                }
            }

            if (consumer == null) {
                RenderType rt = part.enchanted
                    ? RenderType.entityGlint()
                    : (noCull ? HatRenderTypes.hatTranslucent(baseTexture) : HatRenderTypes.hatTranslucentCull(baseTexture));
                consumer = buffers.getBuffer(rt);
            }

            for (HatModelData.Cube cube : bone.cubes) {
                renderCube(cube, format, poseStack, consumer, light, overlay, boneRgba,
                    bone.mirror || cube.mirror, boneTexW, boneTexH);
            }
        }

        for (HatModelData.Bone child : bone.children) {
            renderBone(child, format, poseStack, buffers, sharedConsumer,
                baseTexture, part, light, overlay, rgba,
                texW, texH, hiddenParts,
                boneTexOverrides, boneColorOverrides, noCull);
        }

        poseStack.popPose();
    }

    private static void renderCube(HatModelData.Cube cube, HatModelData.Format format,
            PoseStack poseStack, VertexConsumer consumer, int light, int overlay,
            float[] rgba, boolean mirror, int texW, int texH)
    {
        float ox = cube.originX / 16f;
        float oy = cube.originY / 16f;
        float oz = cube.originZ / 16f;
        float sx = cube.sizeX / 16f;
        float sy = cube.sizeY / 16f;
        float sz = cube.sizeZ / 16f;
        float inflateX = cube.inflateX / 16f;
        float inflateY = cube.inflateY / 16f;
        float inflateZ = cube.inflateZ / 16f;

        if (format == HatModelData.Format.GEO_JSON) {
            oy = -oy - sy;
        }

        // Track flat axes before expanding so CubeRenderer can fix UV/winding on the back face.
        boolean flatX = sx == 0f;
        boolean flatY = sy == 0f;
        boolean flatZ = sz == 0f;

        // Expand zero-thickness axes by a sub-pixel epsilon so coplanar faces have distinct depths.
        final float EPS = 5e-5f;
        if (flatX) { ox -= EPS; sx = EPS * 2f; }
        if (flatY) { oy -= EPS; sy = EPS * 2f; }
        if (flatZ) { oz -= EPS; sz = EPS * 2f; }

        // GEO_JSON Y-flip causes the "top" UV region to end up on y0 (visible from below).
        // Swap Y face UV so the correct region shows from above.
        boolean swapYFaceUV = (format == HatModelData.Format.GEO_JSON) && flatY;

        CubeRenderer.renderCube(poseStack, consumer, ox, oy, oz, sx, sy, sz,
            inflateX, inflateY, inflateZ,
            cube.texU, cube.texV, cube.sizeX, cube.sizeY, cube.sizeZ,
            texW, texH,
            light, overlay, rgba[0], rgba[1], rgba[2], rgba[3], mirror, flatX, flatY, flatZ, swapYFaceUV);
    }

    private static float[] resolveColour(HatPart part)
    {
        float r = 1f - part.colour[0];
        float g = 1f - part.colour[1];
        float b = 1f - part.colour[2];
        float a = part.colour[3] == 0f ? 1f : 1f - part.colour[3];
        return new float[]{r, g, b, a};
    }
}
