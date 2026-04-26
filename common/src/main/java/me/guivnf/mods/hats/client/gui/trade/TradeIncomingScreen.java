package me.guivnf.mods.hats.client.gui.trade;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import me.guivnf.mods.hats.client.gui.HatPreviewRenderer;
import me.guivnf.mods.hats.client.gui.HatsScreen;
import me.guivnf.mods.hats.client.trade.ClientTradeState;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.packet.PacketRespondTradeOffer;
import me.guivnf.mods.hats.common.trade.TradeOffer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class TradeIncomingScreen
        extends Screen
{
    private static final int W = 300;
    private static final int H = 210;

    private static final int CELL_W = 44;
    private static final int CELL_H = 50;
    private static final int NAME_H = 10;
    private static final int ROW_H  = CELL_H + NAME_H;

    private final ClientTradeState.IncomingOffer offer;
    private int x, y;
    private int colY, colH;

    private int leftScroll  = 0;
    private int rightScroll = 0;

    public TradeIncomingScreen(ClientTradeState.IncomingOffer offer)
    {
        super(Component.literal("Trade offer from " + offer.senderName));
        this.offer = offer;
    }

    @Override
    protected void init()
    {
        x = (width - W) / 2;
        y = (height - H) / 2;

        addRenderableWidget(Button.builder(
            Component.literal("Accept").withStyle(ChatFormatting.GREEN), b -> respond(true))
            .bounds(x + W / 2 - 76, y + H - 22, 70, 18).build());

        addRenderableWidget(Button.builder(
            Component.literal("Decline").withStyle(ChatFormatting.RED), b -> respond(false))
            .bounds(x + W / 2 + 6, y + H - 22, 70, 18).build());

        addRenderableWidget(Button.builder(
            Component.literal("Back"), b -> back())
            .bounds(x + 6, y + H - 22, 50, 18).build());

        colY = y + 36;
        colH = (y + H - 40) - colY;
    }

    private void respond(boolean accept)
    {
        NetworkManager.sendToServer(HatsNetwork.RESPOND_TRADE_OFFER,
            PacketRespondTradeOffer.encode(offer.offerId, accept));
        ClientTradeState.removeIncomingOffer(offer.offerId);
        HatsScreen.open();
    }

    private void back()
    {
        HatsScreen.open();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(g);

        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF555555);
        g.fill(x,     y,     x + W,     y + H,     0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + W - 1, y + 2,     0xFFFFFFFF);

        Component header = Component.literal("Offer from ")
            .append(Component.literal(offer.senderName).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
        g.drawString(font, header, x + (W - font.width(header)) / 2, y + 6, 0xFF222222, false);

        long remaining = Math.max(0L, TradeOffer.EXPIRY_MS - (System.currentTimeMillis() - offer.createdAtClientMs));
        long secs = remaining / 1000L;
        String expires = "Expires in " + (secs / 60) + ":" + String.format("%02d", secs % 60);
        g.drawString(font, expires, x + (W - font.width(expires)) / 2, y + 18, 0xFF555555, false);

        int colW = (W - 18) / 2;
        int leftX  = x + 6;
        int rightX = x + 12 + colW;

        Player player = Minecraft.getInstance().player;
        float[] savedCloth = EpicFightCompat.captureClothState(player);

        renderColumn(g, leftX,  colY, colW, "They give:", offer.senderHats,   leftScroll,  player);
        renderColumn(g, rightX, colY, colW, "You give:",  offer.receiverHats, rightScroll, player);

        EpicFightCompat.restoreClothState(player, savedCloth);

        if (offer.receiverHats.isEmpty()) {
            String note = "Gift — they ask nothing in return";
            g.drawString(font, note, x + (W - font.width(note)) / 2, y + H - 36, 0xFF227722, false);
        } else if (offer.senderHats.isEmpty()) {
            String note = "They want a gift from you";
            g.drawString(font, note, x + (W - font.width(note)) / 2, y + H - 36, 0xFF884400, false);
        }

        super.render(g, mouseX, mouseY, partialTick);
    }

    private void renderColumn(GuiGraphics g, int cx, int cy, int cw, String header,
                              List<HatPart> hats, int scroll, Player player)
    {
        g.drawString(font, header, cx, cy, 0xFF222222, false);
        int rowsY = cy + 11;
        int rowsH = colH - 11;

        g.fill(cx - 1, rowsY - 1, cx + cw + 1, rowsY + rowsH + 1, 0xFF555555);
        g.fill(cx,     rowsY,     cx + cw,     rowsY + rowsH,     0xFF2B2B2B);

        if (hats.isEmpty()) {
            String s = "(nothing)";
            g.drawString(font, s, cx + (cw - font.width(s)) / 2, rowsY + rowsH / 2 - 4, 0xFF777777, false);
            return;
        }

        if (player == null) return;

        int cellsPerRow = Math.max(1, (cw - 6) / CELL_W);
        int gap = (cw - 6 - cellsPerRow * CELL_W) / Math.max(1, cellsPerRow + 1);

        g.enableScissor(cx + 1, rowsY + 1, cx + cw - 1, rowsY + rowsH - 1);

        for (int i = 0; i < hats.size(); i++) {
            int r = i / cellsPerRow;
            int c = i % cellsPerRow;
            int hx = cx + 3 + gap + c * (CELL_W + gap);
            int hy = rowsY + 3 + r * ROW_H - scroll;

            if (hy + ROW_H <= rowsY || hy >= rowsY + rowsH) continue;

            renderHatCell(g, hats.get(i), hx, hy, player);
        }

        g.disableScissor();

        int totalRows = (int) Math.ceil(hats.size() / (double) cellsPerRow);
        int contentH  = totalRows * ROW_H + 6;
        int maxScroll = Math.max(0, contentH - rowsH);
        if (maxScroll > 0) {
            int sbX = cx + cw - 4;
            g.fill(sbX, rowsY, sbX + 3, rowsY + rowsH, 0xFF1A1A1A);
            int barH = Math.max(10, rowsH * rowsH / contentH);
            int barY = rowsY + (rowsH - barH) * scroll / maxScroll;
            g.fill(sbX, barY, sbX + 3, barY + barH, 0xFF888888);
        }
    }

    private void renderHatCell(GuiGraphics g, HatPart hat, int cx, int cy, Player player)
    {
        g.fill(cx, cy, cx + CELL_W, cy + CELL_H, 0xFF404040);
        g.fill(cx, cy, cx + CELL_W, cy + 1, 0xFF555555);
        g.fill(cx, cy, cx + 1, cy + CELL_H, 0xFF555555);
        g.fill(cx, cy + CELL_H - 1, cx + CELL_W, cy + CELL_H, 0xFF2A2A2A);
        g.fill(cx + CELL_W - 1, cy, cx + CELL_W, cy + CELL_H, 0xFF2A2A2A);

        int centerX = cx + CELL_W / 2;
        int footY = cy + CELL_H + 42;
        HatPreviewRenderer.render(g, player, hat,
            centerX, footY, 34,
            cx + 1, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1);

        if (hat.count > 1) {
            String tag = "x" + hat.count;
            int tw = font.width(tag);
            g.fill(cx + CELL_W - tw - 4, cy + CELL_H - 11, cx + CELL_W - 1, cy + CELL_H - 1, 0xCC1A4A8A);
            g.drawString(font, tag, cx + CELL_W - tw - 2, cy + CELL_H - 10, 0xFFFFFFFF, false);
        }

        drawScrollingName(g, hat.name, cx, cy + CELL_H + 1, CELL_W, 0xFFDDDDDD);
    }

    private void drawScrollingName(GuiGraphics g, String s, int xPos, int yPos, int availW, int color)
    {
        float sc = 0.7f;
        int textW = (int)(font.width(s) * sc);
        int xOff;
        if (textW <= availW) {
            xOff = (availW - textW) / 2;
        } else {
            float overflow = textW - availW;
            float scrollMs = overflow * 50f;
            float pauseMs  = 1000f;
            long period = Math.max(100L, (long)(2 * (scrollMs + pauseMs)));
            long t = System.currentTimeMillis() % period;
            float p;
            if (t < pauseMs) p = 0f;
            else if (t < pauseMs + scrollMs) p = (t - pauseMs) / scrollMs;
            else if (t < 2 * pauseMs + scrollMs) p = 1f;
            else p = 1f - (t - 2 * pauseMs - scrollMs) / scrollMs;
            xOff = -(int)(overflow * p);
        }

        g.enableScissor(xPos, yPos - 1, xPos + availW, yPos + (int)(font.lineHeight * sc) + 1);
        g.pose().pushPose();
        g.pose().translate(xPos + xOff, yPos, 0);
        g.pose().scale(sc, sc, 1f);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
        g.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        int colW = (W - 18) / 2;
        int leftX  = x + 6;
        int rightX = x + 12 + colW;
        int rowsY  = colY + 11;
        int rowsH  = colH - 11;

        if (mouseY >= rowsY && mouseY < rowsY + rowsH) {
            if (mouseX >= leftX && mouseX < leftX + colW) {
                leftScroll = clampScroll(leftScroll - (int)(delta * 12), offer.senderHats.size(), colW, rowsH);
                return true;
            }
            if (mouseX >= rightX && mouseX < rightX + colW) {
                rightScroll = clampScroll(rightScroll - (int)(delta * 12), offer.receiverHats.size(), colW, rowsH);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private int clampScroll(int proposed, int hatCount, int cw, int rowsH)
    {
        int cellsPerRow = Math.max(1, (cw - 6) / CELL_W);
        int totalRows = (int) Math.ceil(hatCount / (double) cellsPerRow);
        int contentH  = totalRows * ROW_H + 6;
        int max = Math.max(0, contentH - rowsH);
        return Math.max(0, Math.min(max, proposed));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose()
    {
        Minecraft.getInstance().setScreen(null);
    }
}
