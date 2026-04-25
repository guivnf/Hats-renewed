package me.guivnf.mods.hats.common.hat.placement;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class HatPlacementRegistry
{
    private static final Map<EntityType<?>, HatPlacementInfo> REGISTRY = new HashMap<>();

    private static final Map<ResourceLocation, HatPlacementInfo> OVERRIDES = new HashMap<>();

    private static final Map<EntityType<?>, HatPlacementInfo> RESOLVED = new HashMap<>();


    public static void register(EntityType<?> type, HatPlacementInfo info)
    {
        REGISTRY.put(type, info);
    }

    public static void registerOverride(ResourceLocation id, HatPlacementInfo info)
    {
        OVERRIDES.put(id, info);
        RESOLVED.clear();
    }

    public static void registerOverride(EntityType<?> type, HatPlacementInfo info)
    {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id != null) registerOverride(id, info);
    }

    public static void removeOverride(EntityType<?> type)
    {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id != null) {
            OVERRIDES.remove(id);
            RESOLVED.clear();
        }
    }

    public static boolean hasOverride(EntityType<?> type)
    {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id != null && OVERRIDES.containsKey(id);
    }


    public static HatPlacementInfo get(LivingEntity entity)
    {
        EntityType<?> type = entity.getType();

        HatPlacementInfo cached = RESOLVED.get(type);
        if (cached != null) return cached;

        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        if (id != null) {
            HatPlacementInfo override = OVERRIDES.get(id);
            if (override != null) {
                RESOLVED.put(type, override);
                return override;
            }
        }

        HatPlacementInfo registered = REGISTRY.get(type);
        HatPlacementInfo result = registered != null ? registered : HatPlacementInfo.DEFAULT;
        RESOLVED.put(type, result);
        return result;
    }

    public static boolean isBoss(LivingEntity entity)
    {
        return get(entity).isBoss;
    }


    public static Set<Map.Entry<ResourceLocation, HatPlacementInfo>> getOverrideEntries()
    {
        return OVERRIDES.entrySet();
    }

    public static void getOverrides(Map<ResourceLocation, HatPlacementInfo> out)
    {
        out.putAll(OVERRIDES);
    }


    public static void registerVanilla()
    {
        EntityType<?>[] normal = {
            net.minecraft.world.entity.EntityType.ZOMBIE,
            net.minecraft.world.entity.EntityType.ZOMBIE_VILLAGER,
            net.minecraft.world.entity.EntityType.HUSK,
            net.minecraft.world.entity.EntityType.DROWNED,
            net.minecraft.world.entity.EntityType.SKELETON,
            net.minecraft.world.entity.EntityType.STRAY,
            net.minecraft.world.entity.EntityType.WITHER_SKELETON,
            net.minecraft.world.entity.EntityType.CREEPER,
            net.minecraft.world.entity.EntityType.SPIDER,
            net.minecraft.world.entity.EntityType.CAVE_SPIDER,
            net.minecraft.world.entity.EntityType.ENDERMAN,
            net.minecraft.world.entity.EntityType.BLAZE,
            net.minecraft.world.entity.EntityType.GHAST,
            net.minecraft.world.entity.EntityType.VILLAGER,
            net.minecraft.world.entity.EntityType.WANDERING_TRADER,
            net.minecraft.world.entity.EntityType.PILLAGER,
            net.minecraft.world.entity.EntityType.VINDICATOR,
            net.minecraft.world.entity.EntityType.EVOKER,
            net.minecraft.world.entity.EntityType.ILLUSIONER,
            net.minecraft.world.entity.EntityType.WITCH,
            net.minecraft.world.entity.EntityType.COW,
            net.minecraft.world.entity.EntityType.MOOSHROOM,
            net.minecraft.world.entity.EntityType.PIG,
            net.minecraft.world.entity.EntityType.SHEEP,
            net.minecraft.world.entity.EntityType.CHICKEN,
            net.minecraft.world.entity.EntityType.RABBIT,
            net.minecraft.world.entity.EntityType.WOLF,
            net.minecraft.world.entity.EntityType.CAT,
            net.minecraft.world.entity.EntityType.OCELOT,
            net.minecraft.world.entity.EntityType.FOX,
            net.minecraft.world.entity.EntityType.PANDA,
            net.minecraft.world.entity.EntityType.PARROT,
            net.minecraft.world.entity.EntityType.TURTLE,
            net.minecraft.world.entity.EntityType.DOLPHIN,
            net.minecraft.world.entity.EntityType.COD,
            net.minecraft.world.entity.EntityType.SALMON,
            net.minecraft.world.entity.EntityType.TROPICAL_FISH,
            net.minecraft.world.entity.EntityType.PUFFERFISH,
            net.minecraft.world.entity.EntityType.PHANTOM,
            net.minecraft.world.entity.EntityType.BAT,
            net.minecraft.world.entity.EntityType.SLIME,
            net.minecraft.world.entity.EntityType.MAGMA_CUBE,
            net.minecraft.world.entity.EntityType.SILVERFISH,
            net.minecraft.world.entity.EntityType.ENDERMITE,
            net.minecraft.world.entity.EntityType.SHULKER,
            net.minecraft.world.entity.EntityType.GUARDIAN,
            net.minecraft.world.entity.EntityType.PIGLIN,
            net.minecraft.world.entity.EntityType.PIGLIN_BRUTE,
            net.minecraft.world.entity.EntityType.ZOMBIFIED_PIGLIN,
            net.minecraft.world.entity.EntityType.HOGLIN,
            net.minecraft.world.entity.EntityType.ZOGLIN,
            net.minecraft.world.entity.EntityType.STRIDER,
            net.minecraft.world.entity.EntityType.GOAT,
            net.minecraft.world.entity.EntityType.AXOLOTL,
            net.minecraft.world.entity.EntityType.GLOW_SQUID,
            net.minecraft.world.entity.EntityType.FROG,
            net.minecraft.world.entity.EntityType.TADPOLE,
            net.minecraft.world.entity.EntityType.ALLAY,
            net.minecraft.world.entity.EntityType.CAMEL,
            net.minecraft.world.entity.EntityType.SNIFFER
        };

        for (EntityType<?> type : normal) {
            REGISTRY.put(type, HatPlacementInfo.DEFAULT);
        }

        register(net.minecraft.world.entity.EntityType.HORSE,
            HatPlacementInfo.builder().scale(1.2f).build());
        register(net.minecraft.world.entity.EntityType.DONKEY,
            HatPlacementInfo.builder().scale(1.1f).build());
        register(net.minecraft.world.entity.EntityType.MULE,
            HatPlacementInfo.builder().scale(1.1f).build());
        register(net.minecraft.world.entity.EntityType.LLAMA,
            HatPlacementInfo.builder().scale(1.1f).build());
        register(net.minecraft.world.entity.EntityType.TRADER_LLAMA,
            HatPlacementInfo.builder().scale(1.1f).build());
        register(net.minecraft.world.entity.EntityType.POLAR_BEAR,
            HatPlacementInfo.builder().scale(1.2f).build());
        register(net.minecraft.world.entity.EntityType.IRON_GOLEM,
            HatPlacementInfo.builder().scale(1.5f).build());
        register(net.minecraft.world.entity.EntityType.SNOW_GOLEM,
            HatPlacementInfo.builder().scale(1.1f).build());
        register(net.minecraft.world.entity.EntityType.BEE,
            HatPlacementInfo.builder().scale(0.9f).build());
        register(net.minecraft.world.entity.EntityType.RABBIT,
            HatPlacementInfo.builder().scale(0.8f).build());

        register(net.minecraft.world.entity.EntityType.ELDER_GUARDIAN,
            HatPlacementInfo.builder().scale(1.5f).boss().build());
        register(net.minecraft.world.entity.EntityType.WITHER,
            HatPlacementInfo.builder().scale(1.5f).boss().build());
        register(net.minecraft.world.entity.EntityType.ENDER_DRAGON,
            HatPlacementInfo.builder().scale(4f).boss().build());
        register(net.minecraft.world.entity.EntityType.RAVAGER,
            HatPlacementInfo.builder().scale(1.8f).boss().build());

        register(net.minecraft.world.entity.EntityType.PLAYER,
            HatPlacementInfo.DEFAULT);
    }
}
