package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class PacketHatCustomisation
{
    public static FriendlyByteBuf encode(HatPart hat)
    {
        FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeBoolean(hat != null);
        if (hat != null) {
            buf.writeNbt(hat.save());
        }
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        HatPart hat = null;
        if (buf.readBoolean()) {
            CompoundTag tag = buf.readNbt();
            hat = tag != null ? HatPart.load(tag) : null;
        }

        HatPart finalHat = hat;
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            HatsSavedData data = HatsSavedData.get(player.serverLevel());

            if (finalHat == null) {
                data.removeEquipped(player.getUUID());
                data.setEntityHat(player.getUUID(), null);
            } else {
                finalHat.isShowing = true;
                data.equipHat(player.getUUID(), finalHat);
                data.setEntityHat(player.getUUID(), finalHat);
            }

            me.guivnf.mods.hats.common.network.NetworkHelper.broadcastEntityHat(player);
        });
    }
}
