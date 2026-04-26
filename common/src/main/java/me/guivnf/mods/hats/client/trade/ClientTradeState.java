package me.guivnf.mods.hats.client.trade;

import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.network.packet.PacketNearbyPlayersList;
import me.guivnf.mods.hats.common.trade.TradeErrorType;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ClientTradeState
{
    public static class IncomingOffer
    {
        public final UUID offerId;
        public final UUID senderUuid;
        public final String senderName;
        public final List<HatPart> senderHats;
        public final List<HatPart> receiverHats;
        public final long createdAtClientMs;

        public IncomingOffer(UUID offerId, UUID senderUuid, String senderName,
                             List<HatPart> senderHats, List<HatPart> receiverHats,
                             long createdAtClientMs)
        {
            this.offerId = offerId;
            this.senderUuid = senderUuid;
            this.senderName = senderName;
            this.senderHats = senderHats;
            this.receiverHats = receiverHats;
            this.createdAtClientMs = createdAtClientMs;
        }
    }

    private static List<PacketNearbyPlayersList.Entry> nearby = new ArrayList<>();
    private static boolean nearbyReceived = false;

    private static final Map<UUID, List<HatPart>> peerInventories = new LinkedHashMap<>();
    private static final Map<UUID, HatPart> peerEquipped = new LinkedHashMap<>();

    private static final Map<UUID, IncomingOffer> incomingOffers = new LinkedHashMap<>();

    @Nullable private static UUID activeBuilderTarget;

    @Nullable private static TradeErrorType pendingError;
    @Nullable private static String pendingErrorDetails;

    public static void setNearbyPlayers(List<PacketNearbyPlayersList.Entry> players)
    {
        nearby = new ArrayList<>(players);
        nearbyReceived = true;
    }

    public static void clearNearby()
    {
        nearby = new ArrayList<>();
        nearbyReceived = false;
    }

    public static List<PacketNearbyPlayersList.Entry> getNearby() { return Collections.unmodifiableList(nearby); }
    public static boolean isNearbyReceived() { return nearbyReceived; }

    public static void setPeerInventory(UUID owner, List<HatPart> hats)
    {
        peerInventories.put(owner, hats);
    }

    @Nullable
    public static List<HatPart> getPeerInventory(UUID owner)
    {
        return peerInventories.get(owner);
    }

    public static void clearPeerInventory(UUID owner)
    {
        peerInventories.remove(owner);
        peerEquipped.remove(owner);
    }

    public static void setPeerEquipped(UUID owner, @Nullable HatPart equipped)
    {
        if (equipped == null) peerEquipped.remove(owner);
        else peerEquipped.put(owner, equipped);
    }

    @Nullable
    public static HatPart getPeerEquipped(UUID owner)
    {
        return peerEquipped.get(owner);
    }

    public static void addIncomingOffer(IncomingOffer offer)
    {
        incomingOffers.put(offer.offerId, offer);
    }

    public static void removeIncomingOffer(UUID offerId)
    {
        incomingOffers.remove(offerId);
    }

    public static java.util.Collection<IncomingOffer> getIncomingOffers()
    {
        return Collections.unmodifiableCollection(incomingOffers.values());
    }

    @Nullable
    public static IncomingOffer getIncomingOffer(UUID offerId)
    {
        return incomingOffers.get(offerId);
    }

    public static boolean hasIncomingOffers()
    {
        return !incomingOffers.isEmpty();
    }

    public static void setActiveBuilderTarget(@Nullable UUID target)
    {
        activeBuilderTarget = target;
    }

    @Nullable
    public static UUID getActiveBuilderTarget() { return activeBuilderTarget; }

    public static void setError(TradeErrorType type, @Nullable String details)
    {
        pendingError = type;
        pendingErrorDetails = details;
    }

    @Nullable public static TradeErrorType consumeError()
    {
        TradeErrorType e = pendingError;
        pendingError = null;
        return e;
    }

    @Nullable public static String getErrorDetails() { return pendingErrorDetails; }

    public static void reset()
    {
        nearby = new ArrayList<>();
        nearbyReceived = false;
        peerInventories.clear();
        peerEquipped.clear();
        incomingOffers.clear();
        activeBuilderTarget = null;
        pendingError = null;
        pendingErrorDetails = null;
    }
}
