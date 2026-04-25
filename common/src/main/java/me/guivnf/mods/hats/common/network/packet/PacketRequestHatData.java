package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public class PacketRequestHatData
{
    private static final int FRAGMENT_SIZE = 32_768;

    public static FriendlyByteBuf encode(List<String> hatNames)
    {
        FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(hatNames.size());
        for (String name : hatNames) {
            buf.writeUtf(name);
        }
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        int count = buf.readInt();
        List<String> requested = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            requested.add(buf.readUtf());
        }

        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            for (String name : requested) {
                HatDefinition def = HatRegistry.get(name);
                if (def == null) continue;
                sendFragments(player, def);
            }
        });
    }

    private static void sendFragments(ServerPlayer player, HatDefinition def)
    {
        byte[] texture = def.textureBytes != null ? def.textureBytes : new byte[0];
        int totalFragments = (texture.length + FRAGMENT_SIZE - 1) / FRAGMENT_SIZE;
        if (totalFragments == 0) totalFragments = 1;

        for (int i = 0; i < totalFragments; i++) {
            int from = i * FRAGMENT_SIZE;
            int to = Math.min(from + FRAGMENT_SIZE, texture.length);
            byte[] chunk = java.util.Arrays.copyOfRange(texture, from, to);

            FriendlyByteBuf fragmentBuf = PacketHatDataFragment.encode(
                def.name, def.pack.id, i, totalFragments, chunk,
                i == 0 ? def.modelData : null,
                i == 0 ? def.meta : null
            );
            NetworkManager.sendToPlayer(player, HatsNetwork.HAT_DATA_FRAGMENT, fragmentBuf);
        }
    }
}
