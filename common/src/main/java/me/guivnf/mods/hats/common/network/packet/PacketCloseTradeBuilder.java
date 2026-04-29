package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.trade.TradeOfferStore;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;

public class PacketCloseTradeBuilder
{
    public static RegistryFriendlyByteBuf encode()
    {
        return new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        context.queue(() -> {
            ServerPlayer sender = (ServerPlayer) context.getPlayer();
            TradeOfferStore.get().closeSession(sender.getUUID());
        });
    }
}
