package me.guivnf.mods.hats.fabric;

import me.guivnf.mods.hats.HatsMod;
import net.fabricmc.api.ModInitializer;

public class HatsFabric
        implements ModInitializer
{
    @Override
    public void onInitialize()
    {
        me.guivnf.mods.hats.common.registry.HatsRegistries.register();
        HatsMod.init();
        HatsMod.onLoadComplete();
    }
}
