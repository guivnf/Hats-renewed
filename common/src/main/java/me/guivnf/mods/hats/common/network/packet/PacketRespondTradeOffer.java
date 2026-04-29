package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.NetworkHelper;
import me.guivnf.mods.hats.common.trade.TradeErrorType;
import me.guivnf.mods.hats.common.trade.TradeOffer;
import me.guivnf.mods.hats.common.trade.TradeOfferStore;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import me.guivnf.mods.hats.common.world.PlayerHatData;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

public class PacketRespondTradeOffer
{
    public static RegistryFriendlyByteBuf encode(UUID offerId, boolean accepted)
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeUUID(offerId);
        buf.writeBoolean(accepted);
        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID offerId = buf.readUUID();
        boolean accepted = buf.readBoolean();

        context.queue(() -> {
            ServerPlayer responder = (ServerPlayer) context.getPlayer();
            TradeOffer offer = TradeOfferStore.get().get(offerId);

            if (offer == null) {
                NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.OFFER_NOT_FOUND));
                return;
            }
            if (!offer.receiverUuid.equals(responder.getUUID())) return;

            if (offer.isExpired(System.currentTimeMillis())) {
                TradeOfferStore.get().remove(offerId);
                NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.OFFER_EXPIRED));
                NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_OFFER_REVOKED,
                    PacketTradeOfferRevoked.encode(offerId));
                return;
            }

            ServerPlayer sender = responder.getServer().getPlayerList().getPlayer(offer.senderUuid);

            if (!accepted) {
                TradeOfferStore.get().remove(offerId);
                NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_OFFER_REVOKED,
                    PacketTradeOfferRevoked.encode(offerId));
                if (sender != null) {
                    NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_RESOLVED,
                        PacketTradeResolved.encode(offerId, false, offer.receiverName));
                }
                return;
            }

            if (sender == null || sender.level() != responder.level()) {
                TradeOfferStore.get().remove(offerId);
                NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.RECEIVER_OFFLINE));
                NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_OFFER_REVOKED,
                    PacketTradeOfferRevoked.encode(offerId));
                return;
            }

            double range = TradeOfferStore.TRADE_RANGE_BLOCKS;
            if (sender.distanceToSqr(responder) > range * range) {
                NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_ERROR,
                    PacketTradeError.encode(TradeErrorType.OUT_OF_RANGE));
                return;
            }

            HatsSavedData data = HatsSavedData.get(sender.serverLevel());
            PlayerHatData senderData   = data.getOrCreatePlayer(sender.getUUID());
            PlayerHatData receiverData = data.getOrCreatePlayer(responder.getUUID());

            for (HatPart h : offer.senderHats) {
                if (!senderData.hasHat(h.name)) {
                    revokeAndError(responder, sender, offer, TradeErrorType.SENDER_MISSING_HAT, h.name);
                    return;
                }
                if (senderData.equippedHat != null && senderData.equippedHat.name.equals(h.name)) {
                    revokeAndError(responder, sender, offer, TradeErrorType.SENDER_HAT_EQUIPPED, h.name);
                    return;
                }
            }
            for (HatPart h : offer.receiverHats) {
                if (!receiverData.hasHat(h.name)) {
                    revokeAndError(responder, sender, offer, TradeErrorType.RECEIVER_MISSING_HAT, h.name);
                    return;
                }
                if (receiverData.equippedHat != null && receiverData.equippedHat.name.equals(h.name)) {
                    revokeAndError(responder, sender, offer, TradeErrorType.RECEIVER_HAT_EQUIPPED, h.name);
                    return;
                }
            }

            for (HatPart h : offer.senderHats) {
                HatPart taken = senderData.takeSpecific(h.name);
                if (taken != null) receiverData.addHat(applyOfferedAccessories(taken, h));
            }
            for (HatPart h : offer.receiverHats) {
                HatPart taken = receiverData.takeSpecific(h.name);
                if (taken != null) senderData.addHat(applyOfferedAccessories(taken, h));
            }
            data.setDirty();

            TradeOfferStore.get().remove(offerId);

            NetworkHelper.syncInventory(sender, data);
            NetworkHelper.syncInventory(responder, data);

            NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_OFFER_REVOKED,
                PacketTradeOfferRevoked.encode(offerId));

            NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_RESOLVED,
                PacketTradeResolved.encode(offerId, true, offer.receiverName));
            NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_RESOLVED,
                PacketTradeResolved.encode(offerId, true, offer.senderName));
        });
    }

    private static HatPart applyOfferedAccessories(HatPart taken, HatPart offered)
    {
        taken.accessories.clear();
        for (HatPart a : offered.accessories) {
            if (!a.isShowing) continue;
            taken.accessories.add(a.copy());
        }
        return taken;
    }

    private static void revokeAndError(ServerPlayer responder, ServerPlayer sender,
            TradeOffer offer, TradeErrorType type, String detail)
    {
        TradeOfferStore.get().remove(offer.offerId);
        NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_ERROR,
            PacketTradeError.encode(type, detail));
        NetworkManager.sendToPlayer(responder, HatsNetwork.TRADE_OFFER_REVOKED,
            PacketTradeOfferRevoked.encode(offer.offerId));
        if (sender != null) {
            NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_ERROR,
                PacketTradeError.encode(type, detail));
        }
    }
}
