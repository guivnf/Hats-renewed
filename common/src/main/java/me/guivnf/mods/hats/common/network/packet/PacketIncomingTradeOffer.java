package me.guivnf.mods.hats.common.network.packet;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.trade.TradeOffer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PacketIncomingTradeOffer
{
    public static FriendlyByteBuf encode(TradeOffer offer)
    {
        FriendlyByteBuf buf = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeUUID(offer.offerId);
        buf.writeUUID(offer.senderUuid);
        buf.writeUtf(offer.senderName);
        buf.writeInt(offer.senderHats.size());
        for (HatPart h : offer.senderHats) buf.writeNbt(h.save());
        buf.writeInt(offer.receiverHats.size());
        for (HatPart h : offer.receiverHats) buf.writeNbt(h.save());
        buf.writeLong(offer.createdAtMs);
        return buf;
    }

    public static void handle(FriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        UUID offerId = buf.readUUID();
        UUID senderUuid = buf.readUUID();
        String senderName = buf.readUtf();
        int sn = buf.readInt();
        List<HatPart> senderHats = new ArrayList<>(sn);
        for (int i = 0; i < sn; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) senderHats.add(HatPart.load(tag));
        }
        int rn = buf.readInt();
        List<HatPart> receiverHats = new ArrayList<>(rn);
        for (int i = 0; i < rn; i++) {
            CompoundTag tag = buf.readNbt();
            if (tag != null) receiverHats.add(HatPart.load(tag));
        }
        long createdAt = buf.readLong();

        context.queue(() -> {
            long elapsed = System.currentTimeMillis() - createdAt;
            long localCreated = System.currentTimeMillis() - Math.max(0L, elapsed);
            me.guivnf.mods.hats.client.trade.ClientTradeState.IncomingOffer offer =
                new me.guivnf.mods.hats.client.trade.ClientTradeState.IncomingOffer(
                    offerId, senderUuid, senderName, senderHats, receiverHats, localCreated);
            me.guivnf.mods.hats.client.trade.ClientTradeState.addIncomingOffer(offer);
            me.guivnf.mods.hats.client.toast.TradeRequestToast.show(senderName);
        });
    }
}
