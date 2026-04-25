package me.guivnf.mods.hats.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementInfo;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementJsonLoader;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.*;
import java.util.stream.Collectors;

public class HatPlacementScreen extends Screen
{
    private static final int PANEL_W    = 180;
    private static final int GIZMO_SIZE = 48;
    private static final int FIELD_H    = 14;
    private static final int FIELD_PAD  = 3;
    private static final int BTN_H      = 16;

    private float camYaw     = 20f;
    private float camPitch   = 10f;
    private float camZoom    = 1.0f;
    private float camOffsetX = 0f;
    private float camOffsetY = 0f;

    private final List<ResourceLocation> entityIds;
    private int entityDropdownOpen  = 0;
    private int entityScrollOffset  = 0;
    private int selectedEntityIndex = 0;
    @Nullable private LivingEntity previewEntity;

    private final List<String> hatNames;
    private int hatDropdownOpen  = 0;
    private int hatScrollOffset  = 0;
    private int selectedHatIndex = -1;
    @Nullable private HatPart previewHat;

    private float offsetX = 0f;
    private float offsetY = 0f;
    private float offsetZ = 0f;
    private float rotX    = 0f;
    private float rotY    = 0f;
    private float rotZ    = 0f;
    private float scale   = 1f;

    private int    draggingField    = -1;
    private double dragStartX       = 0;
    private float  dragStartValue   = 0f;
    private int    potentialEditField = -1;

    private int    editingField = -1;
    private String editingText  = "";

    private boolean draggingViewport = false;
    private double  vpLastX = 0, vpLastY = 0;

    private String entitySearchText = "";
    private String hatSearchText    = "";

    private final Screen parent;
    private int savedGuiScale = -1;
    private static final int FORCED_SCALE = 2;

    public HatPlacementScreen(Screen parent)
    {
        super(Component.literal("Hat Placement Editor"));
        this.parent = parent;

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        entityIds = BuiltInRegistries.ENTITY_TYPE.entrySet().stream()
            .filter(e -> {
                if (e.getValue().getCategory() != MobCategory.MISC) return true;
                if (mc.level == null) return false;
                try { return e.getValue().create(mc.level) instanceof LivingEntity; }
                catch (Exception ignored) { return false; }
            })
            .map(e -> e.getKey().location())
            .sorted(Comparator.comparing(ResourceLocation::toString))
            .collect(Collectors.toList());

        hatNames = HatRegistry.getAll().stream()
            .filter(d -> !d.isAccessory())
            .map(d -> d.name)
            .sorted()
            .collect(Collectors.toList());
    }


    private int panelX()    { return width - PANEL_W; }
    private int viewportW() { return width - PANEL_W; }


    @Override
    protected void init()
    {
        double actualScale = minecraft.getWindow().getGuiScale();
        if (actualScale != FORCED_SCALE && savedGuiScale == -1) {
            savedGuiScale = (int) Math.round(actualScale);
            minecraft.getWindow().setGuiScale(FORCED_SCALE);
            this.width  = minecraft.getWindow().getGuiScaledWidth();
            this.height = minecraft.getWindow().getGuiScaledHeight();
        }
        spawnPreviewEntity();
        loadPlacementForSelected();
        buildButtons();
    }

