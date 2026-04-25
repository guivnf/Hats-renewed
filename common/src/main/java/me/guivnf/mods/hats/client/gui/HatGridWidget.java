package me.guivnf.mods.hats.client.gui;

import com.mojang.math.Axis;
import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import me.guivnf.mods.hats.client.render.LayerHat;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class HatGridWidget implements GuiEventListener, NarratableEntry {
    public static final int CELL_W = 54;
    public static final int CELL_H = 66;
    public static final int SCROLL_W = 7;
    private static final int STRIP_W = 10;
    private static final int BTN_SZ = 11;
    private static final int BTN_GAP = 2;

    private final Minecraft mc;
    private final Font font;
    private final Consumer<@Nullable HatPart> onSelect;
    private final Runnable onColorRequested;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private int cols;
    private int scrollOffset = 0;
    private boolean scrollDragging = false;
    private int scrollDragStartY;
    private int scrollDragStartOffset;

    private List<HatPart> allHats = new ArrayList<>();
    private List<HatPart> filtered = new ArrayList<>();
    private String currentFilter = "";

    @Nullable
    private HatPart selected;

    private int hoveredIdx = -1;
    private int menuOpenIdx = -1;

    private boolean inAccessoryView = false;
    @Nullable
    private HatPart accessoryViewHat = null;
    private int accScrollOffset = 0;

    private boolean focused = false;
    private boolean visible = true;

    public HatGridWidget(Minecraft mc, int x, int y, int width, int height,
            Consumer<@Nullable HatPart> onSelect, Runnable onColorRequested) {
        this.mc = mc;
        this.font = mc.font;
        this.onSelect = onSelect;
        this.onColorRequested = onColorRequested;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.cols = Math.max(1, gridW() / CELL_W);
    }

    private int gridW() {
        return width - SCROLL_W - 1;
    }

    private int totalRows() {
        return (int) Math.ceil(filtered.size() / (double) cols);
    }

    private int maxScroll() {
        return Math.max(0, totalRows() * CELL_H - height);
    }

    public void setVisible(boolean v) {
        visible = v;
    }

    public void refreshHats(List<HatPart> hats) {
        allHats = new ArrayList<>(hats);
        applyFilter();
        inAccessoryView = false;
        accessoryViewHat = null;
        menuOpenIdx = -1;
    }

    public void setFilter(String query) {
        currentFilter = query.toLowerCase(Locale.ROOT);
        applyFilter();
        scrollOffset = 0;
    }

    private void applyFilter() {
        filtered.clear();
        for (HatPart h : allHats) {
            if (currentFilter.isEmpty() || h.name.toLowerCase(Locale.ROOT).contains(currentFilter))
                filtered.add(h);
        }
    }

    @Nullable
    public HatPart getSelected() {
        return selected;
    }

    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        if (inAccessoryView) {
            renderAccessoryView(g, mouseX, mouseY);
            return;
        }

        g.fill(x, y, x + gridW(), y + height, 0xFF3B3B3B);
        g.fill(x, y, x + gridW(), y + 1, 0xFF222222);
        g.fill(x, y, x + 1, y + height, 0xFF222222);
        g.fill(x, y + height - 1, x + gridW(), y + height, 0xFF666666);
        g.fill(x + gridW() - 1, y, x + gridW(), y + height, 0xFF666666);

        hoveredIdx = -1;
        if (isInGrid(mouseX, mouseY)) {
            int relX = (int) (mouseX - x);
            int relY = (int) (mouseY - y) + scrollOffset;
            int hc = relX / CELL_W;
            int hr = relY / CELL_H;
            int idx = hr * cols + hc;
            if (hc < cols && idx >= 0 && idx < filtered.size())
                hoveredIdx = idx;
        }

        if (filtered.isEmpty()) {
            g.drawString(font, Component.translatable("hats.gui.no_hats").getString(), x + 4, y + 4, 0x888888);
        } else {
            g.enableScissor(x + 1, y + 1, x + gridW() - 1, y + height - 1);

            Player player = mc.player;
            float[] savedCloth = EpicFightCompat.captureClothState(player);
            int row = 0, col = 0;

            for (int i = 0; i < filtered.size(); i++) {
                HatPart hat = filtered.get(i);
                int cx = x + col * CELL_W;
                int cy = y + row * CELL_H - scrollOffset;

                if (cy + CELL_H > y && cy < y + height) {
                    renderCell(g, hat, i, cx, cy, mouseX, mouseY, player);
                }

                if (++col >= cols) {
                    col = 0;
                    row++;
                }
            }

            EpicFightCompat.restoreClothState(player, savedCloth);
            g.disableScissor();
        }

        renderScrollbar(g, mouseX, mouseY);

        if (hoveredIdx >= 0 && hoveredIdx < filtered.size() && menuOpenIdx == -1) {
            HatPart hat = filtered.get(hoveredIdx);
            HatDefinition def = HatRegistry.get(hat.getRegistryKey());
            List<Component> lines = new ArrayList<>();
            if (def != null) {
                int nameCol = nameColorFor(def);
                lines.add(Component.literal(def.name)
                        .withStyle(s -> s.withColor(nameCol)));
                String rarityLabel = def.getRarity().name().charAt(0)
                        + def.getRarity().name().substring(1).toLowerCase();
                lines.add(Component.literal(rarityLabel)
                        .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            } else {
                lines.add(Component.literal(hat.name));
            }
            g.renderTooltip(font,
                    lines.stream().map(Component::getVisualOrderText).toList(),
                    mouseX, mouseY);
        }
    }

    private void renderCell(GuiGraphics g, HatPart hat, int idx, int cx, int cy,
            int mouseX, int mouseY, @Nullable Player player) {
        boolean sel = selected != null && selected.name.equals(hat.name);
        boolean hovered = !scrollDragging && hoveredIdx == idx;
        boolean menuOpen = menuOpenIdx == idx;
        boolean fav = ClientFavourites.isFavourite(hat.name);

        int bg = sel ? 0xFF1A4A8A : (hovered || menuOpen ? 0xFF505060 : 0xFF404040);
        g.fill(cx, cy, cx + CELL_W, cy + CELL_H, bg);
        g.fill(cx, cy, cx + CELL_W, cy + 1, 0xFF555555);
        g.fill(cx, cy, cx + 1, cy + CELL_H, 0xFF555555);
        g.fill(cx, cy + CELL_H - 1, cx + CELL_W, cy + CELL_H, 0xFF2A2A2A);
        g.fill(cx + CELL_W - 1, cy, cx + CELL_W, cy + CELL_H, 0xFF2A2A2A);

        if (fav) {
            g.fill(cx + CELL_W - STRIP_W - 8, cy + 1, cx + CELL_W - STRIP_W - 1, cy + 8, 0xFFFFDD00);
        }

        if (player != null) {
            renderPlayerInCell(g, hat, cx, cy, player);
        }

        renderVerticalName(g, hat, cx, cy);

        if (hovered || menuOpen) {
            renderHamburger(g, cx, cy, menuOpen, mouseX, mouseY);
        }

        if (menuOpen) {
            renderMenuOptions(g, hat, cx, cy, mouseX, mouseY);
        }
    }

    private void renderHamburger(GuiGraphics g, int cx, int cy, boolean active,
            int mouseX, int mouseY) {
        int bx = cx + 1;
        int by = cy + 1;
        boolean hov = mouseX >= bx && mouseX < bx + BTN_SZ && mouseY >= by && mouseY < by + BTN_SZ;
        int bg = active ? 0xCC1A4A8A : (hov ? 0xCCAAAACC : 0xCC333355);
        g.fill(bx, by, bx + BTN_SZ, by + BTN_SZ, bg);
        for (int line = 0; line < 3; line++) {
            g.fill(bx + 2, by + 3 + line * 3, bx + BTN_SZ - 2, by + 4 + line * 3, 0xFFDDDDDD);
        }
    }

    private void renderMenuOptions(GuiGraphics g, HatPart hat, int cx, int cy,
            int mouseX, int mouseY) {
        HatDefinition def = HatRegistry.get(hat.getRegistryKey());
        boolean isCreative = mc.player != null && mc.player.getAbilities().instabuild;
        boolean hasAcc = def != null && !def.getAccessories().isEmpty()
                && (isCreative || !hat.accessories.isEmpty());
        boolean isFav = ClientFavourites.isFavourite(hat.name);

        int bx = cx + 1;
        int optY = cy + 1 + BTN_SZ + BTN_GAP;

        renderOptionBtn(g, bx, optY, "\uD83C\uDFA8", "C", 0xFF884400, mouseX, mouseY);
        optY += BTN_SZ + BTN_GAP;

        if (hasAcc) {
            renderOptionBtn(g, bx, optY, "+", "+", 0xFF004488, mouseX, mouseY);
            optY += BTN_SZ + BTN_GAP;
        }

        renderOptionBtn(g, bx, optY, isFav ? "\u2605" : "\u2606", isFav ? "\u2605" : "\u2606",
                isFav ? 0xFF885500 : 0xFF555555, mouseX, mouseY);
    }

    private void renderOptionBtn(GuiGraphics g, int bx, int by, String unicodeLabel, String fallback,
            int iconColor, int mouseX, int mouseY) {
        boolean hov = mouseX >= bx && mouseX < bx + BTN_SZ && mouseY >= by && mouseY < by + BTN_SZ;
        int bg = hov ? 0xCCBBBBDD : 0xCC222244;
        g.fill(bx, by, bx + BTN_SZ, by + BTN_SZ, iconColor | 0xBB000000);
        g.fill(bx + 1, by + 1, bx + BTN_SZ - 1, by + BTN_SZ - 1, bg);

        String label = fallback;
        float sc = 0.7f;
        g.pose().pushPose();
        g.pose().translate(bx + BTN_SZ / 2f, by + BTN_SZ / 2f, 300);
        g.pose().scale(sc, sc, 1);
        int tw = font.width(label);
        g.drawString(font, label, -tw / 2, -font.lineHeight / 2, 0xFFFFFF, true);
        g.pose().popPose();
    }

    private void renderPlayerInCell(GuiGraphics g, HatPart hat, int cx, int cy, Player player) {
        HatPart prev = ClientHatCache.getEntityHat(player.getUUID());

        HatPart show = hat.copy();
        show.isShowing = true;
        ClientHatCache.setEntityHat(player.getUUID(), show);

        int contentW = CELL_W - STRIP_W;
        int centerX = cx + contentW / 2 + 1;
        int footY = cy + CELL_H + 54;

        Quaternionf cam = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf entity = new Quaternionf().rotateY((float) Math.toRadians(-25.0));

        float prevBodyYaw = player.yBodyRot;
        float prevYRot = player.getYRot();
        float prevXRot = player.getXRot();
        float prevYHeadO = player.yHeadRotO;
        float prevYHead = player.yHeadRot;
        float prevWalkDist   = player.walkDist;
        float prevWalkDistO  = player.walkDistO;
        boolean prevOnGround = player.onGround();

        player.yBodyRot = 180f;
        player.setYRot(180f);
        player.setXRot(-8f);
        player.yHeadRot = 180f;
        player.yHeadRotO = 180f;
        player.walkDist  = 0f;
        player.walkDistO = 0f;
        player.setOnGround(true);

        g.enableScissor(cx + 1, cy + 1, cx + contentW - 1, cy + CELL_H - 1);
        LayerHat.RENDERING_HAT_GUI_PREVIEW.set(true);
        EpicFightCompat.GUI_PREVIEW_ACTIVE = true;
        InventoryScreen.renderEntityInInventory(g, centerX, footY, 42, cam, entity, player);
        EpicFightCompat.GUI_PREVIEW_ACTIVE = false;
        LayerHat.RENDERING_HAT_GUI_PREVIEW.set(false);
        g.disableScissor();

        player.yBodyRot = prevBodyYaw;
        player.setYRot(prevYRot);
        player.setXRot(prevXRot);
        player.yHeadRotO = prevYHeadO;
        player.yHeadRot = prevYHead;
        player.walkDist  = prevWalkDist;
        player.walkDistO = prevWalkDistO;
        player.setOnGround(prevOnGround);

        ClientHatCache.setEntityHat(player.getUUID(), prev);
    }

    private void renderVerticalName(GuiGraphics g, HatPart hat, int cx, int cy) {
        HatDefinition def = HatRegistry.get(hat.getRegistryKey());
        int stripX = cx + CELL_W - STRIP_W;
        g.fill(stripX, cy + 1, cx + CELL_W - 1, cy + CELL_H - 1, 0x99000000);

        float sc = 0.6f;
        int availW = (int) ((CELL_H - 6) / sc);
        int textW = font.width(hat.name);
        int yOff = -(int) (STRIP_W / sc / 2f + font.lineHeight / 2f - 1);
        int col = nameColorFor(def);

        g.enableScissor(stripX + 1, cy + 2, cx + CELL_W - 1, cy + CELL_H - 2);
        g.pose().pushPose();
        g.pose().translate(cx + CELL_W - 2, cy + CELL_H - 3, 200);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-90f));
        g.pose().scale(sc, sc, 1f);
        g.drawString(font, hat.name, (int)(2 - scrollOffset(textW, availW)), yOff, col, true);
        g.pose().popPose();
        g.disableScissor();
    }

    private void renderAccessoryView(GuiGraphics g, int mouseX, int mouseY) {
        if (accessoryViewHat == null)
            return;

        float[] savedCloth = EpicFightCompat.captureClothState(mc.player);
        renderAccessoryViewInner(g, mouseX, mouseY);
        EpicFightCompat.restoreClothState(mc.player, savedCloth);
    }

    private void renderAccessoryViewInner(GuiGraphics g, int mouseX, int mouseY) {
        boolean isCreative = mc.player != null && mc.player.getAbilities().instabuild;
        HatDefinition def = HatRegistry.get(accessoryViewHat.getRegistryKey());
        List<HatDefinition> accDefs;
        if (isCreative) {
            accDefs = (def != null) ? def.getAccessories() : List.of();
        } else {
            accDefs = new ArrayList<>();
            if (def != null) {
                for (HatDefinition ad : def.getAccessories()) {
                    for (HatPart ap : accessoryViewHat.accessories) {
                        if (ap.name.equals(ad.name)) { accDefs.add(ad); break; }
                    }
                }
            }
        }

        int totalW = gridW() + SCROLL_W + 1;
        g.fill(x, y, x + totalW, y + height, 0xFF3B3B3B);

        int mainW = totalW * 2 / 5;

        int backBtnH = 16;
        int mainContentH = height - backBtnH - 2;

        renderBigCard(g, accessoryViewHat, x, y, mainW, mainContentH, true, false, mouseX, mouseY);

        if (mc.player != null) {
            renderPlayerInCard(g, accessoryViewHat, x, y, mainW, mainContentH, mc.player);
        }
        renderVerticalNameInArea(g, accessoryViewHat.name,
                (def != null && def.meta.contributorUuid != null) ? 0x55FFFF : 0xDDDDDD,
                x, y, mainW, mainContentH);

        int backY = y + height - backBtnH;
        boolean backHov = mouseX >= x + 1 && mouseX < x + mainW - 1
                && mouseY >= backY + 1 && mouseY < backY + backBtnH - 1;
        g.fill(x + 1, backY + 1, x + mainW - 1, backY + backBtnH - 1, backHov ? 0xFFAAAAAA : 0xFF777777);
        g.fill(x + 1, backY, x + mainW - 1, backY + 1, 0xFF555555);
        g.drawString(font, "\u2190 Back", x + 4, backY + 4, 0xFFFFFF, false);

        int accX = x + mainW + 1;
        int accW = totalW - mainW - 1 - SCROLL_W;

        if (accDefs.isEmpty()) {
            g.drawString(font, "No accessories", accX + 4, y + 4, 0x888888, false);
            return;
        }

        int accH = Math.max(CELL_H, height / Math.max(1, accDefs.size()));
        int totalAccH = accDefs.size() * accH;
        int maxAccScroll = Math.max(0, totalAccH - height);
        if (accScrollOffset > maxAccScroll)
            accScrollOffset = maxAccScroll;

        Player player = mc.player;

        g.enableScissor(accX, y, accX + accW, y + height);

        for (int i = 0; i < accDefs.size(); i++) {
            HatDefinition accDef = accDefs.get(i);
            int ay = y + i * accH - accScrollOffset;

            if (ay + accH <= y || ay >= y + height)
                continue;

            boolean enabled = false;
            for (HatPart ap : accessoryViewHat.accessories) {
                if (ap.getRegistryKey().equals(accDef.getFullName())) {
                    enabled = ap.isShowing;
                    break;
                }
            }

            boolean blocked = !enabled && isBlockedByLayer(accDef, accessoryViewHat.accessories);

            boolean hov = !scrollDragging && !blocked && mouseX >= accX && mouseX < accX + accW
                    && mouseY >= ay && mouseY < ay + accH
                    && mouseY >= y && mouseY < y + height;

            renderBigCard(g, null, accX, ay, accW, accH, false, hov, mouseX, mouseY);

            if (enabled) {
                g.fill(accX + 1, ay + 1, accX + 5, ay + accH - 1, 0xFF44FF44);
            }

            if (blocked) {
                g.fill(accX + 1, ay + 1, accX + accW - 1, ay + accH - 1, 0x88000000);
            }

            if (player != null) {
                HatPart preview = new HatPart(accessoryViewHat.name);
                preview.registryKey = accessoryViewHat.getRegistryKey();
                preview.isShowing = true;
                HatPart accPart = new HatPart(accDef.name);
                accPart.registryKey = accDef.getFullName();
                accPart.isShowing = true;
                preview.accessories.add(accPart);
                renderPlayerInCard(g, preview, accX, ay, accW, accH, player);
            }

            renderVerticalNameInArea(g, accDef.name,
                    accDef.meta.contributorUuid != null ? 0x55FFFF : 0xDDDDDD,
                    accX, ay, accW, accH);
        }

        g.disableScissor();

        if (maxAccScroll > 0) {
            int sbX = accX + accW;
            g.fill(sbX, y, sbX + SCROLL_W, y + height, 0xFF2A2A2A);
            g.fill(sbX + 1, y + 1, sbX + SCROLL_W - 1, y + height - 1, 0xFF333333);

            int sbInner = height - 2;
            int thumbH = Math.max(14, sbInner * sbInner / (sbInner + maxAccScroll));
            int thumbY = y + 1 + (int) ((sbInner - thumbH) * (float) accScrollOffset / maxAccScroll);

            g.fill(sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + thumbH, 0xFF777777);
        }
    }

    private void renderBigCard(GuiGraphics g, @Nullable HatPart hat, int cx, int cy,
            int cw, int ch, boolean sel, boolean hov,
            int mouseX, int mouseY) {
        int bg = sel ? 0xFF1A3A6A : (hov ? 0xFF505060 : 0xFF404040);
        g.fill(cx, cy, cx + cw, cy + ch, bg);
        g.fill(cx, cy, cx + cw, cy + 1, 0xFF666666);
        g.fill(cx, cy, cx + 1, cy + ch, 0xFF666666);
        g.fill(cx, cy + ch - 1, cx + cw, cy + ch, 0xFF222222);
        g.fill(cx + cw - 1, cy, cx + cw, cy + ch, 0xFF222222);
    }

    private void renderPlayerInCard(GuiGraphics g, HatPart hat, int cx, int cy,
            int cw, int ch, Player player) {
        HatPart prev = ClientHatCache.getEntityHat(player.getUUID());

        HatPart show = hat.copy();
        show.isShowing = true;
        ClientHatCache.setEntityHat(player.getUUID(), show);

        int scale = Math.max(20, Math.min(cw / 2, ch / 3));

        Quaternionf cam = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf entity = new Quaternionf().rotateY((float) Math.toRadians(-25.0));

        float prevBodyYaw = player.yBodyRot;
        float prevYRot = player.getYRot();
        float prevXRot = player.getXRot();
        float prevYHeadO = player.yHeadRotO;
        float prevYHead = player.yHeadRot;
        float prevWalkDist   = player.walkDist;
        float prevWalkDistO  = player.walkDistO;
        boolean prevOnGround = player.onGround();

        player.yBodyRot = 180f;
        player.setYRot(180f);
        player.setXRot(-8f);
        player.yHeadRot = 180f;
        player.yHeadRotO = 180f;
        player.walkDist  = 0f;
        player.walkDistO = 0f;
        player.setOnGround(true);

        g.enableScissor(cx + 1, cy + 1, cx + cw - STRIP_W - 1, cy + ch - 1);
        LayerHat.RENDERING_HAT_GUI_PREVIEW.set(true);
        EpicFightCompat.GUI_PREVIEW_ACTIVE = true;
        InventoryScreen.renderEntityInInventory(g, cx + (cw - STRIP_W) / 2, cy + ch - 4, scale, cam, entity, player);
        EpicFightCompat.GUI_PREVIEW_ACTIVE = false;
        LayerHat.RENDERING_HAT_GUI_PREVIEW.set(false);
        g.disableScissor();

        player.yBodyRot = prevBodyYaw;
        player.setYRot(prevYRot);
        player.setXRot(prevXRot);
        player.yHeadRotO = prevYHeadO;
        player.yHeadRot = prevYHead;
        player.walkDist  = prevWalkDist;
        player.walkDistO = prevWalkDistO;
        player.setOnGround(prevOnGround);

        ClientHatCache.setEntityHat(player.getUUID(), prev);
    }

    private void renderVerticalNameInArea(GuiGraphics g, String name, int col,
            int cx, int cy, int cw, int ch) {
        int stripX = cx + cw - STRIP_W;
        g.fill(stripX, cy + 1, cx + cw - 1, cy + ch - 1, 0x99000000);

        float sc = 0.6f;
        int availW = (int) ((ch - 6) / sc);
        int textW = font.width(name);
        int yOff = -(int) (STRIP_W / sc / 2f + font.lineHeight / 2f - 1);

        g.enableScissor(stripX + 1, cy + 2, cx + cw - 1, cy + ch - 2);
        g.pose().pushPose();
        g.pose().translate(cx + cw - 2, cy + ch - 3, 200);
        g.pose().mulPose(Axis.ZP.rotationDegrees(-90f));
        g.pose().scale(sc, sc, 1f);
        g.drawString(font, name, (int)(2 - scrollOffset(textW, availW)), yOff, col, true);
        g.pose().popPose();
        g.disableScissor();
    }

    private float scrollOffset(int textW, int availW) {
        if (textW <= availW) return 0f;
        float overflow = textW - availW;
        float scrollMs  = overflow / 30f * 1000f;
        float pauseMs   = 1000f;
        long  period    = Math.max(100L, (long)(2 * (scrollMs + pauseMs)));
        long  t         = System.currentTimeMillis() % period;
        if (t < pauseMs)                          return 0f;
        if (t < pauseMs + scrollMs)               return (t - pauseMs) / scrollMs * overflow;
        if (t < 2 * pauseMs + scrollMs)           return overflow;
        return (1f - (t - 2 * pauseMs - scrollMs) / scrollMs) * overflow;
    }

    private int nameColorFor(@org.jetbrains.annotations.Nullable HatDefinition def) {
        if (def == null) return 0xFFFFFF;
        if (def.meta.contributorUuid != null) return 0x55FFFF;
        Integer c = def.getRarity().colour.getColor();
        return c != null ? c : 0xFFFFFF;
    }

    private boolean isBlockedByLayer(HatDefinition candidate, List<HatPart> equipped) {
        if (candidate.meta.accessoryLayers.isEmpty())
            return false;
        for (HatPart ap : equipped) {
            if (!ap.isShowing)
                continue;
            if (ap.name.equals(candidate.name))
                continue;
            HatDefinition other = HatRegistry.get(ap.getRegistryKey());
            if (other == null)
                continue;
            for (String layer : candidate.meta.accessoryLayers) {
                if (other.meta.accessoryLayers.contains(layer))
                    return true;
            }
        }
        return false;
    }

    private void renderScrollbar(GuiGraphics g, int mouseX, int mouseY) {
        int sbX = x + gridW();
        g.fill(sbX, y, sbX + SCROLL_W, y + height, 0xFF2A2A2A);
        g.fill(sbX + 1, y + 1, sbX + SCROLL_W - 1, y + height - 1, 0xFF333333);

        int max = maxScroll();
        if (max <= 0)
            return;

        int sbInner = height - 2;
        int thumbH = Math.max(14, sbInner * sbInner / (sbInner + max));
        int thumbY = y + 1 + (int) ((sbInner - thumbH) * (float) scrollOffset / max);

        boolean over = mouseX >= sbX && mouseX < sbX + SCROLL_W
                && mouseY >= thumbY && mouseY < thumbY + thumbH;
        int col = (scrollDragging || over) ? 0xFFAAAAAA : 0xFF777777;

        g.fill(sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + thumbH, col);
        g.fill(sbX + 1, thumbY, sbX + SCROLL_W - 1, thumbY + 1, 0xFFCCCCCC);
        g.fill(sbX + 1, thumbY, sbX + 2, thumbY + thumbH, 0xFFCCCCCC);
    }

    private int thumbH() {
        int sbInner = height - 2;
        int max = maxScroll();
        return max <= 0 ? sbInner : Math.max(14, sbInner * sbInner / (sbInner + max));
    }

    private boolean isInGrid(double mx, double my) {
        return mx >= x && mx < x + gridW() && my >= y && my < y + height;
    }

    private boolean isInScrollbar(double mx, double my) {
        int sbX = x + gridW();
        return mx >= sbX && mx < sbX + SCROLL_W && my >= y && my < y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !isMouseOver(mouseX, mouseY))
            return false;

        if (button == 0 && inAccessoryView) {
            return handleAccessoryViewClick(mouseX, mouseY);
        }

        if (button == 0 && isInScrollbar(mouseX, mouseY)) {
            scrollDragging = true;
            scrollDragStartY = (int) mouseY;
            scrollDragStartOffset = scrollOffset;
            return true;
        }

        if (!isInGrid(mouseX, mouseY))
            return false;

        int relX = (int) (mouseX - x);
        int relY = (int) (mouseY - y) + scrollOffset;
        int col = relX / CELL_W;
        int row = relY / CELL_H;
        int index = row * cols + col;

        if (col >= cols || index < 0 || index >= filtered.size()) {
            menuOpenIdx = -1;
            return true;
        }

        int cx = x + col * CELL_W;
        int cy = y + row * CELL_H - scrollOffset;

        if (button == 0) {
            if (mouseX >= cx + 1 && mouseX < cx + 1 + BTN_SZ
                    && mouseY >= cy + 1 && mouseY < cy + 1 + BTN_SZ) {
                menuOpenIdx = (menuOpenIdx == index) ? -1 : index;
                return true;
            }

            if (menuOpenIdx == index) {
                if (handleMenuOptionClick(filtered.get(index), cx, cy, mouseX, mouseY)) {
                    return true;
                }
            }
        }

        menuOpenIdx = -1;

        HatPart clicked = filtered.get(index);
        selected = (selected != null && selected.name.equals(clicked.name)) ? null : clicked;
        onSelect.accept(selected);
        return true;
    }

    private boolean handleMenuOptionClick(HatPart hat, int cx, int cy,
            double mouseX, double mouseY) {
        HatDefinition def = HatRegistry.get(hat.getRegistryKey());
        boolean isCreative = mc.player != null && mc.player.getAbilities().instabuild;
        boolean hasAcc = def != null && !def.getAccessories().isEmpty()
                && (isCreative || !hat.accessories.isEmpty());

        int bx = cx + 1;
        int optY = cy + 1 + BTN_SZ + BTN_GAP;

        if (mouseX >= bx && mouseX < bx + BTN_SZ && mouseY >= optY && mouseY < optY + BTN_SZ) {
            selected = hat;
            onSelect.accept(selected);
            menuOpenIdx = -1;
            onColorRequested.run();
            return true;
        }
        optY += BTN_SZ + BTN_GAP;

        if (hasAcc) {
            if (mouseX >= bx && mouseX < bx + BTN_SZ && mouseY >= optY && mouseY < optY + BTN_SZ) {
                enterAccessoryView(hat);
                return true;
            }
            optY += BTN_SZ + BTN_GAP;
        }

        if (mouseX >= bx && mouseX < bx + BTN_SZ && mouseY >= optY && mouseY < optY + BTN_SZ) {
            ClientFavourites.toggle(hat.name);
            menuOpenIdx = -1;
            return true;
        }

        return false;
    }

    private boolean handleAccessoryViewClick(double mouseX, double mouseY) {
        if (accessoryViewHat == null)
            return false;

        boolean isCreative = mc.player != null && mc.player.getAbilities().instabuild;
        HatDefinition def = HatRegistry.get(accessoryViewHat.getRegistryKey());
        List<HatDefinition> accDefs;
        if (isCreative) {
            accDefs = (def != null) ? def.getAccessories() : List.of();
        } else {
            accDefs = new ArrayList<>();
            if (def != null) {
                for (HatDefinition ad : def.getAccessories()) {
                    for (HatPart ap : accessoryViewHat.accessories) {
                        if (ap.name.equals(ad.name)) { accDefs.add(ad); break; }
                    }
                }
            }
        }

        int totalW = gridW() + SCROLL_W + 1;
        int mainW = totalW * 2 / 5;
        int backBtnH = 16;
        int backY = y + height - backBtnH;

        if (mouseX >= x + 1 && mouseX < x + mainW - 1
                && mouseY >= backY + 1 && mouseY < backY + backBtnH - 1) {
            exitAccessoryView();
            return true;
        }

        int accX = x + mainW + 1;
        int accW = totalW - mainW - 1 - SCROLL_W;
        int accH = accDefs.isEmpty() ? height : Math.max(CELL_H, height / Math.max(1, accDefs.size()));

        if (mouseX >= accX && mouseX < accX + accW && mouseY >= y && mouseY < y + height) {
            int relY = (int) (mouseY - y) + accScrollOffset;
            int index = relY / accH;
            if (index >= 0 && index < accDefs.size()) {
                HatDefinition accDef = accDefs.get(index);

                boolean currentlyOn = false;
                for (HatPart ap : accessoryViewHat.accessories) {
                    if (ap.getRegistryKey().equals(accDef.getFullName())) {
                        currentlyOn = ap.isShowing;
                        break;
                    }
                }

                if (!currentlyOn && isBlockedByLayer(accDef, accessoryViewHat.accessories)) {
                    return true;
                }

                boolean turningOn = !currentlyOn;

                if (turningOn && !accDef.meta.accessoryLayers.isEmpty()) {
                    for (HatPart ap : accessoryViewHat.accessories) {
                        if (ap.getRegistryKey().equals(accDef.getFullName()))
                            continue;
                        HatDefinition otherDef = HatRegistry.get(ap.getRegistryKey());
                        if (otherDef == null)
                            continue;
                        for (String layer : accDef.meta.accessoryLayers) {
                            if (otherDef.meta.accessoryLayers.contains(layer)) {
                                ap.isShowing = false;
                                break;
                            }
                        }
                    }
                }

                boolean found = false;
                for (HatPart ap : accessoryViewHat.accessories) {
                    if (ap.getRegistryKey().equals(accDef.getFullName())) {
                        ap.isShowing = !ap.isShowing;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    HatPart newAcc = new HatPart(accDef.name);
                    newAcc.registryKey = accDef.getFullName();
                    newAcc.isShowing = true;
                    accessoryViewHat.accessories.add(newAcc);
                }

                onSelect.accept(accessoryViewHat);
                return true;
            }
        }

        return true;
    }

    private void enterAccessoryView(HatPart hat) {
        HatDefinition def = HatRegistry.get(hat.getRegistryKey());

        accessoryViewHat = hat.copy();
        accessoryViewHat.isShowing = true;

        boolean isCreative = mc.player != null && mc.player.getAbilities().instabuild;
        if (isCreative && def != null && accessoryViewHat.accessories.isEmpty()) {
            for (HatDefinition accDef : def.getAccessories()) {
                HatPart ap = new HatPart(accDef.name);
                ap.registryKey = accDef.getFullName();
                ap.isShowing = false;
                accessoryViewHat.accessories.add(ap);
            }
        }

        selected = accessoryViewHat;
        menuOpenIdx = -1;
        accScrollOffset = 0;
        inAccessoryView = true;
        onSelect.accept(accessoryViewHat);
    }

    private void exitAccessoryView() {
        inAccessoryView = false;
        accessoryViewHat = null;
        accScrollOffset = 0;
        selected = null;
        onSelect.accept(null);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0)
            scrollDragging = false;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible || button != 0 || !scrollDragging)
            return false;

        int max = maxScroll();
        int travel = height - 2 - thumbH();
        if (travel <= 0)
            return true;

        int delta = (int) mouseY - scrollDragStartY;
        scrollOffset = (int) Math.max(0, Math.min(max, scrollDragStartOffset + (long) delta * max / travel));
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible || !isMouseOver(mouseX, mouseY))
            return false;
        if (inAccessoryView) {
            HatDefinition def = accessoryViewHat != null ? HatRegistry.get(accessoryViewHat.getRegistryKey()) : null;
            List<HatDefinition> accDefs = (def != null) ? def.getAccessories() : List.of();
            int accH = Math.max(CELL_H, height / Math.max(1, accDefs.size()));
            int maxAccScroll = Math.max(0, accDefs.size() * accH - height);
            accScrollOffset = (int) Math.max(0, Math.min(maxAccScroll, accScrollOffset - delta * 12));
        } else {
            scrollOffset = (int) Math.max(0, Math.min(maxScroll(), scrollOffset - delta * 12));
        }
        return true;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    @Override
    public NarrationPriority narrationPriority() {
        return NarrationPriority.HOVERED;
    }

    @Override
    public void updateNarration(NarrationElementOutput o) {
    }
}
