package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketPeerInventory
{
    public static RegistryFriendlyByteBuf encode(UUID owner, @Nullable HatPart equipped, List<HatPart> inventory)
    {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), RegistryAccess.EMPTY);
        buf.writeUUID(owner);
        buf.writeBoolean(equipped != null);
        if (equipped != null) buf.writeNbt(equipped.save());
        buf.writeInt(inventory.size());
        for (HatPart h : inventory) buf.writeNbt(h.save());
        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID owner = buf.readUUID();
        HatPart equipped = null;
        if (buf.readBoolean()) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) equipped = HatPart.load(tag);
        }
        int n = buf.readInt();
        List<HatPart> inv = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) inv.add(HatPart.load(tag));
        }

        HatPart finalEquipped = equipped;
        context.queue(() -> {
            me.guivnf.mods.hats.client.trade.ClientTradeState.setPeerInventory(owner, inv);
            me.guivnf.mods.hats.client.trade.ClientTradeState.setPeerEquipped(owner, finalEquipped);
        });
    }
}
