package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.trade.TradeOfferStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketCloseTradeBuilder
{
    public static FriendlyByteBuf encode()
    {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        context.queue(() -> {
            ServerPlayer sender = (ServerPlayer) context.getPlayer();
            TradeOfferStore.get().closeSession(sender.getUUID());
        });
    }
}
