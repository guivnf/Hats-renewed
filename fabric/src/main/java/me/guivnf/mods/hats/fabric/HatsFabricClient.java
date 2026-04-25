package me.guivnf.mods.hats.fabric;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.registry.HatsRegistries;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class HatsFabricClient
        implements ClientModInitializer
{
    @Override
    public void onInitializeClient()
    {
        HatsMod.clientInit();
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
            });
    }
}
