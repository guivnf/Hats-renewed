package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class PacketTradePartnerLeft
{
    public static FriendlyByteBuf encode(UUID targetUuid)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUUID(targetUuid);
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID targetUuid = buf.readUUID();
        context.queue(() -> me.guivnf.mods.hats.client.trade.ClientTradeFlow.onPartnerLeft(targetUuid));
    }
}
