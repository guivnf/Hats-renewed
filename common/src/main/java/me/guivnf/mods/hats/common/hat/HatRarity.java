package me.guivnf.mods.hats.common.hat;

import net.minecraft.ChatFormatting;

public enum HatRarity
{
    COMMON(ChatFormatting.WHITE, 10),
    UNCOMMON(ChatFormatting.GREEN, 20),
    RARE(ChatFormatting.AQUA, 40),
    EPIC(ChatFormatting.LIGHT_PURPLE, 70),
    LEGENDARY(ChatFormatting.GOLD, 110);

    public final ChatFormatting colour;
    public final int defaultWorth;

    HatRarity(ChatFormatting colour, int defaultWorth)
    {
        this.colour = colour;
        this.defaultWorth = defaultWorth;
    }

    public static HatRarity fromString(String name)
    {
        for (HatRarity rarity : values()) {
            if (rarity.name().equalsIgnoreCase(name)) return rarity;
        }
        return null;
    }
}
