package me.guivnf.mods.hats.common.trade;

import java.util.UUID;

public class TradeBuilderSession
{
    public final UUID initiatorUuid;
    public final UUID targetUuid;
    public final long openedAtMs;

    public TradeBuilderSession(UUID initiatorUuid, UUID targetUuid, long openedAtMs)
    {
        this.initiatorUuid = initiatorUuid;
        this.targetUuid = targetUuid;
        this.openedAtMs = openedAtMs;
    }
}
