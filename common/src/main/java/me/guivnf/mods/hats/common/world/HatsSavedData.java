package me.guivnf.mods.hats.common.world;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.config.HatsConfig;
import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatPool;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.hat.HatRarity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.Nullable;
import java.util.*;

public class HatsSavedData
        extends SavedData
{
    private static final String DATA_NAME = "hats_save";

    private final Map<UUID, PlayerHatData> playerData = new HashMap<>();
    private final Map<UUID, HatPart> entityHats = new HashMap<>();

    private static final Random RAND = new Random();

    public static HatsSavedData get(net.minecraft.world.level.Level level)
    {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getServer().overworld().getDataStorage()
                    .computeIfAbsent(HatsSavedData::load, HatsSavedData::new, DATA_NAME);
        }
        throw new IllegalStateException("HatsSavedData accessed on the wrong side");
    }

    public HatsConfig config()
    {
        return HatsMod.getConfig();
    }

    public PlayerHatData getOrCreatePlayer(UUID uuid)
    {
        boolean isNew = !playerData.containsKey(uuid);
        PlayerHatData data = playerData.computeIfAbsent(uuid, PlayerHatData::new);
        if (isNew) setDirty();
        return data;
    }

    @Nullable
    public PlayerHatData getPlayer(UUID uuid)
    {
        return playerData.get(uuid);
    }

    public boolean hasHat(UUID playerUuid, String hatName)
    {
        PlayerHatData data = playerData.get(playerUuid);
        return data != null && data.hasHat(hatName);
    }

    public void addHatToInventory(UUID playerUuid, HatPart hat)
    {
        getOrCreatePlayer(playerUuid).addHat(hat);
        setDirty();
    }

    public void equipHat(UUID playerUuid, HatPart hat)
    {
        getOrCreatePlayer(playerUuid).equipHat(hat);
        setDirty();
    }

    public void removeEquipped(UUID playerUuid)
    {
        PlayerHatData data = playerData.get(playerUuid);
        if (data != null) {
            data.equipHat(null);
            setDirty();
        }
    }

    public boolean removeOneFromInventory(UUID playerUuid, String hatName)
    {
        PlayerHatData data = playerData.get(playerUuid);
        if (data == null) return false;
        boolean removed = data.removeOne(hatName);
        if (removed) setDirty();
        return removed;
    }

    @Nullable
    public HatPart takeRandomHat(UUID playerUuid)
    {
        PlayerHatData data = playerData.get(playerUuid);
        if (data == null) return null;

        String seed = HatsMod.getConfig().randSeed;
        RAND.setSeed(Math.abs((seed + playerUuid).hashCode()) * 425480085L);
        HatPart taken = data.takeRandom(RAND);
        if (taken != null) setDirty();
        return taken;
    }

    @Nullable
    public HatPart takeRandomNonFavourite(UUID playerUuid)
    {
        PlayerHatData data = playerData.get(playerUuid);
        if (data == null) return null;

        String seed = HatsMod.getConfig().randSeed;
        RAND.setSeed(Math.abs((seed + playerUuid).hashCode()) * 425480085L);
        HatPart taken = data.takeRandomNonFavourite(RAND);
        if (taken != null) setDirty();
        return taken;
    }

    @Nullable
    public HatPart takeSpecificHat(UUID playerUuid, String hatName)
    {
        PlayerHatData data = playerData.get(playerUuid);
        if (data == null) return null;
        HatPart taken = data.takeSpecific(hatName);
        if (taken != null) setDirty();
        return taken;
    }

    public void setEntityHat(UUID entityUuid, @Nullable HatPart hat)
    {
        if (hat == null) {
            entityHats.remove(entityUuid);
        } else {
            entityHats.put(entityUuid, hat);
        }
        setDirty();
    }

    @Nullable
    public HatPart getEntityHat(UUID entityUuid)
    {
        return entityHats.get(entityUuid);
    }

    public Map<UUID, HatPart> getAllEntityHats()
    {
        return Collections.unmodifiableMap(entityHats);
    }

    public void assignMobHat(UUID entityUuid, String entityRegistryName)
    {
        if (entityHats.containsKey(entityUuid)) return;

        HatsConfig cfg = config();

        if (cfg.disabledEntities.contains(entityRegistryName)) return;

        RAND.setSeed(Math.abs((cfg.randSeed + entityUuid).hashCode()) * 425480085L);

        HatRarity rarity = rollRarity(cfg, entityRegistryName);
        if (rarity == null) return;

        List<HatPool> pools = HatRegistry.getPoolsForRarity(rarity);
        if (pools.isEmpty()) return;

        List<HatDefinition> candidates = new ArrayList<>();
        for (HatPool pool : pools) {
            for (HatDefinition def : pool.hats) {
                if (cfg.hatBlacklist.contains(def.name)) continue;
                if (cfg.disableContributorHats && def.meta.contributorUuid != null) continue;
                candidates.add(def);
            }
        }
        if (candidates.isEmpty()) return;

        HatDefinition def = candidates.get(RAND.nextInt(candidates.size()));
        HatPart hat = new HatPart(def.name);
        hat.isShowing = true;
        hat.count = 1;
        def.attachAccessoriesToPart(hat, entityUuid, cfg);

        entityHats.put(entityUuid, hat);
        setDirty();
    }

    @org.jetbrains.annotations.Nullable
    private HatRarity rollRarity(HatsConfig cfg, String entityRegistryName)
    {
        double rateScale = cfg.entityHatChanceOverrides.containsKey(entityRegistryName)
            ? cfg.entityHatChanceOverrides.get(entityRegistryName) / Math.max(cfg.hatChance, 1e-9)
            : 1.0;

        double roll = RAND.nextDouble();
        double cumulative = 0.0;
        for (HatRarity rarity : HatRarity.values()) {
            cumulative += cfg.rarityChances.getOrDefault(rarity, 0.0) * rateScale;
            if (roll < cumulative) return rarity;
        }
        return null;
    }

    @Override
    public CompoundTag save(CompoundTag tag)
    {
        ListTag players = new ListTag();
        for (PlayerHatData data : playerData.values()) {
            players.add(data.save());
        }
        tag.put("players", players);

        ListTag entityHatsList = new ListTag();
        for (Map.Entry<UUID, HatPart> entry : entityHats.entrySet()) {
            CompoundTag entry_tag = new CompoundTag();
            entry_tag.putUUID("uuid", entry.getKey());
            entry_tag.put("hat", entry.getValue().save());
            entityHatsList.add(entry_tag);
        }
        tag.put("entityHats", entityHatsList);

        return tag;
    }

    public static HatsSavedData load(CompoundTag tag)
    {
        HatsSavedData data = new HatsSavedData();

        if (tag.contains("players", Tag.TAG_LIST)) {
            ListTag players = tag.getList("players", Tag.TAG_COMPOUND);
            for (int i = 0; i < players.size(); i++) {
                PlayerHatData playerData = PlayerHatData.load(players.getCompound(i));
                data.playerData.put(playerData.owner, playerData);
            }
        }

        if (tag.contains("entityHats", Tag.TAG_LIST)) {
            ListTag entityHatsList = tag.getList("entityHats", Tag.TAG_COMPOUND);
            for (int i = 0; i < entityHatsList.size(); i++) {
                CompoundTag entry = entityHatsList.getCompound(i);
                UUID uuid = entry.getUUID("uuid");
                HatPart hat = HatPart.load(entry.getCompound("hat"));
                data.entityHats.put(uuid, hat);
            }
        }

        return data;
    }
}
