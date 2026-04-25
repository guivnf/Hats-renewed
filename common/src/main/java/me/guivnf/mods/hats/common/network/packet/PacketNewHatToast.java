package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

public class PacketNewHatToast
{
    public static FriendlyByteBuf encode(HatPart hat, boolean isAccessory)
    {
        FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeNbt(hat.save());
        buf.writeBoolean(isAccessory);
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
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
