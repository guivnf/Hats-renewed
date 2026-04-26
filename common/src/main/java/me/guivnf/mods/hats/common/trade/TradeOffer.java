package me.guivnf.mods.hats.common.trade;

import me.guivnf.mods.hats.common.hat.HatPart;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeOffer
{
    public static final long EXPIRY_MS = 5L * 60L * 1000L;

    public final UUID offerId;
    public final UUID senderUuid;
    public final String senderName;
    public final UUID receiverUuid;
    public final String receiverName;
    public final List<HatPart> senderHats;
    public final List<HatPart> receiverHats;
    public final long createdAtMs;

    public TradeOffer(UUID offerId, UUID senderUuid, String senderName,
                      UUID receiverUuid, String receiverName,
                      List<HatPart> senderHats, List<HatPart> receiverHats,
                      long createdAtMs)
    {
        this.offerId = offerId;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.senderHats = senderHats;
        this.receiverHats = receiverHats;
        this.createdAtMs = createdAtMs;
    }

    public boolean isExpired(long nowMs)
    {
        return nowMs - createdAtMs >= EXPIRY_MS;
    }

    public long remainingMs(long nowMs)
    {
        return Math.max(0L, EXPIRY_MS - (nowMs - createdAtMs));
    }

    public static List<HatPart> copyList(List<HatPart> src)
    {
        List<HatPart> copy = new ArrayList<>(src.size());
        for (HatPart p : src) copy.add(p.copy());
        return copy;
    }
}
