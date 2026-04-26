package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.trade.TradeOfferStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PacketRequestNearbyPlayers
{
    public static FriendlyByteBuf encode()
    {
        return new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        context.queue(() -> {
            ServerPlayer sender = (ServerPlayer) context.getPlayer();
            List<ServerPlayer> nearby = new ArrayList<>();

            double range = TradeOfferStore.TRADE_RANGE_BLOCKS;
            double rSq = range * range;
            for (ServerPlayer other : sender.getServer().getPlayerList().getPlayers()) {
                if (other == sender) continue;
                if (other.level() != sender.level()) continue;
                if (other.distanceToSqr(sender) > rSq) continue;
                nearby.add(other);
            }

            NetworkManager.sendToPlayer(sender, HatsNetwork.NEARBY_PLAYERS_LIST,
                PacketNearbyPlayersList.encode(nearby));
        });
    }
}
