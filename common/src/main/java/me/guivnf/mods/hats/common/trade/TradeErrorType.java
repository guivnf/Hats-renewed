package me.guivnf.mods.hats.common.trade;

public enum TradeErrorType
{
    DUPLICATE_OFFER,
    OUT_OF_RANGE,
    SENDER_HAT_EQUIPPED,
    RECEIVER_HAT_EQUIPPED,
    SENDER_MISSING_HAT,
    RECEIVER_MISSING_HAT,
    EMPTY_OFFER,
    RECEIVER_OFFLINE,
    OFFER_EXPIRED,
    OFFER_NOT_FOUND;

    public String messageKey()
    {
        return "hats.trade.error." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
