package me.guivnf.mods.hats.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.client.event.ClientEventHandler;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.packet.PacketHatCustomisation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HatsScreen extends Screen
{
    private static final int PANEL_PAD    = 6;
    private static final int PANEL_INSET  = 4;
    private static final int SCREEN_MARGIN = 5;

    @Nullable private HatGridWidget hatGrid;
    @Nullable private EditBox searchBox;
    @Nullable private Button equipButton;
    @Nullable private Button removeButton;
    @Nullable private Button tradeButton;
    @Nullable private Button sortButton;

    @Nullable private HatPart selectedHat;
    @Nullable private HatPart prevHatInCache;
    @Nullable private UUID playerUuid;

    private static final ResourceLocation ICON_CANCEL =
            new ResourceLocation("hats", "textures/icon/cancel.png");
    private static final ResourceLocation ICON_SORT =
            new ResourceLocation("hats", "textures/icon/categories.png");
    private static final ResourceLocation ICON_TRADE =
            new ResourceLocation("hats", "textures/icon/trade.png");

    private static final int SORT_ALPHA       = 0;
    private static final int SORT_RARITY      = 1;
    private static final int SORT_UNLOCKED    = 2;
    private static final int SORT_ACCESSORIES = 3;
    private static final String[] SORT_LABELS = {
        "Alphabetical", "By Rarity", "By Unlocked", "Has Accessories"
    };
    private static final int SORT_MENU_W      = 104;
    private static final int SORT_MENU_ITEM_H = 13;
    private static final int SORT_MENU_PAD    = 4;

    private boolean colorPanelOpen = false;
    private boolean sortMenuOpen   = false;
    private int draggingSlider = -1;
    private int sortOrder = SORT_ALPHA;

    private static final int FORCED_SCALE = 4;
    private int savedGuiScale = -1;

    private HatsScreen()
    {
        super(Component.translatable("hats.gui.title"));
    }

    public static void open()
    {
        ClientEventHandler.guiYaw   = 0f;
        ClientEventHandler.guiPitch = 0f;
        ClientEventHandler.guiZoom  = 0f;
        ClientEventHandler.guiPanX  = 0f;
        ClientEventHandler.guiPanY  = 0f;
        Minecraft.getInstance().setScreen(new HatsScreen());
    }

    private static final int GRID_COLS     = 3;
    private static final int FIXED_PANEL_W = GRID_COLS * HatGridWidget.CELL_W
            + HatGridWidget.SCROLL_W + 1
            + (PANEL_INSET + PANEL_PAD) * 2;

    private static final int SIDE_BTN_SIZE = 20;
    private static final int SIDE_BTN_GAP  = 4;

    private int panelX() { return width - SCREEN_MARGIN - FIXED_PANEL_W; }
    private int panelY() { return SCREEN_MARGIN; }
    private int panelW() { return FIXED_PANEL_W; }
    private int panelH() { return height - SCREEN_MARGIN * 2; }
    private int innerX() { return panelX() + PANEL_INSET + PANEL_PAD; }
    private int innerW() { return panelW() - (PANEL_INSET + PANEL_PAD) * 2; }
    private int innerY() { return panelY() + PANEL_INSET + PANEL_PAD; }
    private int sideX()  { return panelX() - SCREEN_MARGIN - SIDE_BTN_SIZE; }
    private boolean isInPreviewArea(double mouseX) {
        return mouseX < sideX();
    }

    @Override
    protected void init()
    {
        int autoScale    = minecraft.getWindow().calculateScale(0, minecraft.isEnforceUnicode());
        int effectiveScale = Math.min(FORCED_SCALE, autoScale);
        if (savedGuiScale == -1) {
            savedGuiScale = (int) Math.round(minecraft.getWindow().getGuiScale());
        }
        minecraft.getWindow().setGuiScale(effectiveScale);
        this.width  = minecraft.getWindow().getGuiScaledWidth();
        this.height = minecraft.getWindow().getGuiScaledHeight();

        LocalPlayer player = minecraft.player;

        if (player != null) {
            playerUuid = player.getUUID();
            prevHatInCache = ClientHatCache.getEntityHat(playerUuid);
        }

        int ix  = innerX();
        int iw  = innerW();
        int iy  = innerY();
        int searchH = 18;
        int btnH    = 20;

        searchBox = addRenderableWidget(new EditBox(font,
            ix, iy, iw, searchH,
            Component.translatable("hats.gui.search")));
        searchBox.setHint(Component.translatable("hats.gui.search"));
        searchBox.setResponder(q -> { if (hatGrid != null) hatGrid.setFilter(q); });

        int gridTop    = iy + searchH + PANEL_PAD;
        int gridBottom = panelY() + panelH() - PANEL_INSET - PANEL_PAD - btnH - PANEL_PAD;

        hatGrid = new HatGridWidget(minecraft, ix, gridTop, iw, gridBottom - gridTop,
            this::onHatSelected, this::openColorPanel);
        hatGrid.refreshHats(buildAllHatsList());
        addWidget(hatGrid);

        int btnY = panelY() + panelH() - PANEL_INSET - PANEL_PAD - btnH;

        equipButton = addRenderableWidget(Button.builder(
            Component.translatable("hats.gui.equip"), b -> equipSelected())
            .bounds(ix, btnY, iw, btnH).build());

        int sx = sideX();
        int sy = innerY();

        removeButton = addRenderableWidget(Button.builder(
            Component.empty(), b -> removeEquipped())
            .tooltip(Tooltip.create(Component.literal("Remove")))
            .bounds(sx, sy, SIDE_BTN_SIZE, SIDE_BTN_SIZE).build());

        tradeButton = addRenderableWidget(Button.builder(
            Component.empty(), b -> openTradeGui())
            .tooltip(Tooltip.create(Component.literal("Trade")))
            .bounds(sx, sy + SIDE_BTN_SIZE + SIDE_BTN_GAP, SIDE_BTN_SIZE, SIDE_BTN_SIZE).build());

        sortButton = addRenderableWidget(Button.builder(
            Component.empty(), b -> openSortMenu())
            .tooltip(Tooltip.create(Component.literal("Sort")))
            .bounds(sx, sy + (SIDE_BTN_SIZE + SIDE_BTN_GAP) * 2, SIDE_BTN_SIZE, SIDE_BTN_SIZE).build());

        updateButtons();
    }

    @Override
    public void onClose()
    {
        if (playerUuid != null) {
            ClientHatCache.setEntityHat(playerUuid, prevHatInCache);
        }
        if (savedGuiScale != -1) {
            minecraft.getWindow().setGuiScale(savedGuiScale);
            savedGuiScale = -1;
        }
        super.onClose();
    }

    private void openColorPanel()
    {
        colorPanelOpen = true;
        draggingSlider = -1;
    }

    private List<HatPart> buildAllHatsList()
    {
        List<HatPart> result = new ArrayList<>();

        LocalPlayer player = minecraft.player;
        boolean isCreative = player != null && player.getAbilities().instabuild;

        if (isCreative) {
            for (HatDefinition def : HatRegistry.getAll()) {
                if (def.isAccessory()) continue;
                HatPart hat = new HatPart(def.name);
                hat.registryKey = def.getFullName();
                hat.count = 1;
                result.add(hat);
            }
        } else {
            for (HatPart hat : ClientHatCache.getLocalInventory()) {
                if (hat.count <= 0) continue;
                HatDefinition def = HatRegistry.get(hat.getRegistryKey());
                if (def == null || def.isAccessory()) continue;
                result.add(hat);
            }
        }

        result.sort((a, b) -> {
            boolean af = ClientFavourites.isFavourite(a.name);
            boolean bf = ClientFavourites.isFavourite(b.name);
            if (af != bf) return af ? -1 : 1;

            return switch (sortOrder) {
                case SORT_RARITY -> {
                    HatDefinition da = HatRegistry.get(a.getRegistryKey());
                    HatDefinition db = HatRegistry.get(b.getRegistryKey());
                    if (da == null || db == null) yield 0;
                    boolean ac = da.meta.contributorUuid != null;
                    boolean bc = db.meta.contributorUuid != null;
                    if (ac != bc) yield ac ? -1 : 1;
                    int rc = db.getRarity().ordinal() - da.getRarity().ordinal();
                    yield rc != 0 ? rc : sortKey(a.name).compareToIgnoreCase(sortKey(b.name));
                }
                case SORT_UNLOCKED -> {
                    int rc = Integer.compare(b.count, a.count);
                    yield rc != 0 ? rc : sortKey(a.name).compareToIgnoreCase(sortKey(b.name));
                }
                case SORT_ACCESSORIES -> {
                    HatDefinition da = HatRegistry.get(a.getRegistryKey());
                    HatDefinition db = HatRegistry.get(b.getRegistryKey());
                    boolean aHasAcc = da != null && !da.getAccessories().isEmpty();
                    boolean bHasAcc = db != null && !db.getAccessories().isEmpty();
                    if (aHasAcc != bHasAcc) yield aHasAcc ? -1 : 1;
                    yield sortKey(a.name).compareToIgnoreCase(sortKey(b.name));
                }
                default -> sortKey(a.name).compareToIgnoreCase(sortKey(b.name));
            };
        });

        return result;
    }

    private void onHatSelected(@Nullable HatPart hat)
    {
        selectedHat = hat != null ? hat.copy() : null;
        if (selectedHat != null) {
            boolean isCreative = minecraft.player != null && minecraft.player.getAbilities().instabuild;
            if (isCreative) {
                HatDefinition def = HatRegistry.get(selectedHat.getRegistryKey());
                if (def != null) {
                    java.util.Map<String, Boolean> prevShowing = new java.util.HashMap<>();
                    for (HatPart acc : selectedHat.accessories) {
                        prevShowing.put(acc.name, acc.isShowing);
                    }
                    selectedHat.accessories.clear();
                    for (HatDefinition accDef : def.getAccessories()) {
                        HatPart accPart = new HatPart(accDef.name);
                        accPart.registryKey = accDef.getFullName();
                        accPart.isShowing = prevShowing.getOrDefault(accDef.name, false);
                        selectedHat.accessories.add(accPart);
                    }
                }
            }
        }
        updatePreviewHat();
        updateButtons();
    }

    private void updatePreviewHat()
    {
        if (playerUuid == null) return;

        if (selectedHat != null) {
            HatPart show = selectedHat.copy();
            show.isShowing = true;
            ClientHatCache.setEntityHat(playerUuid, show);
        } else {
            ClientHatCache.setEntityHat(playerUuid, prevHatInCache);
        }
    }

    private void equipSelected()
    {
        if (selectedHat == null) return;
        selectedHat.isShowing = true;
        NetworkManager.sendToServer(HatsNetwork.HAT_CUSTOMISATION,
            PacketHatCustomisation.encode(selectedHat));
        prevHatInCache = selectedHat.copy();
        updateButtons();
    }

    private void removeEquipped()
    {
        NetworkManager.sendToServer(HatsNetwork.HAT_CUSTOMISATION,
            PacketHatCustomisation.encode(null));
        prevHatInCache = null;
        selectedHat = null;
        if (playerUuid != null) ClientHatCache.setEntityHat(playerUuid, null);
        updateButtons();
    }

    private static String sortKey(String name)
    {
        if (name.startsWith("(")) {
            int end = name.indexOf(')');
            if (end > 0 && end + 1 < name.length())
                return name.substring(end + 1).trim();
        }
        return name;
    }

    private void openSortMenu()
    {
        sortMenuOpen = !sortMenuOpen;
    }

    private void updateButtons()
    {
        boolean equipped = selectedHat != null && matchesEquipped(selectedHat, prevHatInCache);
        if (equipButton != null) {
            equipButton.active = selectedHat != null && !equipped;
            equipButton.setMessage(equipped
                ? Component.translatable("hats.gui.equipped")
                : Component.translatable("hats.gui.equip"));
        }
        if (removeButton != null) {
            removeButton.active = prevHatInCache != null;
        }
    }

    private static boolean matchesEquipped(HatPart a, HatPart b)
    {
        if (a == null || b == null) return false;
        if (!a.getRegistryKey().equals(b.getRegistryKey())) return false;
        if (a.enchanted != b.enchanted) return false;
        for (int i = 0; i < 4; i++) if (a.colour[i] != b.colour[i]) return false;
        for (int i = 0; i < 3; i++) if (a.hsb[i] != b.hsb[i]) return false;
        if (a.accessories.size() != b.accessories.size()) return false;
        for (HatPart accA : a.accessories) {
            boolean found = false;
            for (HatPart accB : b.accessories) {
                if (accA.name.equals(accB.name) && accA.isShowing == accB.isShowing) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        drawPanel(g, panelX(), panelY(), panelW(), panelH());

        super.render(g, mouseX, mouseY, partialTick);

        if (hatGrid != null) {
            hatGrid.render(g, mouseX, mouseY, partialTick);
        }

        if (removeButton != null && removeButton.visible) {
            int pad = 2;
            g.blit(ICON_CANCEL,
                removeButton.getX() + pad, removeButton.getY() + pad,
                0, 0,
                SIDE_BTN_SIZE - pad * 2, SIDE_BTN_SIZE - pad * 2,
                SIDE_BTN_SIZE - pad * 2, SIDE_BTN_SIZE - pad * 2);
        }
        if (tradeButton != null && tradeButton.visible) {
            int pad = 2;
            g.blit(ICON_TRADE,
                tradeButton.getX() + pad, tradeButton.getY() + pad,
                0, 0,
                SIDE_BTN_SIZE - pad * 2, SIDE_BTN_SIZE - pad * 2,
                SIDE_BTN_SIZE - pad * 2, SIDE_BTN_SIZE - pad * 2);
        }
        if (sortButton != null && sortButton.visible) {
            int pad = 2;
            g.blit(ICON_SORT,
                sortButton.getX() + pad, sortButton.getY() + pad,
                0, 0,
                SIDE_BTN_SIZE - pad * 2, SIDE_BTN_SIZE - pad * 2,
                SIDE_BTN_SIZE - pad * 2, SIDE_BTN_SIZE - pad * 2);
        }

        if (sortMenuOpen) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 200);
            renderSortMenu(g, mouseX, mouseY);
            g.pose().popPose();
        }

        if (colorPanelOpen) {
            g.pose().pushPose();
            g.pose().translate(0, 0, 400);
            RenderSystem.disableDepthTest();
            renderColorPanel(g, mouseX, mouseY);
            RenderSystem.enableDepthTest();
            g.pose().popPose();
        }
    }

    private void renderSortMenu(GuiGraphics g, int mouseX, int mouseY)
    {
        if (sortButton == null) return;

        int menuH = SORT_MENU_PAD * 2 + SORT_LABELS.length * SORT_MENU_ITEM_H;
        int mx = sortButton.getX() - SORT_MENU_W - 2;
        int my = sortButton.getY();

        g.fill(mx - 1, my - 1, mx + SORT_MENU_W + 1, my + menuH + 1, 0xFF555555);
        g.fill(mx,     my,     mx + SORT_MENU_W,     my + menuH,     0xFF2B2B2B);

        for (int i = 0; i < SORT_LABELS.length; i++) {
            int iy = my + SORT_MENU_PAD + i * SORT_MENU_ITEM_H;
            boolean hovered = mouseX >= mx && mouseX < mx + SORT_MENU_W
                           && mouseY >= iy && mouseY < iy + SORT_MENU_ITEM_H;
            boolean selected = sortOrder == i;

            if (hovered)  g.fill(mx, iy, mx + SORT_MENU_W, iy + SORT_MENU_ITEM_H, 0xFF3D5A80);
            if (selected) g.fill(mx, iy, mx + SORT_MENU_W, iy + SORT_MENU_ITEM_H,
                    hovered ? 0xFF4A7AB5 : 0xFF2D4A6A);

            int textCol = selected ? 0xFFFFD700 : (hovered ? 0xFFFFFFFF : 0xFFCCCCCC);
            g.drawString(font, SORT_LABELS[i], mx + SORT_MENU_PAD, iy + 2, textCol, false);
        }
    }

    private void drawPanel(GuiGraphics g, int x, int y, int w, int h)
    {
        g.fill(x,     y,     x + w,     y + h,     0xFF8B8B8B);
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFC6C6C6);
        g.fill(x + 1, y + 1, x + w - 1, y + 2,     0xFFFFFFFF);
        g.fill(x + 1, y + 1, x + 2,     y + h - 1, 0xFFFFFFFF);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF555555);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF555555);

        g.fill(x,     y,     x + 2, y + 1, 0x00000000);
        g.fill(x,     y,     x + 1, y + 2, 0x00000000);
        g.fill(x + w - 2, y,     x + w, y + 1, 0x00000000);
        g.fill(x + w - 1, y,     x + w, y + 2, 0x00000000);
        g.fill(x,     y + h - 1, x + 2, y + h, 0x00000000);
        g.fill(x,     y + h - 2, x + 1, y + h, 0x00000000);
        g.fill(x + w - 2, y + h - 1, x + w, y + h, 0x00000000);
        g.fill(x + w - 1, y + h - 2, x + w, y + h, 0x00000000);
    }

    private void renderColorPanel(GuiGraphics g, int mouseX, int mouseY)
    {
        int cx = innerX();
        int cy = innerY() + 44;
        int cw = innerW();
        int panelH = 2 + 7 * 14 + 14;

        g.fill(cx - 2, cy - 2, cx + cw + 2, cy + panelH + 2, 0xFF555555);
        g.fill(cx - 1, cy - 1, cx + cw + 1, cy + panelH + 1, 0xFF3B3B3B);

        int closeX = cx + cw - 10;
        boolean hoverClose = mouseX >= closeX && mouseX < closeX + 10
                          && mouseY >= cy     && mouseY < cy + 10;
        g.fill(closeX, cy, closeX + 10, cy + 10, hoverClose ? 0xFFAA3333 : 0xFF773333);
        g.drawString(font, "X", closeX + 2, cy + 1, 0xFFFFFF, false);

        if (selectedHat == null) {
            g.drawString(font, "Select a hat first", cx + 4, cy + 6, 0x888888, false);
            return;
        }

        float[] col = selectedHat.colour;
        float[] hsb = selectedHat.hsb;

        String[] labels   = { "R", "G", "B", "A", "H", "S", "B" };
        float[]  vals     = { col[0], col[1], col[2], col[3], hsb[0], hsb[1], hsb[2] };
        int[]    barColors = {
            0xFFFF4444, 0xFF44FF44, 0xFF4444FF, 0xFFFFFFFF,
            0xFFFFAA00, 0xFF88FF88, 0xFFFFFF44
        };

        int labelW = 10;
        int valW   = 28;
        int sliderW = cw - labelW - 2 - 2 - valW;

        for (int i = 0; i < 7; i++) {
            int ry = cy + 2 + i * 14;
            g.drawString(font, labels[i], cx, ry + 2, 0xCCCCCC, false);

            int bx = cx + labelW + 2;
            int by = ry + 1;
            int bh = 10;

            g.fill(bx, by, bx + sliderW, by + bh, 0xFF222222);
            g.fill(bx + 1, by + 1, bx + sliderW - 1, by + bh - 1, 0xFF444444);

            int filled = (int)((vals[i] + (i < 4 ? 0f : 1f)) / (i < 4 ? 1f : 2f) * (sliderW - 2));
            filled = Mth.clamp(filled, 0, sliderW - 2);
            if (filled > 0) {
                g.fill(bx + 1, by + 1, bx + 1 + filled, by + bh - 1, barColors[i] | 0xFF000000);
            }

            int knobX = bx + 1 + filled;
            g.fill(knobX - 1, by, knobX + 2, by + bh, 0xFFFFFFFF);

            String valStr = String.format("%.2f", vals[i]);
            g.drawString(font, valStr, bx + sliderW + 2, ry + 2, 0xCCCCCC, false);
        }

        int resetY = cy + 2 + 7 * 14 + 2;
        boolean hoverReset = mouseX >= cx && mouseX < cx + 40 && mouseY >= resetY && mouseY < resetY + 10;
        g.fill(cx, resetY, cx + 40, resetY + 10, hoverReset ? 0xFFAAAAAA : 0xFF777777);
        g.drawString(font, "Reset", cx + 2, resetY + 1, 0xFFFFFF, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta)
    {
        if (isInPreviewArea(mouseX)) {
            ClientEventHandler.guiZoom = Mth.clamp(
                ClientEventHandler.guiZoom + (float)(delta * 0.12), 0f, 1.5f);
            return true;
        }

        if (hatGrid != null && hatGrid.isMouseOver(mouseX, mouseY)) {
            return hatGrid.mouseScrolled(mouseX, mouseY, delta);
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0 && sortMenuOpen && sortButton != null) {
            int menuH = SORT_MENU_PAD * 2 + SORT_LABELS.length * SORT_MENU_ITEM_H;
            int mx = sortButton.getX() - SORT_MENU_W - 2;
            int my = sortButton.getY();

            if (mouseX >= mx && mouseX < mx + SORT_MENU_W
                    && mouseY >= my && mouseY < my + menuH) {
                int idx = (int)(mouseY - my - SORT_MENU_PAD) / SORT_MENU_ITEM_H;
                if (idx >= 0 && idx < SORT_LABELS.length) {
                    sortOrder = idx;
                    if (hatGrid != null) hatGrid.refreshHats(buildAllHatsList());
                }
                sortMenuOpen = false;
                return true;
            }
            sortMenuOpen = false;
        }

        if (button == 0 && colorPanelOpen) {
            int cx = innerX();
            int cy = innerY() + 44;
            int cw = innerW();
            int panelH = 2 + 7 * 14 + 14;

            int closeX = cx + cw - 10;
            if (mouseX >= closeX && mouseX < closeX + 10 && mouseY >= cy && mouseY < cy + 10) {
                colorPanelOpen = false;
                draggingSlider = -1;
                return true;
            }

            if (handleColorClick(mouseX, mouseY)) return true;

            boolean insidePanel = mouseX >= cx - 2 && mouseX < cx + cw + 2
                               && mouseY >= cy - 2 && mouseY < cy + panelH + 2;
            if (!insidePanel) {
                colorPanelOpen = false;
                draggingSlider = -1;
            }

            return true;
        }

        if (hatGrid != null && hatGrid.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean handleColorClick(double mouseX, double mouseY)
    {
        if (selectedHat == null) return false;

        int cx = innerX();
        int cy = innerY() + 44;
        int cw = innerW();
        int sliderW = cw - 10 - 2 - 2 - 28;
        int bx = cx + 10 + 2;

        for (int i = 0; i < 7; i++) {
            int by = cy + 2 + i * 14 + 1;
            if (mouseY >= by && mouseY < by + 10 && mouseX >= bx && mouseX < bx + sliderW) {
                draggingSlider = i;
                applySlider(i, (float)(mouseX - bx - 1) / (sliderW - 2));
                return true;
            }
        }

        int resetY = cy + 2 + 7 * 14 + 2;
        if (mouseY >= resetY && mouseY < resetY + 10 && mouseX >= cx && mouseX < cx + 40) {
            selectedHat.colour = new float[]{ 0f, 0f, 0f, 0f };
            selectedHat.hsb    = new float[]{ 0f, 0f, 0f };
            updatePreviewHat();
            return true;
        }

        return false;
    }

    private void applySlider(int index, float t)
    {
        if (selectedHat == null) return;
        t = Mth.clamp(t, 0f, 1f);
        if (index < 4) {
            selectedHat.colour[index] = t;
        } else {
            selectedHat.hsb[index - 4] = t * 2f - 1f;
        }
        updatePreviewHat();
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (button == 0) {
            draggingSlider = -1;
        }

        if (hatGrid != null) hatGrid.mouseReleased(mouseX, mouseY, button);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (button == 0 && isInPreviewArea(mouseX)) {
            if (hasShiftDown()) {
                ClientEventHandler.guiPanX = Mth.clamp(
                    ClientEventHandler.guiPanX - (float)(dragX * 0.01), -2f, 2f);
                ClientEventHandler.guiPanY = Mth.clamp(
                    ClientEventHandler.guiPanY + (float)(dragY * 0.01), -2f, 2f);
            } else {
                ClientEventHandler.guiYaw  += (float)(dragX * 0.5);
                ClientEventHandler.guiPitch = Mth.clamp(
                    ClientEventHandler.guiPitch + (float)(dragY * 0.25), -85f, 85f);
            }
            return true;
        }

        if (draggingSlider >= 0 && colorPanelOpen) {
            int cx = innerX();
            int cw = innerW();
            int sliderW = cw - 10 - 2 - 2 - 28;
            int bx = cx + 10 + 2;
            applySlider(draggingSlider, (float)(mouseX - bx - 1) / (sliderW - 2));
            return true;
        }

        if (hatGrid != null && hatGrid.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private void openTradeGui()
    {
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }
}
