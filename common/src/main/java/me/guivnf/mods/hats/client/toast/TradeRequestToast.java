package me.guivnf.mods.hats.client.toast;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;

public class TradeRequestToast
        implements Toast
{
    private static final long DISPLAY_TIME_MS = 7000L;
    private static final int W = 200;
    private static final int H = 32;

    private final String senderName;
    private long startTime = -1;

    public TradeRequestToast(String senderName)
    {
        this.senderName = senderName;
    }

    @Override public int width()  { return W; }
    @Override public int height() { return H; }

    @Override
    public Visibility render(GuiGraphics g, ToastComponent toastComponent, long timeSinceLastVisible)
    {
        if (startTime < 0) startTime = timeSinceLastVisible;
        long elapsed = timeSinceLastVisible - startTime;

        g.fill(0,     0,     W,     H,     0xFF202020);
        g.fill(1,     1,     W - 1, H - 1, 0xFF2B2B2B);

        Minecraft mc = toastComponent.getMinecraft();

        g.drawString(mc.font, Component.literal("Hat Trade request by:").withStyle(ChatFormatting.GOLD),
            6, 6, 0xFFAA00, false);

        Component nameComp = Component.literal(senderName)
            .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD);
        g.drawString(mc.font, nameComp, 6, 18, 0xFF55FF55, false);

        int barY = H - 3;
        int barX = 4;
        int barW = W - 8;
        g.fill(barX, barY, barX + barW, barY + 2, 0xFF444444);
        float frac = 1f - Math.min(1f, (float) elapsed / DISPLAY_TIME_MS);
        int filled = (int)(barW * frac);
        if (filled > 0) {
            g.fill(barX, barY, barX + filled, barY + 2, 0xFFFFAA00);
        }

        return elapsed >= DISPLAY_TIME_MS ? Visibility.HIDE : Visibility.SHOW;
    }

    public static void show(String senderName)
    {
        Minecraft.getInstance().getToasts().addToast(new TradeRequestToast(senderName));
    }
}
