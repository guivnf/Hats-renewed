package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.trade.TradeErrorType;
import net.minecraft.network.FriendlyByteBuf;

public class PacketTradeError
{
    public static FriendlyByteBuf encode(TradeErrorType type)
    {
        return encode(type, "");
    }

    public static FriendlyByteBuf encode(TradeErrorType type, String details)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeEnum(type);
        buf.writeUtf(details == null ? "" : details);
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        TradeErrorType type = buf.readEnum(TradeErrorType.class);
        String details = buf.readUtf();
        context.queue(() -> me.guivnf.mods.hats.client.trade.ClientTradeState.setError(type, details));
    }
}
