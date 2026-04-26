package me.guivnf.mods.hats.client.trade;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.client.gui.HatsScreen;
import me.guivnf.mods.hats.client.gui.trade.TradeIncomingScreen;
import me.guivnf.mods.hats.client.gui.trade.TradeOfferScreen;
import me.guivnf.mods.hats.client.gui.trade.TradePartnerSelectScreen;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.packet.PacketCloseTradeBuilder;
import me.guivnf.mods.hats.common.network.packet.PacketRequestNearbyPlayers;
import me.guivnf.mods.hats.common.network.packet.PacketRequestPeerInventory;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public class ClientTradeFlow
{
    public static void openPartnerSelect()
    {
        ClientTradeState.clearNearby();
        NetworkManager.sendToServer(HatsNetwork.REQUEST_NEARBY_PLAYERS, PacketRequestNearbyPlayers.encode());
        Minecraft.getInstance().setScreen(new TradePartnerSelectScreen());
    }

    public static void openOfferBuilder(UUID targetUuid, String targetName)
    {
        ClientTradeState.setActiveBuilderTarget(targetUuid);
        NetworkManager.sendToServer(HatsNetwork.REQUEST_PEER_INVENTORY,
            PacketRequestPeerInventory.encode(targetUuid));
        Minecraft.getInstance().setScreen(new TradeOfferScreen(targetUuid, targetName));
    }

    public static void openIncoming(UUID offerId)
    {
        ClientTradeState.IncomingOffer offer = ClientTradeState.getIncomingOffer(offerId);
        if (offer == null) return;
        Minecraft.getInstance().setScreen(new TradeIncomingScreen(offer));
    }

    public static void closeBuilder()
    {
        ClientTradeState.setActiveBuilderTarget(null);
        NetworkManager.sendToServer(HatsNetwork.CLOSE_TRADE_BUILDER, PacketCloseTradeBuilder.encode());
    }

    public static void onPartnerLeft(UUID targetUuid)
    {
        UUID active = ClientTradeState.getActiveBuilderTarget();
        if (active == null || !active.equals(targetUuid)) return;
        ClientTradeState.setActiveBuilderTarget(null);
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof TradeOfferScreen || mc.screen instanceof TradePartnerSelectScreen) {
            HatsScreen.open();
            if (mc.player != null) {
                mc.player.displayClientMessage(
                    Component.literal("Trade partner left your range.")
                        .withStyle(net.minecraft.ChatFormatting.RED), true);
            }
        }
    }
}
