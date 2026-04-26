package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketNearbyPlayersList
{
    public record Entry(UUID uuid, String name) {}

    public static FriendlyByteBuf encode(List<ServerPlayer> players)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(players.size());
        for (ServerPlayer p : players) {
            buf.writeUUID(p.getUUID());
            buf.writeUtf(p.getGameProfile().getName());
        }
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        int n = buf.readInt();
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            UUID uuid = buf.readUUID();
            String name = buf.readUtf();
            entries.add(new Entry(uuid, name));
        }

        context.queue(() -> me.guivnf.mods.hats.client.trade.ClientTradeState.setNearbyPlayers(entries));
    }
}
