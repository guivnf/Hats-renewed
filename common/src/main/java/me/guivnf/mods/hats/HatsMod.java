package me.guivnf.mods.hats;

import dev.architectury.platform.Platform;
import me.guivnf.mods.hats.common.config.HatsConfig;
import me.guivnf.mods.hats.common.config.HatsConfigFile;
import me.guivnf.mods.hats.common.event.ServerEventHandler;
import me.guivnf.mods.hats.common.hat.HatLoader;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementRegistry;
import me.guivnf.mods.hats.common.network.HatsNetwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class HatsMod
{
    public static final String MOD_ID = "hats";
    public static final String MOD_NAME = "Hats";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    public static Path hatsDirectory;
    public static Path configDirectory;

    private static HatsConfig config = new HatsConfig();

    public static HatsConfig getConfig()
    {
        return config;
    }

    public static void setConfig(HatsConfig cfg)
    {
        config = cfg;
    }

    public static void init()
    {
        hatsDirectory   = Platform.getConfigFolder().resolve("hats");
        configDirectory = Platform.getConfigFolder().resolve("hats");

        HatsConfigFile.loadOrCreate(Platform.getConfigFolder(), config);

        HatsNetwork.register();
        ServerEventHandler.register();

        dev.architectury.event.events.common.CommandRegistrationEvent.EVENT.register(
            (dispatcher, ctx, sel) ->
                me.guivnf.mods.hats.common.command.HatCommand.register(dispatcher));

        HatPlacementRegistry.registerVanilla();
        me.guivnf.mods.hats.common.hat.placement.HatPlacementJsonLoader.load();

        HatLoader.beginLoading();
    }

    public static void clientInit()
    {
        HatsNetwork.registerClient();
        me.guivnf.mods.hats.client.event.ClientEventHandler.register();
        dev.architectury.registry.client.level.entity.EntityRendererRegistry.register(
            me.guivnf.mods.hats.common.registry.HatsRegistries.HAT_ENTITY,
            me.guivnf.mods.hats.client.render.HatEntityRenderer::new);

    }

    public static void onLoadComplete()
    {
        HatLoader.awaitAndFinish();
        HatRegistry.buildPools();

        long accCount = HatRegistry.getAll().stream()
            .filter(me.guivnf.mods.hats.common.hat.HatDefinition::isAccessory).count();
        long hatsWithAccs = HatRegistry.getAll().stream()
            .filter(d -> !d.isAccessory() && !d.getAccessories().isEmpty()).count();
        LOGGER.info("Loaded {} hat(s) ({} accessories, {} main hats with accessories attached) across {} pack(s)",
            HatRegistry.hatCount(), accCount, hatsWithAccs, HatRegistry.packCount());
    }
}
