package me.guivnf.mods.hats.client.gui;

import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class HatLaunchSelectScreen extends Screen
{
    private static final int ROW_H    = 20;
    private static final int BTN_W    = 160;
    private static final int PAD      = 4;
    private static final int TITLE_H  = 14;
    private static final int BOTTOM_H = 24;
    private static final int MAX_VISIBLE_HAT_ROWS = 8;

    @Nullable
    public static String pendingHatName = null;

    private final List<HatPart> launchable = new ArrayList<>();

    private int scrollOffset = 0;

    private HatLaunchSelectScreen()
    {
        super(Component.literal("Select Hat to Launch"));
    }

    public static void open()
    {
        Minecraft.getInstance().setScreen(new HatLaunchSelectScreen());
    }

    private int visibleHatRows()
    {
        return Math.min(launchable.size(), MAX_VISIBLE_HAT_ROWS);
    }

    private int panelH()
    {
        return TITLE_H + PAD + ROW_H + visibleHatRows() * ROW_H + PAD + BOTTOM_H;
    }

    @Override
    protected void init()
    {
        launchable.clear();
        for (HatPart part : ClientHatCache.getLocalInventory()) {
            if (!part.isFavourite && part.count > 0) {
                launchable.add(part);
            }
        }

        scrollOffset = Math.max(0, Math.min(scrollOffset, launchable.size() - MAX_VISIBLE_HAT_ROWS));

        int panelW  = BTN_W + PAD * 2;
        int startX  = (width  - panelW) / 2;
        int startY  = (height - panelH()) / 2;

        int y = startY + TITLE_H + PAD;

        addRenderableWidget(Button.builder(randomLabel(), btn -> { pendingHatName = null; rebuildWidgets(); })
            .pos(startX + PAD, y)
            .size(BTN_W, ROW_H - 2)
            .build());
        y += ROW_H;

        int end = Math.min(scrollOffset + MAX_VISIBLE_HAT_ROWS, launchable.size());
        for (int i = scrollOffset; i < end; i++) {
            HatPart part = launchable.get(i);
            final String name = part.name;
            addRenderableWidget(Button.builder(hatLabel(part), btn -> { pendingHatName = name; rebuildWidgets(); })
                .pos(startX + PAD, y)
                .size(BTN_W, ROW_H - 2)
                .build());
            y += ROW_H;
        }

        int confirmY = startY + panelH() - BOTTOM_H + PAD;
        addRenderableWidget(Button.builder(Component.literal("Confirm"), btn -> onClose())
            .pos(startX + PAD, confirmY)
            .size((BTN_W - PAD) / 2, 16)
            .build());
        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> { pendingHatName = null; onClose(); })
            .pos(startX + PAD + (BTN_W - PAD) / 2 + PAD, confirmY)
            .size((BTN_W - PAD) / 2, 16)
            .build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        if (launchable.size() <= MAX_VISIBLE_HAT_ROWS) return false;
        int newOffset = scrollOffset - (int) Math.signum(delta);
        scrollOffset = Math.max(0, Math.min(newOffset, launchable.size() - MAX_VISIBLE_HAT_ROWS));
        rebuildWidgets();
        return true;
    }

    private Component randomLabel()
    {
        String prefix = pendingHatName == null ? "> " : "  ";
        return Component.literal(prefix + "Random");
    }

    private Component hatLabel(HatPart part)
    {
        HatDefinition def = HatRegistry.get(part.getRegistryKey());
        String displayName = def != null ? def.name : part.name;
        String prefix = part.name.equals(pendingHatName) ? "> " : "  ";
        String countSuffix = part.count > 1 ? " x" + part.count : "";
        return Component.literal(prefix + displayName + countSuffix);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial)
    {
        renderBackground(g, mouseX, mouseY, partial);

        int panelW  = BTN_W + PAD * 2;
        int startX  = (width  - panelW) / 2;
        int startY  = (height - panelH()) / 2;

        g.fill(startX, startY, startX + panelW, startY + panelH(), 0xCC000000);
        g.renderOutline(startX, startY, panelW, panelH(), 0xFF888888);
        g.drawCenteredString(font, title, width / 2, startY + PAD, 0xFFFFFF);

        if (launchable.size() > MAX_VISIBLE_HAT_ROWS) {
            int scrollBarX = startX + panelW - 3;
            int listTop    = startY + TITLE_H + PAD + ROW_H;
            int listH      = visibleHatRows() * ROW_H;
            float thumbRatio  = (float) MAX_VISIBLE_HAT_ROWS / launchable.size();
            float thumbOffset = (float) scrollOffset / launchable.size();
            int thumbH   = Math.max(6, (int)(listH * thumbRatio));
            int thumbY   = listTop + (int)((listH - thumbH) * thumbOffset / (1f - thumbRatio));
            g.fill(scrollBarX, listTop, scrollBarX + 2, listTop + listH, 0xFF444444);
            g.fill(scrollBarX, thumbY,  scrollBarX + 2, thumbY + thumbH, 0xFFAAAAAA);
        }

        super.render(g, mouseX, mouseY, partial);
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
}
