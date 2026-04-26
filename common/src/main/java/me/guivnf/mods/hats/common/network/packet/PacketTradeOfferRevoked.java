package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class PacketTradeOfferRevoked
{
    public static FriendlyByteBuf encode(UUID offerId)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUUID(offerId);
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID offerId = buf.readUUID();
        context.queue(() -> me.guivnf.mods.hats.client.trade.ClientTradeState.removeIncomingOffer(offerId));
    }
}
