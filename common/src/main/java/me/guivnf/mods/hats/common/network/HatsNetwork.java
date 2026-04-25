package me.guivnf.mods.hats.common.network;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.network.packet.*;
import net.minecraft.resources.ResourceLocation;

public class HatsNetwork
{
    public static final ResourceLocation SYNC_ENTITY_HAT        = id("sync_entity_hat");
    public static final ResourceLocation SYNC_PLAYER_INVENTORY  = id("sync_player_inventory");
    public static final ResourceLocation HAT_MANIFEST           = id("hat_manifest");
    public static final ResourceLocation REQUEST_HAT_DATA       = id("request_hat_data");
    public static final ResourceLocation HAT_DATA_FRAGMENT      = id("hat_data_fragment");
    public static final ResourceLocation HAT_CUSTOMISATION      = id("hat_customisation");
    public static final ResourceLocation OPEN_HATS_GUI          = id("open_hats_gui");
    public static final ResourceLocation GIVE_HAT               = id("give_hat");
    public static final ResourceLocation NEW_HAT_TOAST          = id("new_hat_toast");
    public static final ResourceLocation LAUNCH_HAT             = id("launch_hat");

    public static void register()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, HAT_CUSTOMISATION,    PacketHatCustomisation::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_HAT_DATA,     PacketRequestHatData::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GIVE_HAT,             PacketGiveHat::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LAUNCH_HAT,           PacketLaunchHat::handle);
    }

    public static void registerClient()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_ENTITY_HAT,      PacketSyncEntityHat::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_PLAYER_INVENTORY, PacketSyncPlayerInventory::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HAT_MANIFEST,         PacketHatManifest::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HAT_DATA_FRAGMENT,    PacketHatDataFragment::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPEN_HATS_GUI,        PacketOpenHatsGui::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NEW_HAT_TOAST,        PacketNewHatToast::handle);
    }

    private static ResourceLocation id(String path)
    {
        return new ResourceLocation(HatsMod.MOD_ID, path);
    }
}
