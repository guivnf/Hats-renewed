package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;


import net.minecraft.network.FriendlyByteBuf;

public class PacketOpenHatsGui
{
    public static FriendlyByteBuf encode()
    {
        return new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        context.queue(PacketOpenHatsGui::handleClient);
    }


    private static void handleClient()
    {
        me.guivnf.mods.hats.client.gui.HatsScreen.open();
    }
}
