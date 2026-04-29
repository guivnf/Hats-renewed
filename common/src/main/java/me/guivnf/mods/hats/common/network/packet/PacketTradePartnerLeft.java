package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

import java.util.UUID;

public class PacketTradePartnerLeft
{
    public static RegistryFriendlyByteBuf encode(UUID targetUuid)
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeUUID(targetUuid);
        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID targetUuid = buf.readUUID();
        context.queue(() -> me.guivnf.mods.hats.client.trade.ClientTradeFlow.onPartnerLeft(targetUuid));
    }
}
