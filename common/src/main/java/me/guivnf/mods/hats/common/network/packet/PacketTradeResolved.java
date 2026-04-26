package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public class PacketTradeResolved
{
    public static FriendlyByteBuf encode(UUID offerId, boolean accepted, String otherName)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUUID(offerId);
        buf.writeBoolean(accepted);
        buf.writeUtf(otherName);
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID offerId = buf.readUUID();
        boolean accepted = buf.readBoolean();
        String otherName = buf.readUtf();

        context.queue(() -> {
            me.guivnf.mods.hats.client.trade.ClientTradeState.removeIncomingOffer(offerId);
            me.guivnf.mods.hats.client.trade.ClientTradeFeedback.onResolved(accepted, otherName);
        });
    }
}
