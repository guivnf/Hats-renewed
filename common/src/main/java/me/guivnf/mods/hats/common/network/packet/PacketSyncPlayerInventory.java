package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.world.PlayerHatData;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketSyncPlayerInventory
{
    public static RegistryFriendlyByteBuf encode(PlayerHatData data)
    {
        RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeUUID(data.owner);
        buf.writeInt(data.tokens);

        buf.writeBoolean(data.equippedHat != null);
        if (data.equippedHat != null) {
            buf.writeNbt(data.equippedHat.save());
        }

        buf.writeInt(data.inventory.size());
        for (HatPart part : data.inventory) {
            buf.writeNbt(part.save());
        }

        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID owner = buf.readUUID();
        int tokens = buf.readInt();

        HatPart equipped = null;
        if (buf.readBoolean()) {
            equipped = HatPart.load(buf.readNbt());
        }

        int count = buf.readInt();
        List<HatPart> inventory = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            inventory.add(HatPart.load(buf.readNbt()));
        }

        HatPart finalEquipped = equipped;
        context.queue(() -> handleClient(owner, tokens, finalEquipped, inventory));
    }


    private static void handleClient(UUID owner, int tokens, HatPart equipped, List<HatPart> inventory)
    {
        me.guivnf.mods.hats.client.cache.ClientHatCache.setPlayerInventory(owner, tokens, equipped, inventory);
    }
}
