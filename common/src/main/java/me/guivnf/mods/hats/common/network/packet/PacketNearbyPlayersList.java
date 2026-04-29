package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketNearbyPlayersList
{
    public record Entry(UUID uuid, String name) {}

    public static RegistryFriendlyByteBuf encode(List<ServerPlayer> players)
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeInt(players.size());
        for (ServerPlayer p : players) {
            buf.writeUUID(p.getUUID());
            buf.writeUtf(p.getGameProfile().getName());
        }
        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
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
