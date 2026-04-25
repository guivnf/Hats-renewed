package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.entity.HatEntity;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.network.NetworkHelper;
import me.guivnf.mods.hats.common.registry.HatsRegistries;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

public class PacketLaunchHat
{
    public static FriendlyByteBuf encode(String hatName)
    {
        FriendlyByteBuf buf = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUtf(hatName);
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        String hatName = buf.readUtf();
        context.queue(() -> {
            ServerPlayer sp = (ServerPlayer) context.getPlayer();
            HatsSavedData data = HatsSavedData.get(sp.serverLevel());

            HatPart hat;
            if (hatName.isEmpty()) {
                hat = data.takeRandomNonFavourite(sp.getUUID());
            } else {
                hat = data.takeSpecificHat(sp.getUUID(), hatName);
            }

            if (hat == null) return;

            HatEntity projectile = HatEntity.launch(sp.serverLevel(), sp, hat);
            sp.serverLevel().addFreshEntity(projectile);

            if (HatsRegistries.BONK.isPresent()) {
                sp.serverLevel().playSound(null, sp.blockPosition(),
                    HatsRegistries.BONK.get(), SoundSource.PLAYERS,
                    0.8f, 0.9f + sp.serverLevel().random.nextFloat() * 0.2f);
            }

            NetworkHelper.syncInventory(sp, data);
            sp.getCooldowns().addCooldown(HatsRegistries.HAT_LAUNCHER.get(), 10);
        });
    }
}
