package me.guivnf.mods.hats.common.hat;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.config.HatsConfig;
import net.minecraft.ChatFormatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class HatDefinition
        implements Comparable<HatDefinition>
{
    public final String name;
    public final HatModelData modelData;
    public final byte[] textureBytes;
    public final HatMeta meta;
    public final HatPack pack;

    private final List<HatDefinition> accessories = new ArrayList<>();

    private HatRarity rarity;
    private int worth = -1;

    public HatDefinition(String name, HatModelData modelData, byte[] textureBytes, HatMeta meta, HatPack pack)
    {
        this.name = name;
        this.modelData = modelData;
        this.textureBytes = textureBytes;
        this.meta = meta;
        this.pack = pack;
    }

    public List<HatDefinition> getAccessories()
    {
        return accessories;
    }

    public void addAccessory(HatDefinition accessory)
    {
        accessories.add(accessory);
    }

    public HatRarity getRarity()
    {
        if (rarity == null) {
            if (meta.forcedRarity != null) {
                rarity = meta.forcedRarity;
            } else {
                Random rand = new Random(Math.abs((HatsMod.getConfig().randSeed + getFullName()).hashCode()) * 154041013L);
                rarity = HatRegistry.getRarityForChance(rand.nextDouble());
            }
        }
        return rarity;
    }

    public int getWorth()
    {
        if (worth < 0) {
            if (meta.forcedWorth >= 0) {
                worth = meta.forcedWorth;
            } else {
                int base = HatsMod.getConfig().tokensByRarity.getOrDefault(getRarity(), getRarity().defaultWorth);
                if (meta.accessoryFor != null) {
                    base = (int) Math.ceil(HatsMod.getConfig().accessoryCostMultiplier * base);
                }
                worth = base;
            }
        }
        return worth;
    }

    public String getDisplayName()
    {
        ChatFormatting prefix = meta.contributorUuid != null ? ChatFormatting.AQUA : getRarity().colour;
        return prefix.toString() + name;
    }

    public String getFullName()
    {
        StringBuilder sb = new StringBuilder();
        if (meta.accessoryFor != null) {
            sb.append(meta.accessoryFor).append(":");
        }
        if (meta.accessoryParent != null) {
            sb.append(meta.accessoryParent).append("|");
        }
        sb.append(name);
        return sb.toString();
    }

    public boolean isAccessory()
    {
        return meta.accessoryFor != null;
    }

    public void collectFullNames(HashSet<String> names)
    {
        names.add(getFullName());
        for (HatDefinition acc : accessories) {
            acc.collectFullNames(names);
        }
    }

    public HatDefinition findByName(String searchName)
    {
        if (name.equals(searchName)) return this;
        for (HatDefinition acc : accessories) {
            HatDefinition found = acc.findByName(searchName);
            if (found != null) return found;
        }
        return null;
    }

    public HatPart asHatPart(int count)
    {
        HatPart part = new HatPart(name);
        part.registryKey = getFullName();
        part.isShowing = true;
        part.count = count;
        for (HatDefinition acc : accessories) {
            HatPart child = acc.asHatPart(count);
            child.isShowing = false;
            part.accessories.add(child);
        }
        return part;
    }

    public void attachAccessories(List<HatDefinition> candidates)
    {
        attachAccessories(candidates, name);
    }

    private void attachAccessories(List<HatDefinition> candidates, String rootName)
    {
        List<HatDefinition> claimed = new ArrayList<>();
        for (HatDefinition candidate : candidates) {
            if (!rootName.equals(candidate.meta.accessoryFor)) continue;
            boolean parentMatch = candidate.meta.accessoryParent == null
                    || candidate.meta.accessoryParent.equals(name);
            if (parentMatch) {
                accessories.add(candidate);
                claimed.add(candidate);
            }
        }
        candidates.removeAll(claimed);

        if (meta.forcedWorth >= 0) worth = meta.forcedWorth;

        for (HatDefinition acc : accessories) {
            acc.attachAccessories(candidates, rootName);
        }
    }

    public void attachAccessoriesToPart(HatPart hatPart, UUID entityUuid, HatsConfig cfg)
    {
        List<HatDefinition> spawning = new ArrayList<>();
        Map<String, List<HatDefinition>> conflicts = new HashMap<>();

        Random rand = new Random();
        for (HatDefinition acc : accessories) {
            rand.setSeed(Math.abs((cfg.randSeed + entityUuid + acc.getFullName()).hashCode()) * 53579997854L);

            double chance = cfg.rarityIndividual.getOrDefault(acc.getRarity(), 0.05);
            if (rand.nextDouble() < chance) {
                spawning.add(acc);
                for (String layer : acc.meta.accessoryLayers) {
                    conflicts.computeIfAbsent(layer, k -> new ArrayList<>()).add(acc);
                }
            }
        }

        conflicts.entrySet().removeIf(entry -> entry.getValue().size() <= 1);

        for (List<HatDefinition> conflictList : conflicts.values()) {
            Random tieBreaker = new Random(entityUuid.hashCode());
            while (conflictList.size() > 1) {
                HatDefinition loser = conflictList.get(tieBreaker.nextInt(conflictList.size()));
                spawning.remove(loser);
                conflictList.remove(loser);
            }
        }

        for (HatDefinition acc : spawning) {
            HatPart accPart = new HatPart(acc.name);
            accPart.registryKey = acc.getFullName();
            accPart.isShowing = true;
            hatPart.accessories.add(accPart);
            acc.attachAccessoriesToPart(accPart, entityUuid, cfg);
        }
    }

    @Override
    public int compareTo(HatDefinition other)
    {
        return name.compareTo(other.name);
    }
}
