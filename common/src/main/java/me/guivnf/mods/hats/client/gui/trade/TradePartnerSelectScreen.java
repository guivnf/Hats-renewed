package me.guivnf.mods.hats.client.gui.trade;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.client.gui.HatsScreen;
import me.guivnf.mods.hats.client.trade.ClientTradeFlow;
import me.guivnf.mods.hats.client.trade.ClientTradeState;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.packet.PacketRequestNearbyPlayers;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class TradePartnerSelectScreen
        extends Screen
{
    private static final int W = 220;
    private static final int H = 180;
    private static final int ROW_H = 16;
    private static final int LIST_VISIBLE_ROWS = 7;

    private static final long AUTO_REFRESH_INTERVAL_MS = 1500L;

    private int x, y;
    private int listX, listY, listW, listH;
    private int scroll = 0;
    private long lastRefreshMs = 0L;

    public TradePartnerSelectScreen()
    {
        super(Component.literal("Trade — choose partner"));
    }

    @Override
    protected void init()
    {
        x = (width - W) / 2;
        y = (height - H) / 2;

        addRenderableWidget(Button.builder(
            Component.literal("Refresh"), b -> requestRefresh())
            .bounds(x + W - 64, y + H - 24, 60, 20).build());

        addRenderableWidget(Button.builder(
            Component.literal("Back"), b -> back())
            .bounds(x + 4, y + H - 24, 60, 20).build());

        listX = x + 8;
        listY = y + 28;
        listW = W - 16;
        listH = LIST_VISIBLE_ROWS * ROW_H;

        if (System.currentTimeMillis() - lastRefreshMs > 250L) requestRefresh();
    }

    private void requestRefresh()
    {
        ClientTradeState.clearNearby();
        NetworkManager.sendToServer(HatsNetwork.REQUEST_NEARBY_PLAYERS, PacketRequestNearbyPlayers.encode());
        lastRefreshMs = System.currentTimeMillis();
    }

    private void silentRefresh()
    {
        NetworkManager.sendToServer(HatsNetwork.REQUEST_NEARBY_PLAYERS, PacketRequestNearbyPlayers.encode());
        lastRefreshMs = System.currentTimeMillis();
    }

    @Override
    public void tick()
    {
        if (System.currentTimeMillis() - lastRefreshMs >= AUTO_REFRESH_INTERVAL_MS) {
            silentRefresh();
        }
    }

    private void back()
    {
        HatsScreen.open();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(g, mouseX, mouseY, partialTick);

        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF555555);
        g.fill(x,     y,     x + W,     y + H,     0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + W - 1, y + 2,     0xFFFFFFFF);
        g.fill(x + 1, y + H - 2, x + W - 1, y + H - 1, 0xFF555555);

        g.drawString(font, title, x + (W - font.width(title)) / 2, y + 8, 0xFF333333, false);

        if (ClientTradeState.hasIncomingOffers()) {
            renderIncomingHeader(g, mouseX, mouseY);
        }

        if (!ClientTradeState.isNearbyReceived()) {
            renderLoading(g);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }

        renderList(g, mouseX, mouseY);

        super.render(g, mouseX, mouseY, partialTick);
    }

    private int incomingHeaderY() { return listY - 14; }

    private void renderIncomingHeader(GuiGraphics g, int mouseX, int mouseY)
    {
        int n = ClientTradeState.getIncomingOffers().size();
        String txt = n + " incoming — click";
        int hy = incomingHeaderY();
        boolean hover = mouseX >= listX && mouseX < listX + listW && mouseY >= hy && mouseY < hy + 12;
        g.fill(listX, hy, listX + listW, hy + 12, hover ? 0xFFFFC04D : 0xFFFFAA00);
        int tw = font.width(txt);
        g.drawString(font, txt, listX + (listW - tw) / 2, hy + 2, 0xFF222222, false);
    }

    private void renderLoading(GuiGraphics g)
    {
        int dots = (int)((System.currentTimeMillis() / 400) % 4);
        StringBuilder s = new StringBuilder("Fetching nearby players");
        for (int i = 0; i < dots; i++) s.append('.');
        int tw = font.width(s.toString());
        g.drawString(font, s.toString(), x + (W - tw) / 2, y + H / 2 - 4, 0xFF333333, false);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY)
    {
        List<me.guivnf.mods.hats.common.network.packet.PacketNearbyPlayersList.Entry> nearby =
            ClientTradeState.getNearby();

        g.fill(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, 0xFF555555);
        g.fill(listX,     listY,     listX + listW,     listY + listH,     0xFF2B2B2B);

        if (nearby.isEmpty()) {
            String s = "No players within range.";
            int tw = font.width(s);
            g.drawString(font, s, x + (W - tw) / 2, listY + listH / 2 - 4, 0xFFAAAAAA, false);
            return;
        }

        int visible = Math.min(LIST_VISIBLE_ROWS, nearby.size() - scroll);
        for (int i = 0; i < visible; i++) {
            int idx = scroll + i;
            int ry = listY + i * ROW_H;
            boolean hover = mouseX >= listX && mouseX < listX + listW
                         && mouseY >= ry   && mouseY < ry + ROW_H;
            if (hover) g.fill(listX, ry, listX + listW, ry + ROW_H, 0xFF3D5A80);
            g.drawString(font, nearby.get(idx).name(), listX + 6, ry + 4, 0xFFFFFFFF, false);
        }

        if (nearby.size() > LIST_VISIBLE_ROWS) {
            int sbX = listX + listW - 4;
            g.fill(sbX, listY, sbX + 3, listY + listH, 0xFF333333);
            int barH = Math.max(8, listH * LIST_VISIBLE_ROWS / nearby.size());
            int maxScroll = nearby.size() - LIST_VISIBLE_ROWS;
            int barY = listY + (maxScroll == 0 ? 0 : (listH - barH) * scroll / maxScroll);
            g.fill(sbX, barY, sbX + 3, barY + barH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && ClientTradeState.hasIncomingOffers()) {
            int hy = incomingHeaderY();
            if (mouseX >= listX && mouseX < listX + listW && mouseY >= hy && mouseY < hy + 12) {
                ClientTradeState.IncomingOffer first = ClientTradeState.getIncomingOffers().iterator().next();
                ClientTradeFlow.openIncoming(first.offerId);
                return true;
            }
        }

        if (button == 0 && ClientTradeState.isNearbyReceived()) {
            List<me.guivnf.mods.hats.common.network.packet.PacketNearbyPlayersList.Entry> nearby =
                ClientTradeState.getNearby();
            int visible = Math.min(LIST_VISIBLE_ROWS, nearby.size() - scroll);
            for (int i = 0; i < visible; i++) {
                int ry = listY + i * ROW_H;
                if (mouseX >= listX && mouseX < listX + listW
                        && mouseY >= ry && mouseY < ry + ROW_H) {
                    var e = nearby.get(scroll + i);
                    ClientTradeFlow.openOfferBuilder(e.uuid(), e.name());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        if (mouseX >= listX && mouseX < listX + listW && mouseY >= listY && mouseY < listY + listH) {
            int total = ClientTradeState.getNearby().size();
            int max = Math.max(0, total - LIST_VISIBLE_ROWS);
            scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(delta)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    @Override
    protected void renderBlurredBackground(float partialTick)
    {
        // disable 1.21+ menu-blur effect over the world
    }

    @Override
    protected void renderMenuBackground(GuiGraphics g)
    {
        // skip the in-world dark dirt-pattern overlay
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void onClose()
    {
        super.onClose();
    }
}
