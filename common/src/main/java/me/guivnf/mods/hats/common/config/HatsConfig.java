package me.guivnf.mods.hats.common.config;

import me.guivnf.mods.hats.common.hat.HatRarity;

import java.util.*;

public class HatsConfig
{
    public String randSeed = "";

    public double hatChance = 0.10;

    public double bossHatChanceBonus = 0.10;

    public double bossRarityBonus = 0.20;

    public Map<String, Double> entityHatChanceOverrides = new HashMap<>();

    public List<String> entityIdBlacklist = new ArrayList<>();

    public java.util.List<String> disabledEntities = new java.util.ArrayList<>();

    public Map<HatRarity, Integer> rarityWeights = new EnumMap<>(Map.of(
        HatRarity.COMMON,    21,
        HatRarity.UNCOMMON,  13,
        HatRarity.RARE,       8,
        HatRarity.EPIC,       5,
        HatRarity.LEGENDARY,  3
    ));

    public Map<HatRarity, Double> rarityIndividual = new EnumMap<>(Map.of(
        HatRarity.COMMON,    0.21,
        HatRarity.UNCOMMON,  0.13,
        HatRarity.RARE,      0.08,
        HatRarity.EPIC,      0.05,
        HatRarity.LEGENDARY, 0.03
    ));

    public Map<HatRarity, Integer> tokensByRarity = new EnumMap<>(Map.of(
        HatRarity.COMMON,    10,
        HatRarity.UNCOMMON,  20,
        HatRarity.RARE,      40,
        HatRarity.EPIC,      70,
        HatRarity.LEGENDARY, 110
    ));

    public double accessoryCostMultiplier = 1.5;

    public double salesCostMultiplier = 0.10;

    public boolean mobHatTakeover = false;

    public int hatEntityLifespan = 6000;

    public boolean hatLauncherReplacesHat = true;

    public boolean hatLauncherKeepsHat = false;

    public double clientOnlyHatChance = 0.10;

    public int maxHatRendersPerFrame = 250;

    public boolean renderOnInvisible = false;

    public boolean disableClientOnlyWarning = false;

    public java.util.List<String> hatBlacklist = new java.util.ArrayList<>();

    public boolean preventUndeadFire = false;

    public boolean disableContributorHats = false;

    public boolean hatUnlockAnnouncement = false;

    public boolean displayHatUnlockToast = true;

    public Map<HatRarity, Double> rarityChances = new EnumMap<>(Map.of(
        HatRarity.COMMON,    0.10,
        HatRarity.UNCOMMON,  0.08,
        HatRarity.RARE,      0.05,
        HatRarity.EPIC,      0.03,
        HatRarity.LEGENDARY, 0.02
    ));
}
