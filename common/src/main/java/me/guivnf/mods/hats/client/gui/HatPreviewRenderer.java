package me.guivnf.mods.hats.client.gui;

import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import me.guivnf.mods.hats.client.render.LayerHat;
import me.guivnf.mods.hats.common.hat.HatPart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;

public final class HatPreviewRenderer
{
    private HatPreviewRenderer() {}

    public static void render(GuiGraphics g, Player player, HatPart hat,
                              int centerX, int footY, int scale,
                              int scissorX1, int scissorY1, int scissorX2, int scissorY2)
    {
        HatPart prev = ClientHatCache.getEntityHat(player.getUUID());

        HatPart show = hat.copy();
        show.isShowing = true;
        ClientHatCache.setEntityHat(player.getUUID(), show);

        Quaternionf cam = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf entity = new Quaternionf().rotateY((float) Math.toRadians(-25.0));

        float prevBodyYaw    = player.yBodyRot;
        float prevYRot       = player.getYRot();
        float prevXRot       = player.getXRot();
        float prevYHeadO     = player.yHeadRotO;
        float prevYHead      = player.yHeadRot;
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

        g.enableScissor(scissorX1, scissorY1, scissorX2, scissorY2);
        LayerHat.RENDERING_HAT_GUI_PREVIEW.set(true);
        EpicFightCompat.GUI_PREVIEW_ACTIVE = true;
        InventoryScreen.renderEntityInInventory(g, centerX, footY, scale, cam, entity, player);
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
}
