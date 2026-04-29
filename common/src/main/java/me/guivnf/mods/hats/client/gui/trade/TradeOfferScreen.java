package me.guivnf.mods.hats.client.gui.trade;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import me.guivnf.mods.hats.client.gui.HatPreviewRenderer;
import me.guivnf.mods.hats.client.trade.ClientTradeFlow;
import me.guivnf.mods.hats.client.trade.ClientTradeState;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.packet.PacketSendTradeOffer;
import me.guivnf.mods.hats.common.trade.TradeErrorType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class TradeOfferScreen
        extends Screen
{
    private static final int W = 420;
    private static final int H = 220;

    private static final int CELL_W   = 54;
    private static final int CELL_H   = 66;
    private static final int SCROLL_W = 7;

    private static final int GRID_COLS = 2;
    private static final int GRID_W    = CELL_W * GRID_COLS + SCROLL_W;
    private static final int GRID_H    = 128;

    private static final int SLOT_SIZE = 60;

    private static final int BURGER_SZ = 11;

    private final UUID targetUuid;
    private final String targetName;

    private int x, y;
    private int leftGridX, rightGridX, gridY;
    private int leftSlotX, rightSlotX, slotY;

    private int leftScroll  = 0;
    private int rightScroll = 0;

    private int leftHovered  = -1;
    private int rightHovered = -1;

    private final List<HatPart> leftSelected  = new ArrayList<>();
    private final List<HatPart> rightSelected = new ArrayList<>();

    private final Map<String, HatPart> leftCustom  = new HashMap<>();
    private final Map<String, HatPart> rightCustom = new HashMap<>();

    private String leftFilter  = "";
    private String rightFilter = "";

    @Nullable private EditBox leftSearch;
    @Nullable private EditBox rightSearch;

    private boolean leftScrollDragging = false;
    private boolean rightScrollDragging = false;
    private int dragStartY;
    private int dragStartScroll;

    @Nullable private HatPart configuringHat;
    private boolean configuringLeft;
    private final java.util.Set<String> configOwnedAccessories = new java.util.HashSet<>();

    @Nullable private Button tradeButton;
    @Nullable private Button cancelButton;

    public TradeOfferScreen(UUID targetUuid, String targetName)
    {
        super(Component.literal("Trade with " + targetName));
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    @Override
    protected void init()
    {
        x = (width - W) / 2;
        y = (height - H) / 2;

        leftGridX  = x + 8;
        rightGridX = x + W - 8 - GRID_W;
        gridY      = y + 32;

        int centerMidX = x + W / 2;
        leftSlotX  = centerMidX - SLOT_SIZE - 18;
        rightSlotX = centerMidX + 18;
        slotY      = gridY + (GRID_H - SLOT_SIZE) / 2;

        int searchW = GRID_W - SCROLL_W - 2;
        int searchY = gridY + GRID_H + 2;

        leftSearch = new EditBox(font, leftGridX + 1, searchY, searchW, 14,
            Component.literal("Search"));
        leftSearch.setBordered(true);
        leftSearch.setMaxLength(40);
        leftSearch.setHint(Component.literal("Search..."));
        leftSearch.setResponder(s -> { leftFilter = s.toLowerCase(Locale.ROOT); leftScroll = 0; });
        addRenderableWidget(leftSearch);

        rightSearch = new EditBox(font, rightGridX + 1, searchY, searchW, 14,
            Component.literal("Search"));
        rightSearch.setBordered(true);
        rightSearch.setMaxLength(40);
        rightSearch.setHint(Component.literal("Search..."));
        rightSearch.setResponder(s -> { rightFilter = s.toLowerCase(Locale.ROOT); rightScroll = 0; });
        addRenderableWidget(rightSearch);

        int btnY = y + H - 22;
        cancelButton = addRenderableWidget(Button.builder(
            Component.literal("Cancel"), b -> back())
            .bounds(centerMidX - 80, btnY, 70, 18).build());

        tradeButton = addRenderableWidget(Button.builder(
            Component.literal("Trade"), b -> sendOffer())
            .bounds(centerMidX + 10, btnY, 70, 18).build());

        updateButtons();
    }

    private void back()
    {
        ClientTradeFlow.closeBuilder();
        Minecraft.getInstance().setScreen(new TradePartnerSelectScreen());
    }

    private void updateButtons()
    {
        if (tradeButton != null) {
            tradeButton.active = !leftSelected.isEmpty() || !rightSelected.isEmpty();
        }
    }

    private void sendOffer()
    {
        NetworkManager.sendToServer(HatsNetwork.SEND_TRADE_OFFER,
            PacketSendTradeOffer.encode(targetUuid, leftSelected, rightSelected));
        ClientTradeFlow.closeBuilder();
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(g, mouseX, mouseY, partialTick);

        TradeErrorType err = ClientTradeState.consumeError();
        if (err != null) handleError(err);

        g.fill(x - 1, y - 1, x + W + 1, y + H + 1, 0xFF555555);
        g.fill(x,     y,     x + W,     y + H,     0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + W - 1, y + 2,     0xFFFFFFFF);
        g.fill(x + 1, y + H - 2, x + W - 1, y + H - 1, 0xFF555555);

        Player player = Minecraft.getInstance().player;

        if (configuringHat != null) {
            setSearchVisible(false);
            renderConfigView(g, mouseX, mouseY, player);
            super.render(g, mouseX, mouseY, partialTick);
            return;
        }
        setSearchVisible(true);

        g.drawString(font, title, x + (W - font.width(title)) / 2, y + 8, 0xFF222222, false);

        String localName = player != null ? player.getGameProfile().getName() : "You";
        g.drawString(font, "You — " + localName, leftGridX, gridY - 12, 0xFF222222, false);
        String themLabel = "Them — " + targetName;
        g.drawString(font, themLabel, rightGridX + GRID_W - font.width(themLabel), gridY - 12, 0xFF222222, false);

        float[] savedCloth = EpicFightCompat.captureClothState(player);

        renderGrid(g, mouseX, mouseY, true,  player, getSourceList(true),  leftScroll);
        renderGrid(g, mouseX, mouseY, false, player, getSourceList(false), rightScroll);

        renderCenter(g, mouseX, mouseY, player);

        EpicFightCompat.restoreClothState(player, savedCloth);

        super.render(g, mouseX, mouseY, partialTick);

        renderTooltips(g, mouseX, mouseY);
    }

    private void setSearchVisible(boolean v)
    {
        if (leftSearch != null)  leftSearch.visible = v;
        if (rightSearch != null) rightSearch.visible = v;
        if (cancelButton != null) cancelButton.visible = v;
        if (tradeButton != null)  tradeButton.visible = v;
    }

    private void handleError(TradeErrorType err)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                Component.translatable(err.messageKey()).withStyle(ChatFormatting.RED), true);
        }
        ClientTradeFlow.closeBuilder();
        mc.setScreen(null);
    }

    private List<HatPart> getSourceList(boolean left)
    {
        List<HatPart> result = new ArrayList<>();
        String filter = left ? leftFilter : rightFilter;
        if (left) {
            HatPart equipped = ClientHatCache.getLocalEquipped();
            for (HatPart h : ClientHatCache.getLocalInventory()) {
                if (h.count <= 0) continue;
                if (equipped != null && equipped.name.equals(h.name)) continue;
                if (!filter.isEmpty() && !h.name.toLowerCase(Locale.ROOT).contains(filter)) continue;
                result.add(h);
            }
        } else {
            List<HatPart> remote = ClientTradeState.getPeerInventory(targetUuid);
            if (remote == null) return result;
            HatPart peerEquipped = ClientTradeState.getPeerEquipped(targetUuid);
            for (HatPart h : remote) {
                if (h.count <= 0) continue;
                if (peerEquipped != null && peerEquipped.name.equals(h.name)) continue;
                if (!filter.isEmpty() && !h.name.toLowerCase(Locale.ROOT).contains(filter)) continue;
                result.add(h);
            }
        }
        return result;
    }

    private int countSelected(List<HatPart> sel, String name)
    {
        int c = 0;
        for (HatPart h : sel) if (h.name.equals(name)) c++;
        return c;
    }

    private HatPart effectiveCopy(HatPart base, boolean left)
    {
        Map<String, HatPart> custom = left ? leftCustom : rightCustom;
        HatPart override = custom.get(base.name);
        if (override != null) {
            HatPart copy = base.copy();
            copy.accessories.clear();
            for (HatPart a : override.accessories) copy.accessories.add(a.copy());
            return copy;
        }
        return base.copy();
    }

    private void renderGrid(GuiGraphics g, int mouseX, int mouseY, boolean left,
                            @Nullable Player player, List<HatPart> src, int scroll)
    {
        int gx = left ? leftGridX : rightGridX;
        int gy = gridY;

        g.fill(gx,             gy,             gx + GRID_W - SCROLL_W, gy + GRID_H,         0xFF3B3B3B);
        g.fill(gx,             gy,             gx + GRID_W - SCROLL_W, gy + 1,              0xFF222222);
        g.fill(gx,             gy,             gx + 1,                 gy + GRID_H,         0xFF222222);
        g.fill(gx,             gy + GRID_H - 1, gx + GRID_W - SCROLL_W, gy + GRID_H,        0xFF666666);
        g.fill(gx + GRID_W - SCROLL_W - 1, gy, gx + GRID_W - SCROLL_W, gy + GRID_H,         0xFF666666);

        if (src.isEmpty()) {
            String s = left ? "No hats" : "Loading...";
            g.drawString(font, s, gx + 4, gy + 4, 0xFFAAAAAA, false);
        } else if (player != null) {
            int hovered = -1;
            int gridContentW = GRID_W - SCROLL_W;
            if (mouseX >= gx && mouseX < gx + gridContentW
                    && mouseY >= gy && mouseY < gy + GRID_H) {
                int relX = (int)(mouseX - gx);
                int relY = (int)(mouseY - gy) + scroll;
                int hc = relX / CELL_W;
                int hr = relY / CELL_H;
                int idx = hr * GRID_COLS + hc;
                if (hc < GRID_COLS && idx >= 0 && idx < src.size()) hovered = idx;
            }
            if (left) leftHovered = hovered; else rightHovered = hovered;

            g.enableScissor(gx + 1, gy + 1, gx + gridContentW - 1, gy + GRID_H - 1);

            int row = 0, col = 0;
            for (int i = 0; i < src.size(); i++) {
                HatPart hat = src.get(i);
                int cx = gx + col * CELL_W;
                int cy = gy + row * CELL_H - scroll;

                if (cy + CELL_H > gy && cy < gy + GRID_H) {
                    renderCell(g, hat, cx, cy, hovered == i, mouseX, mouseY, player, left);
                }

                if (++col >= GRID_COLS) { col = 0; row++; }
            }

            g.disableScissor();
        }

        renderScrollbar(g, gx + GRID_W - SCROLL_W, gy, src.size(), scroll);
    }

    private void renderCell(GuiGraphics g, HatPart hat, int cx, int cy, boolean hovered,
                            int mouseX, int mouseY, Player player, boolean left)
    {
        int bg = hovered ? 0xFF505060 : 0xFF404040;
        g.fill(cx, cy, cx + CELL_W, cy + CELL_H, bg);
        g.fill(cx, cy, cx + CELL_W, cy + 1, 0xFF555555);
        g.fill(cx, cy, cx + 1, cy + CELL_H, 0xFF555555);
        g.fill(cx, cy + CELL_H - 1, cx + CELL_W, cy + CELL_H, 0xFF2A2A2A);
        g.fill(cx + CELL_W - 1, cy, cx + CELL_W, cy + CELL_H, 0xFF2A2A2A);

        HatPart displayHat = hat;
        Map<String, HatPart> custom = left ? leftCustom : rightCustom;
        HatPart override = custom.get(hat.name);
        if (override != null) {
            displayHat = hat.copy();
            displayHat.accessories.clear();
            for (HatPart a : override.accessories) displayHat.accessories.add(a.copy());
        }

        int centerX = cx + CELL_W / 2;
        int footY = cy + CELL_H + 54;
        HatPreviewRenderer.render(g, player, displayHat,
            centerX, footY, 42,
            cx + 1, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1);

        int selectedCount = countSelected(left ? leftSelected : rightSelected, hat.name);
        int available = hat.count - selectedCount;
        if (selectedCount > 0) {
            String tag = "x" + selectedCount;
            int tw = font.width(tag);
            g.fill(cx + CELL_W - tw - 4, cy + CELL_H - 11, cx + CELL_W - 1, cy + CELL_H - 1, 0xCC1A4A8A);
            g.drawString(font, tag, cx + CELL_W - tw - 2, cy + CELL_H - 10, 0xFFFFFFFF, false);
        }
        if (available <= 0) {
            g.fill(cx + 1, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1, 0x88000000);
        }

        HatDefinition def = HatRegistry.get(hat.getRegistryKey());
        boolean hasAcc = def != null && !def.getAccessories().isEmpty();
        if (hovered && hasAcc) {
            renderBurger(g, cx, cy, mouseX, mouseY);
        }
    }

    private void renderBurger(GuiGraphics g, int cx, int cy, int mouseX, int mouseY)
    {
        int bx = cx + 1;
        int by = cy + 1;
        boolean hov = mouseX >= bx && mouseX < bx + BURGER_SZ
                   && mouseY >= by && mouseY < by + BURGER_SZ;
        int bg = hov ? 0xCCAAAACC : 0xCC333355;
        g.fill(bx, by, bx + BURGER_SZ, by + BURGER_SZ, bg);
        for (int line = 0; line < 3; line++) {
            g.fill(bx + 2, by + 3 + line * 3, bx + BURGER_SZ - 2, by + 4 + line * 3, 0xFFDDDDDD);
        }
    }

    private void renderScrollbar(GuiGraphics g, int sbX, int sbY, int total, int scroll)
    {
        g.fill(sbX, sbY, sbX + SCROLL_W, sbY + GRID_H, 0xFF2A2A2A);
        g.fill(sbX + 1, sbY + 1, sbX + SCROLL_W - 1, sbY + GRID_H - 1, 0xFF333333);

        int rows = (int) Math.ceil(total / (double) GRID_COLS);
        int max = Math.max(0, rows * CELL_H - GRID_H);
        if (max <= 0) return;

        int sbInner = GRID_H - 2;
        int thumbH = Math.max(14, sbInner * sbInner / (sbInner + max));
        int thumbY = sbY + 1 + (int) ((sbInner - thumbH) * (float) scroll / max);

        g.fill(sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + thumbH, 0xFF777777);
        g.fill(sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + 1, 0xFFAAAAAA);
    }

    private void renderCenter(GuiGraphics g, int mouseX, int mouseY, @Nullable Player player)
    {
        renderSlot(g, leftSlotX, slotY, leftSelected, player);
        renderSlot(g, rightSlotX, slotY, rightSelected, player);

        int aMidY = slotY + SLOT_SIZE / 2 - 1;
        int aXStart = leftSlotX + SLOT_SIZE + 1;
        int aXEnd   = rightSlotX - 2;
        boolean leftHas  = !leftSelected.isEmpty();
        boolean rightHas = !rightSelected.isEmpty();
        renderArrow(g, aXStart, aXEnd, aMidY, leftHas, rightHas);
    }

    private void renderSlot(GuiGraphics g, int sx, int sy, List<HatPart> selected, @Nullable Player player)
    {
        g.fill(sx - 1, sy - 1, sx + SLOT_SIZE + 1, sy + SLOT_SIZE + 1, 0xFF222222);
        g.fill(sx,     sy,     sx + SLOT_SIZE,     sy + SLOT_SIZE,     0xFF555555);
        g.fill(sx,     sy,     sx + SLOT_SIZE,     sy + 1,             0xFF777777);
        g.fill(sx,     sy,     sx + 1,             sy + SLOT_SIZE,     0xFF777777);
        g.fill(sx,     sy + SLOT_SIZE - 1, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF333333);
        g.fill(sx + SLOT_SIZE - 1, sy, sx + SLOT_SIZE, sy + SLOT_SIZE, 0xFF333333);

        if (selected.isEmpty() || player == null) return;

        HatPart top = selected.get(selected.size() - 1);
        int centerX = sx + SLOT_SIZE / 2;
        int footY = sy + SLOT_SIZE + 48;
        HatPreviewRenderer.render(g, player, top,
            centerX, footY, 44,
            sx + 1, sy + 1, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1);

        if (selected.size() > 1) {
            String tag = "x" + selected.size();
            int tw = font.width(tag);
            g.fill(sx + SLOT_SIZE - tw - 4, sy + SLOT_SIZE - 11, sx + SLOT_SIZE - 1, sy + SLOT_SIZE - 1, 0xCC1A4A8A);
            g.drawString(font, tag, sx + SLOT_SIZE - tw - 2, sy + SLOT_SIZE - 10, 0xFFFFFFFF, false);
        }

        drawScrollingName(g, top.name, sx, sy + SLOT_SIZE + 4, SLOT_SIZE, 0xFF222222);
    }

    private void renderArrow(GuiGraphics g, int x1, int x2, int yMid, boolean leftHas, boolean rightHas)
    {
        if (!leftHas && !rightHas) {
            g.fill(x1, yMid + 1, x2, yMid + 3, 0xFF777777);
            return;
        }

        int color = 0xFF333333;
        int arrowLen = 16;
        int headW = 5;

        if (leftHas) {
            int tipX = x2;
            int headBaseX = tipX - headW + 1;
            int shaftL = tipX - arrowLen + 1;
            g.fill(shaftL, yMid + 1, headBaseX, yMid + 3, color);
            for (int i = 0; i < headW; i++) {
                int col = headBaseX + i;
                int half = headW - 1 - i;
                g.fill(col, yMid + 2 - half, col + 1, yMid + 3 + half, color);
            }
        }
        if (rightHas) {
            int tipX = x1;
            int headBaseX = tipX + headW - 1;
            int shaftR = tipX + arrowLen - 1;
            g.fill(headBaseX + 1, yMid + 1, shaftR + 1, yMid + 3, color);
            for (int i = 0; i < headW; i++) {
                int col = headBaseX - i;
                int half = headW - 1 - i;
                g.fill(col, yMid + 2 - half, col + 1, yMid + 3 + half, color);
            }
        }
    }

    private void renderTooltips(GuiGraphics g, int mouseX, int mouseY)
    {
        int hov = leftHovered;
        List<HatPart> src = getSourceList(true);
        if (hov < 0 || hov >= src.size()) {
            hov = rightHovered;
            src = getSourceList(false);
        }
        if (hov < 0 || hov >= src.size()) return;

        HatPart h = src.get(hov);
        HatDefinition def = HatRegistry.get(h.getRegistryKey());
        List<Component> lines = new ArrayList<>();
        if (def != null) {
            int nameCol = nameColorFor(def);
            lines.add(Component.literal(def.name).withStyle(s -> s.withColor(nameCol)));
            String rarityLabel = def.getRarity().name().charAt(0)
                + def.getRarity().name().substring(1).toLowerCase();
            lines.add(Component.literal(rarityLabel)
                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        } else {
            lines.add(Component.literal(h.name));
        }
        g.renderTooltip(font,
            lines.stream().map(Component::getVisualOrderText).toList(),
            mouseX, mouseY);
    }

    private int nameColorFor(@Nullable HatDefinition def)
    {
        if (def == null) return 0xFFFFFF;
        if (def.meta.contributorUuid != null) return 0x55FFFF;
        Integer c = def.getRarity().colour.getColor();
        return c != null ? c : 0xFFFFFF;
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

    private void renderConfigView(GuiGraphics g, int mouseX, int mouseY, @Nullable Player player)
    {
        if (configuringHat == null) return;

        g.drawString(font, "Configure accessories — " + configuringHat.name,
            x + 8, y + 8, 0xFF222222, false);

        int previewSize = 80;
        int previewX = x + 16;
        int previewY = y + 30;
        g.fill(previewX - 1, previewY - 1,
               previewX + previewSize + 1, previewY + previewSize + 1, 0xFF222222);
        g.fill(previewX, previewY,
               previewX + previewSize, previewY + previewSize, 0xFF555555);

        if (player != null) {
            float[] savedCloth = EpicFightCompat.captureClothState(player);
            HatPreviewRenderer.render(g, player, configuringHat,
                previewX + previewSize / 2, previewY + previewSize + 64, 60,
                previewX + 1, previewY + 1,
                previewX + previewSize - 1, previewY + previewSize - 1);
            EpicFightCompat.restoreClothState(player, savedCloth);
        }

        int listX = previewX + previewSize + 16;
        int listY = y + 30;
        int listW = x + W - listX - 16;
        int listH = H - 30 - 32;

        g.fill(listX - 1, listY - 1, listX + listW + 1, listY + listH + 1, 0xFF555555);
        g.fill(listX,     listY,     listX + listW,     listY + listH,     0xFF2B2B2B);
        g.drawString(font, "Accessories:", listX + 4, listY + 4, 0xFFDDDDDD, false);

        HatDefinition def = HatRegistry.get(configuringHat.getRegistryKey());
        if (def == null || configOwnedAccessories.isEmpty()) {
            g.drawString(font, "(none)", listX + 4, listY + 18, 0xFFAAAAAA, false);
        } else {
            int rowY = listY + 16;
            int rowH = 14;
            for (HatDefinition accDef : def.getAccessories()) {
                if (!configOwnedAccessories.contains(accDef.name)) continue;
                boolean enabled = isAccessoryEnabled(accDef.name);
                boolean rowHov = mouseX >= listX + 2 && mouseX < listX + listW - 2
                              && mouseY >= rowY && mouseY < rowY + rowH;
                int rowBg = rowHov ? 0xFF3D5A80 : 0xFF333333;
                g.fill(listX + 2, rowY, listX + listW - 2, rowY + rowH - 1, rowBg);

                int boxX = listX + 4;
                int boxY = rowY + 2;
                g.fill(boxX, boxY, boxX + 9, boxY + 9, 0xFF000000);
                g.fill(boxX + 1, boxY + 1, boxX + 8, boxY + 8, enabled ? 0xFF44AA44 : 0xFF222222);
                if (enabled) {
                    g.fill(boxX + 3, boxY + 4, boxX + 4, boxY + 7, 0xFFFFFFFF);
                    g.fill(boxX + 4, boxY + 5, boxX + 7, boxY + 6, 0xFFFFFFFF);
                }
                g.drawString(font, accDef.name, boxX + 14, rowY + 3, 0xFFFFFFFF, false);
                rowY += rowH;
                if (rowY + rowH > listY + listH) break;
            }
        }

        int doneY = y + H - 24;
        boolean doneHov = mouseX >= listX && mouseX < listX + 70
                       && mouseY >= doneY && mouseY < doneY + 18;
        g.fill(listX, doneY, listX + 70, doneY + 18, doneHov ? 0xFFAAAAAA : 0xFF777777);
        g.drawString(font, "Done", listX + 28, doneY + 5, 0xFFFFFFFF, false);
    }

    private boolean isAccessoryEnabled(String accName)
    {
        if (configuringHat == null) return false;
        for (HatPart a : configuringHat.accessories) {
            if (a.name.equals(accName)) return a.isShowing;
        }
        return false;
    }

    private void toggleAccessory(String accName)
    {
        if (configuringHat == null) return;
        HatDefinition def = HatRegistry.get(configuringHat.getRegistryKey());
        HatDefinition accDef = null;
        if (def != null) {
            for (HatDefinition d : def.getAccessories()) if (d.name.equals(accName)) { accDef = d; break; }
        }
        for (HatPart a : configuringHat.accessories) {
            if (a.name.equals(accName)) {
                a.isShowing = !a.isShowing;
                return;
            }
        }
        HatPart np = new HatPart(accName);
        if (accDef != null) np.registryKey = accDef.getFullName();
        np.isShowing = true;
        configuringHat.accessories.add(np);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (configuringHat != null) {
            return handleConfigClick(mouseX, mouseY, button);
        }

        if (button == 0) {
            if (handleScrollbarClick(mouseX, mouseY, true))  return true;
            if (handleScrollbarClick(mouseX, mouseY, false)) return true;
            if (handleGridClick(mouseX, mouseY, true))  return true;
            if (handleGridClick(mouseX, mouseY, false)) return true;
            if (handleSlotClick(mouseX, mouseY)) return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleConfigClick(double mouseX, double mouseY, int button)
    {
        if (configuringHat == null) return false;

        int previewSize = 80;
        int previewX = x + 16;
        int listX = previewX + previewSize + 16;
        int listY = y + 30;
        int listW = x + W - listX - 16;
        int listH = H - 30 - 32;
        int doneY = y + H - 24;

        if (button == 0 && mouseX >= listX && mouseX < listX + 70
                && mouseY >= doneY && mouseY < doneY + 18) {
            commitConfig();
            return true;
        }

        if (button == 0) {
            HatDefinition def = HatRegistry.get(configuringHat.getRegistryKey());
            if (def != null) {
                int rowY = listY + 16;
                int rowH = 14;
                for (HatDefinition accDef : def.getAccessories()) {
                    if (!configOwnedAccessories.contains(accDef.name)) continue;
                    if (mouseX >= listX + 2 && mouseX < listX + listW - 2
                            && mouseY >= rowY && mouseY < rowY + rowH) {
                        toggleAccessory(accDef.name);
                        return true;
                    }
                    rowY += rowH;
                    if (rowY + rowH > listY + listH) break;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void commitConfig()
    {
        if (configuringHat == null) return;
        Map<String, HatPart> custom = configuringLeft ? leftCustom : rightCustom;
        custom.put(configuringHat.name, configuringHat);
        configuringHat = null;
    }

    private boolean handleScrollbarClick(double mouseX, double mouseY, boolean left)
    {
        int sbX = (left ? leftGridX : rightGridX) + GRID_W - SCROLL_W;
        if (mouseX < sbX || mouseX >= sbX + SCROLL_W
                || mouseY < gridY || mouseY >= gridY + GRID_H) return false;

        int total = getSourceList(left).size();
        int rows = (int) Math.ceil(total / (double) GRID_COLS);
        int max = Math.max(0, rows * CELL_H - GRID_H);
        if (max <= 0) return true;

        int sbInner = GRID_H - 2;
        int thumbH = Math.max(14, sbInner * sbInner / (sbInner + max));
        int currentScroll = left ? leftScroll : rightScroll;
        int thumbY = gridY + 1 + (int) ((sbInner - thumbH) * (float) currentScroll / max);

        if (mouseY >= thumbY && mouseY < thumbY + thumbH) {
            if (left) {
                leftScrollDragging = true;
            } else {
                rightScrollDragging = true;
            }
            dragStartY = (int) mouseY;
            dragStartScroll = currentScroll;
        } else {
            int travel = sbInner - thumbH;
            int newScroll = travel <= 0 ? 0
                : (int) (((mouseY - thumbH / 2.0 - gridY - 1) * max) / travel);
            newScroll = Math.max(0, Math.min(max, newScroll));
            if (left) leftScroll = newScroll; else rightScroll = newScroll;
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0) {
            leftScrollDragging = false;
            rightScrollDragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (button == 0 && (leftScrollDragging || rightScrollDragging)) {
            boolean left = leftScrollDragging;
            int total = getSourceList(left).size();
            int rows = (int) Math.ceil(total / (double) GRID_COLS);
            int max = Math.max(0, rows * CELL_H - GRID_H);
            if (max <= 0) return true;
            int sbInner = GRID_H - 2;
            int thumbH = Math.max(14, sbInner * sbInner / (sbInner + max));
            int travel = sbInner - thumbH;
            if (travel <= 0) return true;
            int delta = (int) mouseY - dragStartY;
            int newScroll = (int) Math.max(0, Math.min(max,
                dragStartScroll + (long) delta * max / travel));
            if (left) leftScroll = newScroll; else rightScroll = newScroll;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean handleGridClick(double mouseX, double mouseY, boolean left)
    {
        int gx = left ? leftGridX : rightGridX;
        int gridContentW = GRID_W - SCROLL_W;
        if (mouseX < gx || mouseX >= gx + gridContentW
                || mouseY < gridY || mouseY >= gridY + GRID_H) return false;

        List<HatPart> src = getSourceList(left);
        int scroll = left ? leftScroll : rightScroll;
        int relX = (int)(mouseX - gx);
        int relY = (int)(mouseY - gridY) + scroll;
        int hc = relX / CELL_W;
        int hr = relY / CELL_H;
        int idx = hr * GRID_COLS + hc;
        if (hc >= GRID_COLS || idx < 0 || idx >= src.size()) return true;

        int cx = gx + hc * CELL_W;
        int cy = gridY + hr * CELL_H - scroll;

        HatPart picked = src.get(idx);
        HatDefinition def = HatRegistry.get(picked.getRegistryKey());
        boolean hasAcc = def != null && !def.getAccessories().isEmpty();

        if (hasAcc && mouseX >= cx + 1 && mouseX < cx + 1 + BURGER_SZ
                && mouseY >= cy + 1 && mouseY < cy + 1 + BURGER_SZ) {
            Map<String, HatPart> custom = left ? leftCustom : rightCustom;
            HatPart starting = custom.get(picked.name);
            if (starting != null) {
                configuringHat = starting.copy();
            } else {
                configuringHat = picked.copy();
            }
            configuringLeft = left;
            configOwnedAccessories.clear();
            for (HatPart a : picked.accessories) configOwnedAccessories.add(a.name);
            return true;
        }

        List<HatPart> sel = left ? leftSelected : rightSelected;
        int already = countSelected(sel, picked.name);

        if (already >= picked.count) {
            sel.removeIf(h -> h.name.equals(picked.name));
        } else {
            HatPart copy = effectiveCopy(picked, left);
            copy.count = 1;
            sel.add(copy);
        }
        updateButtons();
        return true;
    }

    private boolean handleSlotClick(double mouseX, double mouseY)
    {
        if (mouseX >= leftSlotX && mouseX < leftSlotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
            if (!leftSelected.isEmpty()) leftSelected.remove(leftSelected.size() - 1);
            updateButtons();
            return true;
        }
        if (mouseX >= rightSlotX && mouseX < rightSlotX + SLOT_SIZE
                && mouseY >= slotY && mouseY < slotY + SLOT_SIZE) {
            if (!rightSelected.isEmpty()) rightSelected.remove(rightSelected.size() - 1);
            updateButtons();
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        if (configuringHat != null) return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
        int gridContentW = GRID_W - SCROLL_W;
        if (mouseX >= leftGridX && mouseX < leftGridX + gridContentW
                && mouseY >= gridY && mouseY < gridY + GRID_H) {
            int total = getSourceList(true).size();
            int rows = (int) Math.ceil(total / (double) GRID_COLS);
            int max = Math.max(0, rows * CELL_H - GRID_H);
            leftScroll = (int) Math.max(0, Math.min(max, leftScroll - delta * 12));
            return true;
        }
        if (mouseX >= rightGridX && mouseX < rightGridX + gridContentW
                && mouseY >= gridY && mouseY < gridY + GRID_H) {
            int total = getSourceList(false).size();
            int rows = (int) Math.ceil(total / (double) GRID_COLS);
            int max = Math.max(0, rows * CELL_H - GRID_H);
            rightScroll = (int) Math.max(0, Math.min(max, rightScroll - delta * 12));
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
        ClientTradeFlow.closeBuilder();
        super.onClose();
    }
}
