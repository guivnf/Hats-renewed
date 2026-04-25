package me.guivnf.mods.hats.common.hat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HatPool
{
    public final String poolName;
    public final HatRarity rarity;
    public final List<HatDefinition> hats = new ArrayList<>();

    public HatPool(String poolName, HatRarity rarity)
    {
        this.poolName = poolName;
        this.rarity = rarity;
    }

    public HatDefinition getRandom(Random rand)
    {
        return hats.get(rand.nextInt(hats.size()));
    }

    public boolean isEmpty()
    {
        return hats.isEmpty();
    }
}
