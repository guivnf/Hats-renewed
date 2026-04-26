package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.trade.TradeBuilderSession;
import me.guivnf.mods.hats.common.trade.TradeErrorType;
import me.guivnf.mods.hats.common.trade.TradeOfferStore;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import me.guivnf.mods.hats.common.world.PlayerHatData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketRequestPeerInventory
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

        context.queue(() -> {
            ServerPlayer sender = (ServerPlayer) context.getPlayer();
            ServerPlayer target = sender.getServer().getPlayerList().getPlayer(targetUuid);

            if (target == null || target.level() != sender.level()) {
                NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.RECEIVER_OFFLINE));
                return;
            }

            double range = TradeOfferStore.TRADE_RANGE_BLOCKS;
            if (target.distanceToSqr(sender) > range * range) {
                NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.OUT_OF_RANGE));
                return;
            }

            TradeOfferStore.get().openSession(new TradeBuilderSession(
                sender.getUUID(), targetUuid, System.currentTimeMillis()));

            HatsSavedData data = HatsSavedData.get(target.serverLevel());
            PlayerHatData targetData = data.getPlayer(targetUuid);

            NetworkManager.sendToPlayer(sender, HatsNetwork.PEER_INVENTORY,
                PacketPeerInventory.encode(targetUuid,
                    targetData != null ? targetData.equippedHat : null,
                    targetData != null ? targetData.inventory : java.util.List.of()));
        });
    }
}
