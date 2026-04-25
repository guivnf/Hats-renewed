package me.guivnf.mods.hats.client.toast;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.guivnf.mods.hats.client.render.HatRenderer;
import me.guivnf.mods.hats.common.hat.HatPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

public class HatToast
        implements Toast
{
    private static final long DISPLAY_TIME_MS = 5000L;
    private static final int W = 160;
    private static final int H = 32;

    private final HatPart hat;
    private final boolean isAccessory;
    private long startTime = -1;

    public HatToast(HatPart hat, boolean isAccessory)
    {
        this.hat = hat;
        this.isAccessory = isAccessory;
    }

    @Override
    public int width()  { return W; }

    @Override
    public int height() { return H; }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent toastComponent, long timeSinceLastVisible)
    {
        if (startTime < 0) startTime = timeSinceLastVisible;
        long elapsed = timeSinceLastVisible - startTime;

        graphics.fill(0, 0, W, H, 0xFFFFFFFF);
        graphics.fill(0, 0, W, 1,     0xFFCCCCCC);
        graphics.fill(0, H - 1, W, H, 0xFFAAAAAA);
        graphics.fill(0, 0, 1, H,     0xFFCCCCCC);
        graphics.fill(W - 1, 0, W, H, 0xFFAAAAAA);

        String titleKey = isAccessory ? "hats.toast.new_accessory" : "hats.toast.new_hat";
        graphics.drawString(toastComponent.getMinecraft().font,
            Component.translatable(titleKey), H + 4, 7, 0x333333, false);
        graphics.drawString(toastComponent.getMinecraft().font,
            Component.literal(hat.name), H + 4, 18, 0x555555, false);

        renderHatPreview(graphics, toastComponent.getMinecraft(), elapsed);

        return elapsed >= DISPLAY_TIME_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    private void renderHatPreview(GuiGraphics graphics, Minecraft mc, long elapsedMs)
    {
        HatPart renderPart = new HatPart(hat.name);
        renderPart.isShowing = true;

        PoseStack pose = graphics.pose();
        pose.pushPose();

        pose.translate(H / 2f, H / 2f, 100f);

        float yaw = (elapsedMs / 3000f) * 360f;
        pose.mulPose(Axis.YP.rotationDegrees(yaw));

        float scale = 14f;
        pose.scale(scale, scale, scale);

        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        RenderSystem.enableDepthTest();
        HatRenderer.setForceNoCull(true);
        HatRenderer.render(pose, buffers, 0xF000F0,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                renderPart, 0, 0, 0, 0, 0, 0, 1f);
        HatRenderer.setForceNoCull(false);
        buffers.endBatch();
        RenderSystem.disableDepthTest();

        pose.popPose();
    }

    public static void show(HatPart hat, boolean isAccessory)
    {
        Minecraft.getInstance().getToasts().addToast(new HatToast(hat, isAccessory));
    }
}
