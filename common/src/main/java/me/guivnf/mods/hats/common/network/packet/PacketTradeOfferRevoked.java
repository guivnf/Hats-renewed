package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

import java.util.UUID;

public class PacketTradeOfferRevoked
{
    public static RegistryFriendlyByteBuf encode(UUID offerId)
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeUUID(offerId);
        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID offerId = buf.readUUID();
        context.queue(() -> me.guivnf.mods.hats.client.trade.ClientTradeState.removeIncomingOffer(offerId));
    }
}
