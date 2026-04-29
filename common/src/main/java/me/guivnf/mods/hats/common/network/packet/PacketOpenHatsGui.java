package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

public class PacketOpenHatsGui
{
    public static RegistryFriendlyByteBuf encode()
    {
        return new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        context.queue(PacketOpenHatsGui::handleClient);
    }


    private static void handleClient()
    {
        me.guivnf.mods.hats.client.gui.HatsScreen.open();
    }
}
