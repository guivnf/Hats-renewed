package me.guivnf.mods.hats.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public abstract class HatRenderTypes extends RenderType
{
    protected HatRenderTypes(String pName, VertexFormat pFormat, VertexFormat.Mode pMode,
            int pBufferSize, boolean pAffectsCrumbling, boolean pSortOnUpload,
            Runnable pSetupState, Runnable pClearState)
    {
        super(pName, pFormat, pMode, pBufferSize, pAffectsCrumbling, pSortOnUpload, pSetupState, pClearState);
    }

    private static final Function<ResourceLocation, RenderType> HAT_TRANSLUCENT =
            Util.memoize(texture ->
            {
                CompositeState state = CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEntityCutoutNoCullShader))
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setCullState(NO_CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false);
                return create("hat_entity_cutout_no_cull",
                        DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                        1536, true, false, state);
            });

    private static final Function<ResourceLocation, RenderType> HAT_TRANSLUCENT_CULL =
            Util.memoize(texture ->
            {
                CompositeState state = CompositeState.builder()
                        .setShaderState(new ShaderStateShard(GameRenderer::getRendertypeEntityCutoutShader))
                        .setTextureState(new TextureStateShard(texture, false, false))
                        .setTransparencyState(NO_TRANSPARENCY)
                        .setCullState(CULL)
                        .setLightmapState(LIGHTMAP)
                        .setOverlayState(OVERLAY)
                        .createCompositeState(false);
                return create("hat_entity_cutout",
                        DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
                        1536, true, false, state);
            });

    public static RenderType hatTranslucent(ResourceLocation texture)
    {
        return HAT_TRANSLUCENT.apply(texture);
    }

    public static RenderType hatTranslucentCull(ResourceLocation texture)
    {
        return HAT_TRANSLUCENT_CULL.apply(texture);
    }
}
