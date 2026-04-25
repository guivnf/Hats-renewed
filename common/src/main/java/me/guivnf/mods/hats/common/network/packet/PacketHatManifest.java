package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatRegistry;


import net.minecraft.network.FriendlyByteBuf;

import java.security.MessageDigest;
import java.util.*;

public class PacketHatManifest
{
    public static FriendlyByteBuf encode()
    {
        Collection<HatDefinition> all = HatRegistry.getAll();
        FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        List<HatDefinition> mainHats = all.stream().filter(h -> !h.isAccessory()).toList();
        buf.writeInt(mainHats.size());
        for (HatDefinition hat : mainHats) {
            buf.writeUtf(hat.name);
            buf.writeUtf(hat.pack.id);
            buf.writeByteArray(hash(hat.textureBytes));
        }
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        int count = buf.readInt();
        Map<String, ManifestEntry> entries = new LinkedHashMap<>(count);
        for (int i = 0; i < count; i++) {
            String name = buf.readUtf();
            String packId = buf.readUtf();
            byte[] hash = buf.readByteArray();
            entries.put(name, new ManifestEntry(name, packId, hash));
        }
        context.queue(() -> handleClient(entries));
    }


    private static void handleClient(Map<String, ManifestEntry> entries)
    {
        me.guivnf.mods.hats.client.cache.ClientHatCache.processManifest(entries);
    }

    private static byte[] hash(byte[] data)
    {
        if (data == null) return new byte[0];
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            return new byte[0];
        }
    }

    public record ManifestEntry(String name, String packId, byte[] hash) {}
}
