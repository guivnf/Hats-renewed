package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.trade.TradeErrorType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

public class PacketTradeError
{
    public static RegistryFriendlyByteBuf encode(TradeErrorType type)
    {
        return encode(type, "");
    }

    public static RegistryFriendlyByteBuf encode(TradeErrorType type, String details)
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeEnum(type);
        buf.writeUtf(details == null ? "" : details);
        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        TradeErrorType type = buf.readEnum(TradeErrorType.class);
        String details = buf.readUtf();
        context.queue(() -> me.guivnf.mods.hats.client.trade.ClientTradeState.setError(type, details));
    }
}
