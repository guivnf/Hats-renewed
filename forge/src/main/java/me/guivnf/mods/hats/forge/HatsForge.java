package me.guivnf.mods.hats.forge;

import dev.architectury.platform.forge.EventBuses;
import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.registry.HatsRegistries;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(HatsMod.MOD_ID)
public class HatsForge
{
    public HatsForge()
    {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(HatsMod.MOD_ID, modBus);
        me.guivnf.mods.hats.common.registry.HatsRegistries.register();
        HatsMod.init();
        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onLoadComplete);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            modBus.addListener(this::onClientSetup);
            modBus.addListener(this::onRegisterRenderers);
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event)
    {
    }

    private void onClientSetup(FMLClientSetupEvent event)
    {
        HatsMod.clientInit();
        event.enqueueWork(() ->
            ItemProperties.register(
                HatsRegistries.HAT_LAUNCHER.get(),
                new ResourceLocation(HatsMod.MOD_ID, "offhand"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0f;
                    net.minecraft.world.item.ItemStack offhand = entity.getOffhandItem();
                    if (offhand == stack) return 1.0f;
                    net.minecraft.world.item.ItemStack mainhand = entity.getMainHandItem();
                    if (mainhand != stack && offhand.getItem() == stack.getItem()
                            && net.minecraft.world.item.ItemStack.isSameItemSameTags(offhand, stack)) return 1.0f;
                    return 0.0f;
                }));
    }

    @SuppressWarnings("unchecked")
    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(
            (net.minecraft.world.entity.EntityType<me.guivnf.mods.hats.common.entity.HatEntity>)
                me.guivnf.mods.hats.common.registry.HatsRegistries.HAT_ENTITY.get(),
            me.guivnf.mods.hats.client.render.HatEntityRenderer::new);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        HatsMod.onLoadComplete();
    }
}