    private void buildButtons()
    {
        int px = panelX() + 4;
        int pw = PANEL_W - 8;
        int py = height - BTN_H * 2 - FIELD_PAD * 3;

        int halfW = (pw - FIELD_PAD) / 2;
        addRenderableWidget(Button.builder(Component.literal("Export"), b -> doExport())
            .bounds(px, py, halfW, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal("Import"), b -> doImport())
            .bounds(px + halfW + FIELD_PAD, py, halfW, BTN_H).build());
        addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
            .bounds(px, py + BTN_H + FIELD_PAD, pw, BTN_H).build());
    }

    private void spawnPreviewEntity()
    {
        previewEntity = null;
        if (entityIds.isEmpty() || minecraft.level == null) return;
        ResourceLocation id = entityIds.get(selectedEntityIndex);
        EntityType<?> type  = BuiltInRegistries.ENTITY_TYPE.get(id);
        try {
            if (type.create(minecraft.level) instanceof LivingEntity living) {
                living.yBodyRot  = 180.0F;
                living.yBodyRotO = 180.0F;
                living.setYRot(  180.0F);
                living.yHeadRot  = 180.0F;
                living.yHeadRotO = 180.0F;
                living.setXRot(0.0F);
                previewEntity = living;
            }
        } catch (Exception ignored) {}
    }

    private void loadPlacementForSelected()
    {
        if (previewEntity == null) { offsetX = 0; offsetY = 0; offsetZ = 0; rotX = 0; rotY = 0; rotZ = 0; scale = 1; return; }
        HatPlacementInfo info = HatPlacementRegistry.get(previewEntity);
        offsetX = info.offsetX;
        offsetY = info.offsetY;
        offsetZ = info.offsetZ;
        rotX    = info.rotX;
        rotY    = info.rotY;
        rotZ    = info.rotZ;
        scale   = info.scale;
    }

    private void rebuildHat()
    {
        if (selectedHatIndex >= 0 && selectedHatIndex < hatNames.size()) {
            previewHat = new HatPart(hatNames.get(selectedHatIndex));
            previewHat.isShowing = true;
        } else {
            previewHat = null;
        }
    }


    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick)
    {
        renderBackground(g);
        g.fill(0, 0, viewportW(), height, 0xFF1A1A1A);

        renderViewport(g, partialTick);

        RenderSystem.disableDepthTest();

        renderPanel(g, mouseX, mouseY);
        super.render(g, mouseX, mouseY, partialTick);

        if (entityDropdownOpen == 1) renderEntityDropdown(g, mouseX, mouseY);
        if (hatDropdownOpen    == 1) renderHatDropdown(g, mouseX, mouseY);
    }


    private void renderViewport(GuiGraphics g, float partialTick)
    {
        if (previewEntity == null) {
            if (minecraft.level == null) {
                String msg = "Load a world to preview entities";
                g.drawString(font, msg,
                    (viewportW() - font.width(msg)) / 2,
                    height / 2 - 4, 0xFF888888, false);
            }
            renderGizmo(g);
            return;
        }

        float bbH    = previewEntity.getBbHeight();
        float bbW    = previewEntity.getBbWidth();
        float maxDim = Math.max(bbH, bbW);

        int renderScale = Math.max(8, (int)(height * 0.4f / maxDim / camZoom));

        int screenX = viewportW() / 2;
        int screenY = height / 2 + (int)(bbH * renderScale * 0.4f);


        int scale = (int) minecraft.getWindow().getGuiScale();
        int fbH   = minecraft.getWindow().getHeight();
        RenderSystem.enableScissor(0, 0, viewportW() * scale, fbH);

        PoseStack mvs = RenderSystem.getModelViewStack();
        mvs.pushPose();
        mvs.translate(screenX + camOffsetX, screenY + camOffsetY, 1050.0);
        mvs.scale(1.0F, 1.0F, -1.0F);
        RenderSystem.applyModelViewMatrix();

        PoseStack pose = new PoseStack();
        pose.translate(0.0, 0.0, 1000.0);
        pose.scale(renderScale, renderScale, renderScale);

        Quaternionf pitchQuat = new Quaternionf().rotateX(camPitch * Mth.DEG_TO_RAD);
        Quaternionf flipQuat  = new Quaternionf().rotateZ((float)Math.PI);
        flipQuat.mul(pitchQuat);
        flipQuat.mul(new Quaternionf().rotateY(camYaw * Mth.DEG_TO_RAD));
        pose.mulPose(flipQuat);

        drawGrid3D(pose);

        float savedBodyRot  = previewEntity.yBodyRot;
        float savedBodyRotO = previewEntity.yBodyRotO;
        float savedYaw      = previewEntity.getYRot();
        float savedPitch    = previewEntity.getXRot();
        float savedHeadYaw  = previewEntity.yHeadRot;
        float savedHeadYawO = previewEntity.yHeadRotO;

        previewEntity.yBodyRot  = 180.0F;
        previewEntity.yBodyRotO = 180.0F;
        previewEntity.setYRot(  180.0F);
        previewEntity.yHeadRot  = 180.0F;
        previewEntity.yHeadRotO = 180.0F;
        previewEntity.setXRot(0.0F);

        HatPart saved = ClientHatCache.getEntityHat(previewEntity.getUUID());
        ClientHatCache.setEntityHat(previewEntity.getUUID(),
            previewHat != null ? previewHat : null);

        EntityRenderDispatcher dispatcher = minecraft.getEntityRenderDispatcher();
        dispatcher.overrideCameraOrientation(new Quaternionf(pitchQuat).conjugate());
        dispatcher.setRenderShadow(false);

        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() ->
            dispatcher.render(previewEntity, 0.0, 0.0, 0.0, 0.0F, partialTick,
                pose, buffers, LightTexture.FULL_BRIGHT));
        buffers.endBatch();

        dispatcher.setRenderShadow(true);

        ClientHatCache.setEntityHat(previewEntity.getUUID(), saved);
        previewEntity.yBodyRot  = savedBodyRot;
        previewEntity.yBodyRotO = savedBodyRotO;
        previewEntity.setYRot(savedYaw);
        previewEntity.setXRot(savedPitch);
        previewEntity.yHeadRot  = savedHeadYaw;
        previewEntity.yHeadRotO = savedHeadYawO;

        if (previewHat == null) drawWireframe3D(pose, bbH);

        mvs.popPose();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.disableScissor();

        renderGizmo(g);
    }

    private void drawGrid3D(PoseStack pose)
    {
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        org.joml.Matrix4f mat = pose.last().pose();
        Tesselator tes = Tesselator.getInstance();
        BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        int   lines   = 10;
        float spacing = 0.5f;
        float extent  = lines * spacing;

        for (int i = -lines; i <= lines; i++) {
            float p      = i * spacing;
            float bright = i == 0 ? 0.55f : 0.28f;
            float alpha  = i == 0 ? 1.0f  : 0.7f;

            buf.vertex(mat, -extent, 0f, p).color(bright, bright, bright, alpha).endVertex();
            buf.vertex(mat,  extent, 0f, p).color(bright, bright, bright, alpha).endVertex();

            buf.vertex(mat, p, 0f, -extent).color(bright, bright, bright, alpha).endVertex();
            buf.vertex(mat, p, 0f,  extent).color(bright, bright, bright, alpha).endVertex();
        }

        tes.end();
        RenderSystem.enableDepthTest();
    }

    private void drawWireframe3D(PoseStack pose, float bbHeight)
    {
        RenderSystem.enableDepthTest();
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionColorShader);

        float hs = scale * 0.25f;
        float r = 1f, gr = 0.2f, bl = 0.2f, a = 1f;

        pose.pushPose();
        pose.translate(offsetX / 16f, bbHeight + offsetY / 16f, offsetZ / 16f);
        if (rotZ != 0f) pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotZ));
        if (rotY != 0f) pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(rotY));
        if (rotX != 0f) pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(rotX));

        org.joml.Matrix4f mat = pose.last().pose();

        Tesselator tes = Tesselator.getInstance();
        BufferBuilder buf = tes.getBuilder();
        buf.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        buf.vertex(mat,-hs,-hs,-hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat, hs,-hs,-hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat, hs,-hs,-hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat, hs,-hs, hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat, hs,-hs, hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat,-hs,-hs, hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat,-hs,-hs, hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat,-hs,-hs,-hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat,-hs, hs,-hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat, hs, hs,-hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat, hs, hs,-hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat, hs, hs, hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat, hs, hs, hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat,-hs, hs, hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat,-hs, hs, hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat,-hs, hs,-hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat,-hs,-hs,-hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat,-hs, hs,-hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat, hs,-hs,-hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat, hs, hs,-hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat, hs,-hs, hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat, hs, hs, hs).color(r,gr,bl,a).endVertex();
        buf.vertex(mat,-hs,-hs, hs).color(r,gr,bl,a).endVertex(); buf.vertex(mat,-hs, hs, hs).color(r,gr,bl,a).endVertex();

        tes.end();
        pose.popPose();
    }

    private void renderGizmo(GuiGraphics g)
    {
        int gx = panelX() + 4;
        int gy = 4;
        int sz = GIZMO_SIZE;
        int cx = gx + sz / 2;
        int cy = gy + sz / 2;

        g.fill(gx, gy, gx + sz, gy + sz, 0x88000000);

        double yr = Math.toRadians(camYaw);
        double pr = Math.toRadians(camPitch);
        int len = sz / 2 - 5;

        int xex = cx + (int)( Math.cos(yr) * len);
        int xey = cy + (int)( Math.sin(pr) * (len * 0.5));
        drawLine2D(g, cx, cy, xex, xey, 0xFFFF4444);
        g.drawString(font, "X", xex + 2, xey - 3, 0xFFFF4444, false);

        drawLine2D(g, cx, cy, cx, cy - len, 0xFF44FF44);
        g.drawString(font, "Y", cx + 2, cy - len - 3, 0xFF44FF44, false);

        int zex = cx - (int)( Math.sin(yr) * len);
        int zey = cy + (int)( Math.sin(pr) * (len * 0.5));
        drawLine2D(g, cx, cy, zex, zey, 0xFF4444FF);
        g.drawString(font, "Z", zex + 2, zey - 3, 0xFF4444FF, false);
    }

    private void drawLine2D(GuiGraphics g, int x1, int y1, int x2, int y2, int color)
    {
        int dx = x2 - x1, dy = y2 - y1;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0) { g.fill(x1, y1, x1 + 1, y1 + 1, color); return; }
        for (int i = 0; i <= steps; i++)
            g.fill(x1 + dx*i/steps, y1 + dy*i/steps,
                   x1 + dx*i/steps + 1, y1 + dy*i/steps + 1, color);
    }


    private void renderPanel(GuiGraphics g, int mouseX, int mouseY)
    {
        int px = panelX();
        g.fill(px, 0, px + PANEL_W, height, 0xFF2A2A2A);
        g.fill(px, 0, px + 1, height, 0xFF555555);

        int cx   = px + 4;
        int cw   = PANEL_W - 8;
        int curY = GIZMO_SIZE + 8;

        g.drawString(font, "Entity:", cx, curY, 0xCCCCCC, false);
        curY += 10;
        String eLabel = entityIds.isEmpty() ? "(none)" : entityIds.get(selectedEntityIndex).toString();
        drawDropBtn(g, cx, curY, cw, eLabel, entityDropdownOpen == 1, mouseX, mouseY);
        curY += FIELD_H + FIELD_PAD + 6;

        g.drawString(font, "Offset / Scale:", cx, curY, 0xCCCCCC, false);
        curY += 10;
        float[] vals  = { offsetX, offsetY, offsetZ, scale };
        String[] lbls = { "X", "Y", "Z", "Scale" };
        for (int i = 0; i < 4; i++) {
            drawFieldRow(g, cx, curY, cw, lbls[i], vals[i], i, mouseX, mouseY);
            curY += FIELD_H + FIELD_PAD;
        }
        curY += 4;

        g.drawString(font, "Rotation (deg):", cx, curY, 0xCCCCCC, false);
        curY += 10;
        float[] rvals  = { rotX, rotY, rotZ };
        String[] rlbls = { "rX", "rY", "rZ" };
        for (int i = 0; i < 3; i++) {
            drawFieldRow(g, cx, curY, cw, rlbls[i], rvals[i], 4 + i, mouseX, mouseY);
            curY += FIELD_H + FIELD_PAD;
        }
        curY += 4;

        g.drawString(font, "Preview Hat:", cx, curY, 0xCCCCCC, false);
        curY += 10;
        String hLabel = selectedHatIndex < 0 ? "(wireframe)" : hatNames.get(selectedHatIndex);
        drawDropBtn(g, cx, curY, cw, hLabel, hatDropdownOpen == 1, mouseX, mouseY);
    }

    private void drawDropBtn(GuiGraphics g, int x, int y, int w,
                              String label, boolean open, int mx, int my)
    {
        boolean hover = mx >= x && mx < x + w && my >= y && my < y + FIELD_H;
        g.fill(x, y, x + w, y + FIELD_H, 0xFF888888);
        g.fill(x+1, y+1, x+w-1, y+FIELD_H-1, hover ? 0xFF555555 : 0xFF3A3A3A);
        String t = label.length() > 22 ? "..." + label.substring(label.length()-19) : label;
        g.drawString(font, t, x + 3, y + 3, 0xFFFFFF, false);
        g.drawString(font, open ? "^" : "v", x + w - 10, y + 3, 0xAAAAAA, false);
    }

    private void drawFieldRow(GuiGraphics g, int x, int y, int w,
                               String label, float val, int idx, int mx, int my)
    {
        int lw = 36, fw = w - lw - FIELD_PAD, fx = x + lw;
        g.drawString(font, label + ":", x, y + 3, 0xAAAAAA, false);
        boolean hover   = mx >= fx && mx < fx + fw && my >= y && my < y + FIELD_H;
        boolean drag    = draggingField == idx;
        boolean editing = editingField == idx;
        g.fill(fx, y, fx + fw, y + FIELD_H, 0xFF666666);
        int bg = editing ? 0xFF1A3A5A : drag ? 0xFF223355 : hover ? 0xFF444444 : 0xFF333333;
        g.fill(fx+1, y+1, fx+fw-1, y+FIELD_H-1, bg);
        if (editing) {
            String display = editingText + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : " ");
            g.drawString(font, display, fx + 3, y + 3, 0xFFFFAA, false);
        } else {
            g.drawString(font, String.format("%.3f", val), fx + 3, y + 3, 0xFFFFFF, false);
            if (drag || hover) g.drawString(font, "\u2194", fx + fw - 12, y + 2, 0x88FFFFFF, false);
        }
    }


    private int entityDropdownTopY()
    {
        return GIZMO_SIZE + 8 + 10 + FIELD_H;
    }

    private int hatDropdownTopY()
    {
        return GIZMO_SIZE + 8 + 10 + FIELD_H + FIELD_PAD + 6
             + 10 + 4 * (FIELD_H + FIELD_PAD) + 4
             + 10 + 3 * (FIELD_H + FIELD_PAD) + 4
             + 10 + FIELD_H;
    }

    private static final int DROPDOWN_ROWS = 8;


    private List<String> filteredEntityItems()
    {
        List<String> all = entityIds.stream().map(ResourceLocation::toString).collect(Collectors.toList());
        if (entitySearchText.isEmpty()) return all;
        String q = entitySearchText.toLowerCase(Locale.ROOT);
        return all.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(q)).collect(Collectors.toList());
    }

    private List<String> filteredHatItems()
    {
        List<String> all = new ArrayList<>();
        all.add("(wireframe)");
        all.addAll(hatNames);
        if (hatSearchText.isEmpty()) return all;
        String q = hatSearchText.toLowerCase(Locale.ROOT);
        return all.stream().filter(s -> s.toLowerCase(Locale.ROOT).contains(q)).collect(Collectors.toList());
    }


    private void renderEntityDropdown(GuiGraphics g, int mx, int my)
    {
        int x = panelX() + 4, y = entityDropdownTopY(), w = PANEL_W - 8;
        List<String> items = filteredEntityItems();
        renderDropdown(g, mx, my, x, y, w, items, entityScrollOffset, entitySearchText,
            entityIds.isEmpty() ? -1 : entityIds.indexOf(entityIds.get(selectedEntityIndex)));
    }

    private void renderHatDropdown(GuiGraphics g, int mx, int my)
    {
        int x = panelX() + 4, y = hatDropdownTopY(), w = PANEL_W - 8;
        List<String> items = filteredHatItems();
        String selectedLabel = selectedHatIndex < 0 ? "(wireframe)" : hatNames.get(selectedHatIndex);
        int selInFiltered = items.indexOf(selectedLabel);
        renderDropdown(g, mx, my, x, y, w, items, hatScrollOffset, hatSearchText, selInFiltered);
    }

    private void renderDropdown(GuiGraphics g, int mx, int my,
                                 int x, int y, int w,
                                 List<String> items, int scroll, String searchText, int selected)
    {
        int rows   = Math.min(DROPDOWN_ROWS, items.size());
        int totalH = FIELD_H + rows * FIELD_H;

        g.fill(x-1, y-1, x+w+1, y + totalH + 1, 0xFF888888);
        g.fill(x,   y,   x+w,   y + totalH,     0xFF2A2A2A);

        g.fill(x, y, x+w, y + FIELD_H, 0xFF444444);
        g.fill(x+1, y+1, x+w-1, y+FIELD_H-1, 0xFF222222);
        String prompt = searchText.isEmpty() ? "\u00a77Search..." : searchText + (System.currentTimeMillis() / 500 % 2 == 0 ? "|" : "");
        g.drawString(font, prompt, x + 3, y + 3, searchText.isEmpty() ? 0x888888 : 0xFFFFFF, false);

        int itemsY = y + FIELD_H;
        for (int i = 0; i < rows; i++) {
            int idx = i + scroll;
            if (idx >= items.size()) break;
            int iy = itemsY + i * FIELD_H;
            boolean hover = mx >= x && mx < x+w && my >= iy && my < iy+FIELD_H;
            boolean sel   = idx == selected;
            if (sel)        g.fill(x, iy, x+w, iy+FIELD_H, 0xFF335588);
            else if (hover) g.fill(x, iy, x+w, iy+FIELD_H, 0xFF3A3A3A);
            String lbl = items.get(idx);
            if (lbl.length() > 24) lbl = lbl.substring(0, 21) + "...";
            g.drawString(font, lbl, x+3, iy+3, sel ? 0xFFFFAA : 0xCCCCCC, false);
        }
    }


    @Override
    public boolean mouseClicked(double mx, double my, int button)
    {
        if (editingField >= 0) {
            commitEdit();
        }

        if ((entityDropdownOpen == 1 || hatDropdownOpen == 1) && mx < panelX()) {
            closeDropdowns();
            return true;
        }

        if (entityDropdownOpen == 1) {
            int x = panelX()+4, w = PANEL_W-8;
            int topY = entityDropdownTopY();
            if (hitBox(mx, my, x, topY, w, FIELD_H)) return true;
            List<String> items = filteredEntityItems();
            int r = hitDropdownItems(mx, my, x, topY + FIELD_H, w, items.size(), entityScrollOffset);
            if (r >= 0) {
                String chosen = items.get(r);
                selectedEntityIndex = entityIds.indexOf(entityIds.stream()
                    .filter(id -> id.toString().equals(chosen)).findFirst().orElse(entityIds.get(0)));
                closeDropdowns();
                spawnPreviewEntity();
                loadPlacementForSelected();
            } else {
                closeDropdowns();
            }
            return true;
        }

        if (hatDropdownOpen == 1) {
            int x = panelX()+4, w = PANEL_W-8;
            int topY = hatDropdownTopY();
            if (hitBox(mx, my, x, topY, w, FIELD_H)) return true;
            List<String> items = filteredHatItems();
            int r = hitDropdownItems(mx, my, x, topY + FIELD_H, w, items.size(), hatScrollOffset);
            if (r >= 0) {
                String chosen = items.get(r);
                if (chosen.equals("(wireframe)")) {
                    selectedHatIndex = -1;
                } else {
                    selectedHatIndex = hatNames.indexOf(chosen);
                }
                closeDropdowns();
                rebuildHat();
            } else {
                closeDropdowns();
            }
            return true;
        }

        if (hitBox(mx, my, panelX()+4, entityDropdownTopY()-FIELD_H, PANEL_W-8, FIELD_H)) {
            entityDropdownOpen = entityDropdownOpen == 0 ? 1 : 0;
            hatDropdownOpen    = 0;
            entitySearchText   = "";
            return true;
        }

        if (hitBox(mx, my, panelX()+4, hatDropdownTopY()-FIELD_H, PANEL_W-8, FIELD_H)) {
            hatDropdownOpen    = hatDropdownOpen == 0 ? 1 : 0;
            entityDropdownOpen = 0;
            hatSearchText      = "";
            return true;
        }

        int fi = hitField(mx, my);
        if (fi >= 0) {
            potentialEditField = fi;
            draggingField      = -1;
            dragStartX         = mx;
            dragStartValue     = getField(fi);
            return true;
        }

        if (mx < viewportW()) {
            draggingViewport = true;
            vpLastX = mx; vpLastY = my;
            return true;
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button)
    {
        if (potentialEditField >= 0) {
            editingField       = potentialEditField;
            editingText        = String.format("%.3f", getField(editingField));
            potentialEditField = -1;
            draggingField      = -1;
            return true;
        }
        draggingField      = -1;
        draggingViewport   = false;
        potentialEditField = -1;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy)
    {
        if (potentialEditField >= 0 && Math.abs(mx - dragStartX) > 3) {
            draggingField      = potentialEditField;
            potentialEditField = -1;
        }

        if (draggingField >= 0) {
            float sens  = draggingField == 3 ? 0.004f : 0.008f;
            float delta = (float)(mx - dragStartX) * sens;
            setField(draggingField, dragStartValue + delta);
            applyToRegistry();
            return true;
        }

        if (draggingViewport) {
            float ddx = (float)(mx - vpLastX);
            float ddy = (float)(my - vpLastY);
            if (hasShiftDown()) {
                camOffsetX += ddx;
                camOffsetY += ddy;
            } else {
                camYaw   += ddx * 0.5f;
                camPitch  = Mth.clamp(camPitch + ddy * 0.3f, -80f, 80f);
            }
            vpLastX = mx; vpLastY = my;
            return true;
        }

        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta)
    {
        if (entityDropdownOpen == 1) {
            int max = Math.max(0, filteredEntityItems().size() - DROPDOWN_ROWS);
            entityScrollOffset = Mth.clamp((int)(entityScrollOffset - delta), 0, max);
            return true;
        }
        if (hatDropdownOpen == 1) {
            int max = Math.max(0, filteredHatItems().size() - DROPDOWN_ROWS);
            hatScrollOffset = Mth.clamp((int)(hatScrollOffset - delta), 0, max);
            return true;
        }
        if (mx < viewportW()) {
            camZoom = Mth.clamp(camZoom - (float)(delta * 0.15f), 0.2f, 5f);
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean charTyped(char c, int modifiers)
    {
        if (editingField >= 0) {
            if (c == '-' && editingText.isEmpty()) { editingText = "-"; return true; }
            if ((c >= '0' && c <= '9') || c == '.') {
                if (c == '.' && editingText.contains(".")) return true;
                editingText += c;
            }
            return true;
        }

        if (entityDropdownOpen == 1) {
            entitySearchText += c;
            entityScrollOffset = 0;
            return true;
        }
        if (hatDropdownOpen == 1) {
            hatSearchText += c;
            hatScrollOffset = 0;
            return true;
        }

        return super.charTyped(c, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers)
    {
        final int KEY_BACKSPACE = 259, KEY_ENTER = 257, KEY_KP_ENTER = 335,
                  KEY_ESCAPE = 256, KEY_TAB = 258;

        if (editingField >= 0) {
            if (keyCode == KEY_BACKSPACE) {
                if (!editingText.isEmpty()) editingText = editingText.substring(0, editingText.length() - 1);
                return true;
            }
            if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER) { commitEdit(); return true; }
            if (keyCode == KEY_ESCAPE) { editingField = -1; editingText = ""; return true; }
            if (keyCode == KEY_TAB) {
                commitEdit();
                editingField = (editingField + 1) % 7;
                editingText  = String.format("%.3f", getField(editingField));
                return true;
            }
            return true;
        }

        if (entityDropdownOpen == 1) {
            if (keyCode == KEY_BACKSPACE && !entitySearchText.isEmpty()) {
                entitySearchText = entitySearchText.substring(0, entitySearchText.length() - 1);
                entityScrollOffset = 0;
                return true;
            }
            if (keyCode == KEY_ESCAPE) { closeDropdowns(); return true; }
            if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER) {
                List<String> items = filteredEntityItems();
                if (!items.isEmpty()) {
                    String chosen = items.get(Math.min(entityScrollOffset, items.size()-1));
                    selectedEntityIndex = entityIds.stream()
                        .map(ResourceLocation::toString).collect(Collectors.toList()).indexOf(chosen);
                    closeDropdowns();
                    spawnPreviewEntity();
                    loadPlacementForSelected();
                }
                return true;
            }
            return true;
        }

        if (hatDropdownOpen == 1) {
            if (keyCode == KEY_BACKSPACE && !hatSearchText.isEmpty()) {
                hatSearchText = hatSearchText.substring(0, hatSearchText.length() - 1);
                hatScrollOffset = 0;
                return true;
            }
            if (keyCode == KEY_ESCAPE) { closeDropdowns(); return true; }
            if (keyCode == KEY_ENTER || keyCode == KEY_KP_ENTER) {
                List<String> items = filteredHatItems();
                if (!items.isEmpty()) {
                    String chosen = items.get(Math.min(hatScrollOffset, items.size()-1));
                    if (chosen.equals("(wireframe)")) selectedHatIndex = -1;
                    else selectedHatIndex = hatNames.indexOf(chosen);
                    closeDropdowns();
                    rebuildHat();
                }
                return true;
            }
            return true;
        }

        if (keyCode == KEY_ESCAPE) { onClose(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    private void closeDropdowns()
    {
        entityDropdownOpen = 0;
        hatDropdownOpen    = 0;
        entitySearchText   = "";
        hatSearchText      = "";
        draggingViewport   = false;
        draggingField      = -1;
        potentialEditField = -1;
    }

    private void commitEdit()
    {
        if (editingField < 0) return;
        try { setField(editingField, Float.parseFloat(editingText)); applyToRegistry(); }
        catch (NumberFormatException ignored) {}
        editingField = -1;
        editingText  = "";
    }

    private boolean hitBox(double mx, double my, int x, int y, int w, int h)
    {
        return mx >= x && mx < x+w && my >= y && my < y+h;
    }

    private int hitDropdownItems(double mx, double my, int x, int y, int w, int total, int scroll)
    {
        int rows = Math.min(DROPDOWN_ROWS, total);
        for (int i = 0; i < rows; i++) {
            int iy = y + i * FIELD_H;
            if (hitBox(mx, my, x, iy, w, FIELD_H)) return i + scroll;
        }
        return -1;
    }

    private int hitField(double mx, double my)
    {
        int fx    = panelX() + 4 + 36;
        int fw    = PANEL_W - 8 - 36 - FIELD_PAD;
        int baseY = GIZMO_SIZE + 8 + 10 + FIELD_H + FIELD_PAD + 6 + 10;
        for (int i = 0; i < 4; i++) {
            int iy = baseY + i * (FIELD_H + FIELD_PAD);
            if (hitBox(mx, my, fx, iy, fw, FIELD_H)) return i;
        }
        int rotBaseY = baseY + 4 * (FIELD_H + FIELD_PAD) + 4 + 10;
        for (int i = 0; i < 3; i++) {
            int iy = rotBaseY + i * (FIELD_H + FIELD_PAD);
            if (hitBox(mx, my, fx, iy, fw, FIELD_H)) return 4 + i;
        }
        return -1;
    }

    private float getField(int i)
    {
        return switch (i) {
            case 0 -> offsetX; case 1 -> offsetY; case 2 -> offsetZ;
            case 3 -> scale;
            case 4 -> rotX; case 5 -> rotY; case 6 -> rotZ;
            default -> 0f;
        };
    }

    private void setField(int i, float v)
    {
        switch (i) {
            case 0 -> offsetX = Mth.clamp(v, -64f, 64f);
            case 1 -> offsetY = Mth.clamp(v, -64f, 64f);
            case 2 -> offsetZ = Mth.clamp(v, -64f, 64f);
            case 3 -> scale   = Math.max(0.01f, v);
            case 4 -> rotX    = v;
            case 5 -> rotY    = v;
            case 6 -> rotZ    = v;
        }
    }

    private void applyToRegistry()
    {
        if (previewEntity == null) return;
        HatPlacementRegistry.registerOverride(previewEntity.getType(),
            new HatPlacementInfo(offsetX, offsetY, offsetZ, rotX, rotY, rotZ, scale, false));
    }


    private void doExport()
    {
        applyToRegistry();
        HatPlacementJsonLoader.exportAll();
    }

    private void doImport()
    {
        HatPlacementJsonLoader.load();
        loadPlacementForSelected();
    }

    @Override
    public void onClose()
    {
        if (savedGuiScale != -1) {
            minecraft.getWindow().setGuiScale(savedGuiScale);
            savedGuiScale = -1;
        }
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
