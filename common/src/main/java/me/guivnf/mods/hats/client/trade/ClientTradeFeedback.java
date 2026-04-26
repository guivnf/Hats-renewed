package me.guivnf.mods.hats.client.trade;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ClientTradeFeedback
{
    public static void onResolved(boolean accepted, String otherName)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Component msg = accepted
            ? Component.literal("Trade with " + otherName + " accepted.").withStyle(ChatFormatting.GREEN)
            : Component.literal("Trade with " + otherName + " declined.").withStyle(ChatFormatting.RED);
        mc.player.displayClientMessage(msg, true);
    }
}
