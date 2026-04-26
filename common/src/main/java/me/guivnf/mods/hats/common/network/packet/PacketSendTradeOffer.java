package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.trade.TradeErrorType;
import me.guivnf.mods.hats.common.trade.TradeOffer;
import me.guivnf.mods.hats.common.trade.TradeOfferStore;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import me.guivnf.mods.hats.common.world.PlayerHatData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketSendTradeOffer
{
    public static FriendlyByteBuf encode(UUID receiverUuid, List<HatPart> senderHats, List<HatPart> receiverHats)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUUID(receiverUuid);
        buf.writeInt(senderHats.size());
        for (HatPart h : senderHats) buf.writeNbt(h.save());
        buf.writeInt(receiverHats.size());
        for (HatPart h : receiverHats) buf.writeNbt(h.save());
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID receiverUuid = buf.readUUID();
        int sn = buf.readInt();
        List<HatPart> senderHats = new ArrayList<>(sn);
        for (int i = 0; i < sn; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) senderHats.add(HatPart.load(tag));
        }
        int rn = buf.readInt();
        List<HatPart> receiverHats = new ArrayList<>(rn);
        for (int i = 0; i < rn; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) receiverHats.add(HatPart.load(tag));
        }

        context.queue(() -> {
            ServerPlayer sender = (ServerPlayer) context.getPlayer();
            ServerPlayer receiver = sender.getServer().getPlayerList().getPlayer(receiverUuid);

            if (receiver == null || receiver.level() != sender.level()) {
                NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.RECEIVER_OFFLINE));
                return;
            }
            if (senderHats.isEmpty() && receiverHats.isEmpty()) {
                NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.EMPTY_OFFER));
                return;
            }

            double range = TradeOfferStore.TRADE_RANGE_BLOCKS;
            if (receiver.distanceToSqr(sender) > range * range) {
                NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.OUT_OF_RANGE));
                return;
            }

            HatsSavedData data = HatsSavedData.get(sender.serverLevel());
            PlayerHatData senderData   = data.getOrCreatePlayer(sender.getUUID());
            PlayerHatData receiverData = data.getOrCreatePlayer(receiverUuid);

            for (HatPart h : senderHats) {
                if (!senderData.hasHat(h.name)) {
                    NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                        PacketTradeError.encode(TradeErrorType.SENDER_MISSING_HAT, h.name));
                    return;
                }
                if (senderData.equippedHat != null && senderData.equippedHat.name.equals(h.name)) {
                    NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                        PacketTradeError.encode(TradeErrorType.SENDER_HAT_EQUIPPED, h.name));
                    return;
                }
                pruneToOwnedAccessories(h, findStored(senderData, h.name));
            }
            for (HatPart h : receiverHats) {
                if (!receiverData.hasHat(h.name)) {
                    NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                        PacketTradeError.encode(TradeErrorType.RECEIVER_MISSING_HAT, h.name));
                    return;
                }
                if (receiverData.equippedHat != null && receiverData.equippedHat.name.equals(h.name)) {
                    NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                        PacketTradeError.encode(TradeErrorType.RECEIVER_HAT_EQUIPPED, h.name));
                    return;
                }
                pruneToOwnedAccessories(h, findStored(receiverData, h.name));
            }

            UUID offerId = UUID.randomUUID();
            TradeOffer offer = new TradeOffer(offerId,
                sender.getUUID(), sender.getGameProfile().getName(),
                receiverUuid, receiver.getGameProfile().getName(),
                senderHats, receiverHats,
                System.currentTimeMillis());

            TradeOfferStore.CreateResult result = TradeOfferStore.get().addOffer(offer);
            if (result == TradeOfferStore.CreateResult.DUPLICATE) {
                NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.DUPLICATE_OFFER));
                return;
            }

            TradeOfferStore.get().closeSession(sender.getUUID());

            NetworkManager.sendToPlayer(receiver, HatsNetwork.INCOMING_TRADE_OFFER,
                PacketIncomingTradeOffer.encode(offer));
        });
    }

    private static HatPart findStored(PlayerHatData data, String name)
    {
        for (HatPart p : data.inventory) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }

    private static void pruneToOwnedAccessories(HatPart offered, HatPart stored)
    {
        if (stored == null) {
            offered.accessories.clear();
            return;
        }
        java.util.Set<String> owned = new java.util.HashSet<>();
        for (HatPart a : stored.accessories) owned.add(a.name);
        offered.accessories.removeIf(a -> !owned.contains(a.name));
    }
}
