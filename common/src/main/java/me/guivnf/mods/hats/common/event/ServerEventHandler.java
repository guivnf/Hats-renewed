package me.guivnf.mods.hats.common.event;

import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.NetworkHelper;
import me.guivnf.mods.hats.common.network.packet.PacketTradeOfferRevoked;
import me.guivnf.mods.hats.common.network.packet.PacketTradePartnerLeft;
import me.guivnf.mods.hats.common.trade.TradeBuilderSession;
import me.guivnf.mods.hats.common.trade.TradeOffer;
import me.guivnf.mods.hats.common.trade.TradeOfferStore;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import me.guivnf.mods.hats.common.world.PlayerHatData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ServerEventHandler
{
    public static void register()
    {
        EntityEvent.LIVING_DEATH.register(ServerEventHandler::onEntityDeath);
        EntityEvent.ADD.register(ServerEventHandler::onEntityAdd);
        PlayerEvent.PLAYER_JOIN.register(ServerEventHandler::onPlayerJoin);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, removalReason) -> onPlayerRespawn(player));

        TickEvent.SERVER_LEVEL_POST.register(ServerEventHandler::onServerLevelTick);
        TickEvent.SERVER_POST.register(ServerEventHandler::onServerTick);

        PlayerEvent.PLAYER_QUIT.register(ServerEventHandler::onPlayerQuit);

        dev.architectury.event.events.common.LifecycleEvent.SERVER_LEVEL_LOAD.register(
            level -> onLevelLoad((ServerLevel) level));
    }

    private static int tradeTickCounter = 0;

    private static void onServerTick(net.minecraft.server.MinecraftServer server)
    {
        long now = System.currentTimeMillis();

        for (TradeOffer expired : TradeOfferStore.get().removeExpired(now)) {
            ServerPlayer sender = server.getPlayerList().getPlayer(expired.senderUuid);
            ServerPlayer receiver = server.getPlayerList().getPlayer(expired.receiverUuid);
            if (sender != null) {
                NetworkManager.sendToPlayer(sender, HatsNetwork.TRADE_OFFER_REVOKED,
                    PacketTradeOfferRevoked.encode(expired.offerId));
            }
            if (receiver != null) {
                NetworkManager.sendToPlayer(receiver, HatsNetwork.TRADE_OFFER_REVOKED,
                    PacketTradeOfferRevoked.encode(expired.offerId));
            }
        }

        tradeTickCounter++;
        if (tradeTickCounter >= 10) {
            tradeTickCounter = 0;
            double range = TradeOfferStore.TRADE_RANGE_BLOCKS;
            double rSq = range * range;
            for (TradeBuilderSession session : TradeOfferStore.get().allSessions()) {
                ServerPlayer initiator = server.getPlayerList().getPlayer(session.initiatorUuid);
                ServerPlayer target = server.getPlayerList().getPlayer(session.targetUuid);
                if (initiator == null) {
                    TradeOfferStore.get().closeSession(session.initiatorUuid);
                    continue;
                }
                if (target == null || target.level() != initiator.level()
                        || target.distanceToSqr(initiator) > rSq) {
                    TradeOfferStore.get().closeSession(session.initiatorUuid);
                    NetworkManager.sendToPlayer(initiator, HatsNetwork.TRADE_PARTNER_LEFT,
                        PacketTradePartnerLeft.encode(session.targetUuid));
                }
            }
        }
    }

    private static void onPlayerQuit(ServerPlayer player)
    {
        java.util.UUID uuid = player.getUUID();
        TradeOfferStore.get().closeSession(uuid);
        for (TradeOffer offer : TradeOfferStore.get().removeAllInvolving(uuid)) {
            ServerPlayer other = player.getServer().getPlayerList().getPlayer(
                offer.senderUuid.equals(uuid) ? offer.receiverUuid : offer.senderUuid);
            if (other != null) {
                NetworkManager.sendToPlayer(other, HatsNetwork.TRADE_OFFER_REVOKED,
                    PacketTradeOfferRevoked.encode(offer.offerId));
            }
        }
    }

    private static void onServerLevelTick(ServerLevel level)
    {
        if (!HatsMod.getConfig().preventUndeadFire) return;
        if (!level.isDay()) return;

        HatsSavedData data = HatsSavedData.get(level);
        for (UUID uuid : data.getAllEntityHats().keySet()) {
            Entity ent = level.getEntity(uuid);
            if (!(ent instanceof LivingEntity living)) continue;
            if (!living.getType().is(EntityTypeTags.UNDEAD)) continue;
            if (!living.isOnFire()) continue;
            if (living.isInWaterRainOrBubble() || living.isInPowderSnow) continue;
            net.minecraft.core.BlockPos eyePos = net.minecraft.core.BlockPos.containing(
                living.getX(), living.getEyeY(), living.getZ());
            if (!level.canSeeSky(eyePos)) continue;
            living.clearFire();
        }
    }

    private static void onLevelLoad(ServerLevel level)
    {
    }

    private static EventResult onEntityDeath(LivingEntity entity, DamageSource source)
    {
        if (entity.level().isClientSide) return EventResult.pass();
        if (!(entity.level() instanceof ServerLevel serverLevel)) return EventResult.pass();

        if (entity instanceof ServerPlayer) return EventResult.pass();

        HatsSavedData data = HatsSavedData.get(serverLevel);
        HatPart hat = data.getEntityHat(entity.getUUID());

        if (hat == null) return EventResult.pass();

        data.setEntityHat(entity.getUUID(), null);

        if (source.getEntity() instanceof ServerPlayer player) {
            onPlayerPickupHat(player, hat);
            return EventResult.pass();
        }

        if (HatsMod.getConfig().mobHatTakeover && source.getEntity() instanceof Mob killer) {
            HatPart killerHat = data.getEntityHat(killer.getUUID());
            if (killerHat == null) {
                data.setEntityHat(killer.getUUID(), hat);
                NetworkHelper.broadcastEntityHat(killer);
                return EventResult.pass();
            }
        }

        return EventResult.pass();
    }

    private static void onPlayerRespawn(ServerPlayer player)
    {
        HatsSavedData data = HatsSavedData.get(player.serverLevel());
        PlayerHatData playerData = data.getPlayer(player.getUUID());
        if (playerData == null) return;

        HatPart equipped = playerData.equippedHat;
        if (equipped != null) {
            data.setEntityHat(player.getUUID(), equipped);
            NetworkHelper.broadcastEntityHat(player);
        }
    }

    private static void onPlayerJoin(ServerPlayer player)
    {
        HatsSavedData data = HatsSavedData.get(player.serverLevel());
        PlayerHatData playerData = data.getOrCreatePlayer(player.getUUID());

        NetworkHelper.sendManifest(player);
        NetworkHelper.syncInventory(player, data);

        HatPart equipped = playerData.equippedHat;
        if (equipped != null) {
            data.setEntityHat(player.getUUID(), equipped);
            NetworkHelper.broadcastEntityHat(player);
        }

        NetworkHelper.syncAllEntityHats(player, data);
    }

    private static EventResult onEntityAdd(Entity entity, Level level)
    {
        if (level.isClientSide()) return EventResult.pass();
        if (!(level instanceof ServerLevel serverLevel)) return EventResult.pass();
        if (!(entity instanceof Mob mob)) return EventResult.pass();
        if (entity instanceof ServerPlayer) return EventResult.pass();

        onMobSpawn(mob, serverLevel);
        return EventResult.pass();
    }

    public static void onMobSpawn(Mob mob, ServerLevel level)
    {
        if (HatRegistry.getAllPools().isEmpty()) return;

        HatsSavedData data = HatsSavedData.get(level);
        String registryName = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                .getKey(mob.getType()).toString();

        data.assignMobHat(mob.getUUID(), registryName);

        HatPart hat = data.getEntityHat(mob.getUUID());
        if (hat != null) {
            NetworkHelper.broadcastEntityHat(mob);
        }
    }

    public static void onPlayerPickupHat(ServerPlayer player, HatPart hat)
    {
        HatsSavedData data = HatsSavedData.get(player.serverLevel());

        boolean isNewHat = !data.hasHat(player.getUUID(), hat.name);

        java.util.Set<String> ownedAccessories = new java.util.HashSet<>();
        me.guivnf.mods.hats.common.world.PlayerHatData playerData = data.getPlayer(player.getUUID());
        if (playerData != null) {
            for (HatPart inv : playerData.inventory) {
                if (inv.name.equals(hat.name)) {
                    for (HatPart acc : inv.accessories) ownedAccessories.add(acc.name);
                    break;
                }
            }
        }

        data.addHatToInventory(player.getUUID(), hat);
        NetworkHelper.syncInventory(player, data);

        if (isNewHat) {
            NetworkHelper.sendNewHatToast(player, hat, false);
            if (HatsMod.getConfig().hatUnlockAnnouncement) {
                broadcastHatUnlock(player, hat);
            }
        } else {
            for (HatPart incomingAcc : hat.accessories) {
                if (!ownedAccessories.contains(incomingAcc.name)) {
                    NetworkHelper.sendNewHatToast(player, incomingAcc, true);
                }
            }
        }
    }

    private static void broadcastHatUnlock(ServerPlayer player, HatPart hat)
    {
        me.guivnf.mods.hats.common.hat.HatDefinition def =
            me.guivnf.mods.hats.common.hat.HatRegistry.get(hat.getRegistryKey());
        if (def == null) return;

        me.guivnf.mods.hats.common.hat.HatRarity rarity = def.getRarity();

        net.minecraft.network.chat.MutableComponent tooltip =
            net.minecraft.network.chat.Component.literal(hat.name).withStyle(rarity.colour);
        tooltip.append(net.minecraft.network.chat.Component.literal("\n"));
        tooltip.append(net.minecraft.network.chat.Component.literal(
                rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase(java.util.Locale.ROOT))
            .withStyle(rarity.colour, net.minecraft.ChatFormatting.ITALIC));
        if (def.meta.contributorUuid != null) {
            tooltip.append(net.minecraft.network.chat.Component.literal("\n\nContributor hat")
                .withStyle(net.minecraft.ChatFormatting.AQUA, net.minecraft.ChatFormatting.ITALIC));
        }

        StringBuilder insertion = new StringBuilder("hats:model:").append(hat.getRegistryKey());
        boolean firstAcc = true;
        for (HatPart acc : hat.accessories) {
            if (!acc.isShowing) continue;
            insertion.append(firstAcc ? "|" : ",").append(acc.name);
            firstAcc = false;
        }
        String insertionStr = insertion.toString();

        net.minecraft.network.chat.MutableComponent hatName =
            net.minecraft.network.chat.Component.literal(hat.name)
                .withStyle(s -> s.withColor(rarity.colour)
                    .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                        net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, tooltip))
                    .withInsertion(insertionStr));

        net.minecraft.network.chat.MutableComponent msg =
            net.minecraft.network.chat.Component.literal("")
                .append(player.getDisplayName())
                .append(net.minecraft.network.chat.Component.literal(" unlocked "))
                .append(hatName)
                .append(net.minecraft.network.chat.Component.literal("!"));

        player.server.getPlayerList().broadcastSystemMessage(msg, false);
    }
}
