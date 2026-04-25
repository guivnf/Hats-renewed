package me.guivnf.mods.hats.common.hat;

import me.guivnf.mods.hats.HatsMod;

import java.util.*;

public class HatRegistry
{
    private static final LinkedHashMap<String, HatDefinition> HATS = new LinkedHashMap<>();
    private static final List<HatPack> PACKS = new ArrayList<>();
    private static final EnumMap<HatRarity, List<HatPool>> POOLS = new EnumMap<>(HatRarity.class);

    private static double[] rarityThresholds;

    public static synchronized void register(HatDefinition hat)
    {
        if (HatsMod.getConfig().disableContributorHats && hat.meta.contributorUuid != null) return;
        HATS.put(hat.getFullName(), hat);
        if (!PACKS.contains(hat.pack)) {
            PACKS.add(hat.pack);
        }
    }

    public static synchronized void buildPools()
    {
        POOLS.clear();
        for (HatRarity rarity : HatRarity.values()) {
            POOLS.put(rarity, new ArrayList<>());
        }

        Map<String, HatPool> namedPools = new LinkedHashMap<>();

        for (HatDefinition hat : HATS.values()) {
            if (hat.isAccessory()) continue;

            HatRarity rarity = hat.getRarity();
            String poolName = hat.meta.forcedPool != null ? hat.meta.forcedPool : hat.name;

            String poolKey = rarity.name() + ":" + poolName;
            HatPool pool = namedPools.computeIfAbsent(poolKey, k -> {
                HatPool newPool = new HatPool(poolName, rarity);
                POOLS.get(rarity).add(newPool);
                return newPool;
            });
            pool.hats.add(hat);
        }

        rebuildThresholds();
    }

    private static void rebuildThresholds()
    {
        Map<HatRarity, Integer> weights = HatsMod.getConfig().rarityWeights;
        int total = 0;
        for (HatRarity rarity : HatRarity.values()) {
            total += weights.getOrDefault(rarity, 0);
        }

        rarityThresholds = new double[HatRarity.values().length];
        double cumulative = 0;
        for (HatRarity rarity : HatRarity.values()) {
            cumulative += (double) weights.getOrDefault(rarity, 0) / total;
            rarityThresholds[rarity.ordinal()] = cumulative;
        }
    }

    public static HatRarity getRarityForChance(double chance)
    {
        if (rarityThresholds == null) rebuildThresholds();

        HatRarity[] values = HatRarity.values();
        for (int i = 0; i < values.length; i++) {
            if (chance <= rarityThresholds[i]) return values[i];
        }
        return HatRarity.LEGENDARY;
    }

    public static HatDefinition get(String name)
    {
        return HATS.get(name);
    }

    public static Collection<HatDefinition> getAll()
    {
        return HATS.values();
    }

    public static List<HatPool> getPoolsForRarity(HatRarity rarity)
    {
        return POOLS.getOrDefault(rarity, Collections.emptyList());
    }

    public static List<HatPool> getAllPools()
    {
        List<HatPool> all = new ArrayList<>();
        for (List<HatPool> pools : POOLS.values()) {
            all.addAll(pools);
        }
        return all;
    }

    public static int hatCount()
    {
        return HATS.size();
    }

    public static int packCount()
    {
        return PACKS.size();
    }

    public static synchronized void clear()
    {
        HATS.clear();
        PACKS.clear();
        POOLS.clear();
        rarityThresholds = null;
    }
}
