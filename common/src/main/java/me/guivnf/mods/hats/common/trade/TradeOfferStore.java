package me.guivnf.mods.hats.common.trade;

import me.guivnf.mods.hats.common.hat.HatPart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TradeOfferStore
{
    public static final double TRADE_RANGE_BLOCKS = 4.0;

    private static final TradeOfferStore INSTANCE = new TradeOfferStore();

    public static TradeOfferStore get() { return INSTANCE; }

    private final Map<UUID, TradeOffer> offers = new HashMap<>();
    private final Map<UUID, Set<UUID>> bySender   = new HashMap<>();
    private final Map<UUID, Set<UUID>> byReceiver = new HashMap<>();

    private final Map<UUID, TradeBuilderSession> sessions = new HashMap<>();

    public enum CreateResult { OK, DUPLICATE }

    public CreateResult addOffer(TradeOffer offer)
    {
        if (findPair(offer.senderUuid, offer.receiverUuid) != null) return CreateResult.DUPLICATE;
        offers.put(offer.offerId, offer);
        bySender  .computeIfAbsent(offer.senderUuid,   k -> new HashSet<>()).add(offer.offerId);
        byReceiver.computeIfAbsent(offer.receiverUuid, k -> new HashSet<>()).add(offer.offerId);
        return CreateResult.OK;
    }

    public TradeOffer get(UUID offerId)
    {
        return offers.get(offerId);
    }

    public TradeOffer findPair(UUID sender, UUID receiver)
    {
        Set<UUID> senderOffers = bySender.get(sender);
        if (senderOffers == null) return null;
        for (UUID id : senderOffers) {
            TradeOffer o = offers.get(id);
            if (o != null && o.receiverUuid.equals(receiver)) return o;
        }
        return null;
    }

    public TradeOffer remove(UUID offerId)
    {
        TradeOffer offer = offers.remove(offerId);
        if (offer != null) {
            Set<UUID> s = bySender.get(offer.senderUuid);
            if (s != null) { s.remove(offerId); if (s.isEmpty()) bySender.remove(offer.senderUuid); }
            Set<UUID> r = byReceiver.get(offer.receiverUuid);
            if (r != null) { r.remove(offerId); if (r.isEmpty()) byReceiver.remove(offer.receiverUuid); }
        }
        return offer;
    }

    public List<TradeOffer> getOffersForReceiver(UUID receiver)
    {
        Set<UUID> ids = byReceiver.get(receiver);
        if (ids == null || ids.isEmpty()) return List.of();
        List<TradeOffer> out = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            TradeOffer o = offers.get(id);
            if (o != null) out.add(o);
        }
        return out;
    }

    public List<TradeOffer> removeExpired(long nowMs)
    {
        List<TradeOffer> expired = new ArrayList<>();
        Iterator<Map.Entry<UUID, TradeOffer>> it = offers.entrySet().iterator();
        while (it.hasNext()) {
            TradeOffer o = it.next().getValue();
            if (o.isExpired(nowMs)) {
                expired.add(o);
                it.remove();
                Set<UUID> s = bySender.get(o.senderUuid);
                if (s != null) { s.remove(o.offerId); if (s.isEmpty()) bySender.remove(o.senderUuid); }
                Set<UUID> r = byReceiver.get(o.receiverUuid);
                if (r != null) { r.remove(o.offerId); if (r.isEmpty()) byReceiver.remove(o.receiverUuid); }
            }
        }
        return expired;
    }

    public List<TradeOffer> removeAllInvolving(UUID playerUuid)
    {
        List<TradeOffer> removed = new ArrayList<>();
        Set<UUID> ids = new HashSet<>();
        Set<UUID> a = bySender.get(playerUuid);
        Set<UUID> b = byReceiver.get(playerUuid);
        if (a != null) ids.addAll(a);
        if (b != null) ids.addAll(b);
        for (UUID id : ids) {
            TradeOffer o = remove(id);
            if (o != null) removed.add(o);
        }
        return removed;
    }

    public void openSession(TradeBuilderSession session)
    {
        sessions.put(session.initiatorUuid, session);
    }

    public TradeBuilderSession closeSession(UUID initiatorUuid)
    {
        return sessions.remove(initiatorUuid);
    }

    public TradeBuilderSession getSession(UUID initiatorUuid)
    {
        return sessions.get(initiatorUuid);
    }

    public java.util.Collection<TradeBuilderSession> allSessions()
    {
        return new ArrayList<>(sessions.values());
    }

    public static boolean offerNamesEqual(List<HatPart> a, List<HatPart> b)
    {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).name.equals(b.get(i).name)) return false;
        }
        return true;
    }
}
