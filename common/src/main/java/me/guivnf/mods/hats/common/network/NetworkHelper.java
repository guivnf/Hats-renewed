package me.guivnf.mods.hats.common.network;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.network.packet.PacketHatManifest;
import me.guivnf.mods.hats.common.network.packet.PacketNewHatToast;
import me.guivnf.mods.hats.common.network.packet.PacketSyncEntityHat;
import me.guivnf.mods.hats.common.network.packet.PacketSyncPlayerInventory;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import me.guivnf.mods.hats.common.world.PlayerHatData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;

public class NetworkHelper
{
    public static void syncInventory(ServerPlayer player, HatsSavedData data)
    {
        PlayerHatData playerData = data.getPlayer(player.getUUID());
        if (playerData == null) return;
        NetworkManager.sendToPlayer(player, HatsNetwork.SYNC_PLAYER_INVENTORY, PacketSyncPlayerInventory.encode(playerData));
    }

    public static void broadcastEntityHat(LivingEntity entity)
    {
        if (!(entity.level() instanceof ServerLevel serverLevel)) return;

        HatsSavedData data = HatsSavedData.get(serverLevel);
        HatPart hat = data.getEntityHat(entity.getUUID());

        serverLevel.getServer().getPlayerList().getPlayers().forEach(player -> {
            if (player.level() == serverLevel) {
                NetworkManager.sendToPlayer(player, HatsNetwork.SYNC_ENTITY_HAT,
                    PacketSyncEntityHat.encode(entity.getUUID(), hat));
            }
        });
    }

    public static void syncAllEntityHats(ServerPlayer player, HatsSavedData data)
    {
        data.getAllEntityHats().forEach((uuid, hat) ->
            NetworkManager.sendToPlayer(player, HatsNetwork.SYNC_ENTITY_HAT,
                PacketSyncEntityHat.encode(uuid, hat)));
    }

    public static void sendManifest(ServerPlayer player)
    {
        NetworkManager.sendToPlayer(player, HatsNetwork.HAT_MANIFEST, PacketHatManifest.encode());
    }

    public static void openHatsGui(ServerPlayer player)
    {
        NetworkManager.sendToPlayer(player, HatsNetwork.OPEN_HATS_GUI,
            me.guivnf.mods.hats.common.network.packet.PacketOpenHatsGui.encode());
    }

    public static void sendNewHatToast(ServerPlayer player, HatPart hat, boolean isAccessory)
    {
        NetworkManager.sendToPlayer(player, HatsNetwork.NEW_HAT_TOAST, PacketNewHatToast.encode(hat, isAccessory));
    }
}
