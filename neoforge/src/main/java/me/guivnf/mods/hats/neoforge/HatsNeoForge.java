package me.guivnf.mods.hats.neoforge;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.registry.HatsRegistries;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(HatsMod.MOD_ID)
public class HatsNeoForge
{
    public HatsNeoForge(IEventBus modBus, ModContainer container, Dist dist)
    {
        HatsRegistries.register();
        HatsMod.init();

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onLoadComplete);

        if (dist == Dist.CLIENT) {
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
                ResourceLocation.fromNamespaceAndPath(HatsMod.MOD_ID, "offhand"),
                (stack, level, entity, seed) -> {
                    if (entity == null) return 0.0f;
                    ItemStack offhand = entity.getOffhandItem();
                    if (offhand == stack) return 1.0f;
                    ItemStack mainhand = entity.getMainHandItem();
                    if (mainhand != stack && offhand.getItem() == stack.getItem()
                            && ItemStack.isSameItemSameComponents(offhand, stack)) return 1.0f;
                    return 0.0f;
                }));
    }

    @SuppressWarnings("unchecked")
    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerEntityRenderer(
            (EntityType<me.guivnf.mods.hats.common.entity.HatEntity>)
                HatsRegistries.HAT_ENTITY.get(),
            me.guivnf.mods.hats.client.render.HatEntityRenderer::new);
    }

    private void onLoadComplete(FMLLoadCompleteEvent event)
    {
        HatsMod.onLoadComplete();
    }
}
