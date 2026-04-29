package me.guivnf.mods.hats.common.network;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
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

    public static final ResourceLocation REQUEST_NEARBY_PLAYERS = id("request_nearby_players");
    public static final ResourceLocation NEARBY_PLAYERS_LIST    = id("nearby_players_list");
    public static final ResourceLocation REQUEST_PEER_INVENTORY = id("request_peer_inventory");
    public static final ResourceLocation PEER_INVENTORY         = id("peer_inventory");
    public static final ResourceLocation SEND_TRADE_OFFER       = id("send_trade_offer");
    public static final ResourceLocation RESPOND_TRADE_OFFER    = id("respond_trade_offer");
    public static final ResourceLocation INCOMING_TRADE_OFFER   = id("incoming_trade_offer");
    public static final ResourceLocation TRADE_RESOLVED         = id("trade_resolved");
    public static final ResourceLocation TRADE_OFFER_REVOKED    = id("trade_offer_revoked");
    public static final ResourceLocation TRADE_PARTNER_LEFT     = id("trade_partner_left");
    public static final ResourceLocation TRADE_ERROR            = id("trade_error");
    public static final ResourceLocation CLOSE_TRADE_BUILDER    = id("close_trade_builder");

    public static void register()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, HAT_CUSTOMISATION,    PacketHatCustomisation::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_HAT_DATA,     PacketRequestHatData::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, GIVE_HAT,             PacketGiveHat::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, LAUNCH_HAT,           PacketLaunchHat::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_NEARBY_PLAYERS, PacketRequestNearbyPlayers::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, REQUEST_PEER_INVENTORY, PacketRequestPeerInventory::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SEND_TRADE_OFFER,       PacketSendTradeOffer::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, RESPOND_TRADE_OFFER,    PacketRespondTradeOffer::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, CLOSE_TRADE_BUILDER,    PacketCloseTradeBuilder::handle);

        // On a dedicated server we can't call registerReceiver(Side.S2C, ...) — that path
        // routes through ClientPlayNetworking which is @Environment(CLIENT)-stripped on the
        // server (AbstractMethodError). Instead we register the S2C payload TYPE only, so the
        // server can encode and send these packets to clients. The actual receiver lambdas
        // are wired up on the client by registerClient().
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(SYNC_ENTITY_HAT);
            NetworkManager.registerS2CPayloadType(SYNC_PLAYER_INVENTORY);
            NetworkManager.registerS2CPayloadType(HAT_MANIFEST);
            NetworkManager.registerS2CPayloadType(HAT_DATA_FRAGMENT);
            NetworkManager.registerS2CPayloadType(OPEN_HATS_GUI);
            NetworkManager.registerS2CPayloadType(NEW_HAT_TOAST);

            NetworkManager.registerS2CPayloadType(NEARBY_PLAYERS_LIST);
            NetworkManager.registerS2CPayloadType(PEER_INVENTORY);
            NetworkManager.registerS2CPayloadType(INCOMING_TRADE_OFFER);
            NetworkManager.registerS2CPayloadType(TRADE_RESOLVED);
            NetworkManager.registerS2CPayloadType(TRADE_OFFER_REVOKED);
            NetworkManager.registerS2CPayloadType(TRADE_PARTNER_LEFT);
            NetworkManager.registerS2CPayloadType(TRADE_ERROR);
        }
    }

    public static void registerClient()
    {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_ENTITY_HAT,      PacketSyncEntityHat::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_PLAYER_INVENTORY, PacketSyncPlayerInventory::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HAT_MANIFEST,         PacketHatManifest::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HAT_DATA_FRAGMENT,    PacketHatDataFragment::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPEN_HATS_GUI,        PacketOpenHatsGui::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NEW_HAT_TOAST,        PacketNewHatToast::handle);

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, NEARBY_PLAYERS_LIST,  PacketNearbyPlayersList::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, PEER_INVENTORY,       PacketPeerInventory::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, INCOMING_TRADE_OFFER, PacketIncomingTradeOffer::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TRADE_RESOLVED,       PacketTradeResolved::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TRADE_OFFER_REVOKED,  PacketTradeOfferRevoked::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TRADE_PARTNER_LEFT,   PacketTradePartnerLeft::handle);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TRADE_ERROR,          PacketTradeError::handle);
    }

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(HatsMod.MOD_ID, path);
    }
}
