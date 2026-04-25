package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.hat.HatPart;


import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;

import java.util.UUID;

public class PacketSyncEntityHat
{
    public static FriendlyByteBuf encode(UUID entityUuid, HatPart hat)
    {
        FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUUID(entityUuid);
        buf.writeBoolean(hat != null);
        if (hat != null) {
            buf.writeNbt(hat.save());
        }
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID entityUuid = buf.readUUID();
        boolean hasHat = buf.readBoolean();
        HatPart hat = hasHat ? HatPart.load(buf.readNbt()) : null;

        context.queue(() -> handleClient(entityUuid, hat));
    }


    private static void handleClient(UUID entityUuid, HatPart hat)
    {
        me.guivnf.mods.hats.client.cache.ClientHatCache.setEntityHat(entityUuid, hat);
    }
}
