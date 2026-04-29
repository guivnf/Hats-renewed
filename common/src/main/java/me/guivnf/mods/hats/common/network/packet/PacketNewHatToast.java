package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

public class PacketNewHatToast
{
    public static RegistryFriendlyByteBuf encode(HatPart hat, boolean isAccessory)
    {
        RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeNbt(hat.save());
        buf.writeBoolean(isAccessory);
        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        CompoundTag tag = buf.readNbt();
        HatPart hat = tag != null ? HatPart.load(tag) : null;
        boolean isAccessory = buf.readBoolean();
        if (hat == null) return;

        context.queue(() -> showToast(hat, isAccessory));
    }

    private static void showToast(HatPart hat, boolean isAccessory)
    {
        if (!me.guivnf.mods.hats.HatsMod.getConfig().displayHatUnlockToast) return;
        me.guivnf.mods.hats.client.toast.HatToast.show(hat, isAccessory);
    }
}
