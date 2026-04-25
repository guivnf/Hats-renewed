package me.guivnf.mods.hats.common.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.network.NetworkHelper;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import me.guivnf.mods.hats.common.world.PlayerHatData;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public final class HatCommand
{
    private static final SimpleCommandExceptionType UNKNOWN_HAT =
        new SimpleCommandExceptionType(Component.literal("Unknown hat"));
    private static final SimpleCommandExceptionType UNKNOWN_ACCESSORY =
        new SimpleCommandExceptionType(Component.literal("Unknown accessory"));

    private HatCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("hats")
            .requires(src -> src.hasPermission(2))
            .then(Commands.literal("give")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.literal("all")
                        .executes(HatCommand::giveAll))
                    .then(Commands.literal("random")
                        .executes(HatCommand::giveRandom))
                    .then(Commands.literal("hat")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(suggestMainHatNames())
                            .executes(HatCommand::giveHat)))
                    .then(Commands.literal("accessory")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(suggestAccessoryNames())
                            .executes(HatCommand::giveAccessory)))))
            .then(Commands.literal("remove")
                .then(Commands.argument("targets", EntityArgument.entities())
                    .then(Commands.literal("all")
                        .executes(HatCommand::removeAll))
                    .then(Commands.literal("hat")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(suggestMainHatNames())
                            .executes(HatCommand::removeHat)))
                    .then(Commands.literal("accessory")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .suggests(suggestAccessoryNames())
                            .executes(HatCommand::removeAccessory))))));
    }

    private static SuggestionProvider<CommandSourceStack> suggestMainHatNames()
    {
        return (ctx, builder) -> suggest(builder, mainHatNames());
    }

    private static SuggestionProvider<CommandSourceStack> suggestAccessoryNames()
    {
        return (ctx, builder) -> suggest(builder, accessoryNames());
    }

    private static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, Collection<String> names)
    {
        return SharedSuggestionProvider.suggest(names.stream().map(n -> "\"" + n + "\""), builder);
    }

    private static List<String> mainHatNames()
    {
        List<String> out = new ArrayList<>();
        for (HatDefinition def : HatRegistry.getAll()) {
            if (!def.isAccessory()) out.add(def.name);
        }
        return out;
    }

    private static List<String> accessoryNames()
    {
        List<String> out = new ArrayList<>();
        for (HatDefinition def : HatRegistry.getAll()) {
            if (def.isAccessory()) out.add(def.name);
        }
        return out;
    }

    private static int giveAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int affected = 0;
        for (Entity e : targets) {
            if (!(e instanceof ServerPlayer player)) continue;
            HatsSavedData data = HatsSavedData.get(player.serverLevel());
            for (HatDefinition def : HatRegistry.getAll()) {
                if (def.isAccessory()) continue;
                HatPart part = def.asHatPart(1);
                part.isShowing = false;
                data.addHatToInventory(player.getUUID(), part);
            }
            NetworkHelper.syncInventory(player, data);
            affected++;
        }
        final int count = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Gave all hats to " + count + " player(s)")
            .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static int giveRandom(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        List<HatDefinition> pool = new ArrayList<>();
        for (HatDefinition def : HatRegistry.getAll()) if (!def.isAccessory()) pool.add(def);
        if (pool.isEmpty()) return 0;

        Random rand = new Random();
        int affected = 0;
        for (Entity e : targets) {
            HatDefinition chosen = pool.get(rand.nextInt(pool.size()));
            if (giveHatToEntity(e, chosen)) affected++;
        }
        final int count = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Gave random hat to " + count + " target(s)")
            .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static int giveHat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        String name = StringArgumentType.getString(ctx, "name");
        HatDefinition def = findMainHat(name);
        if (def == null) throw UNKNOWN_HAT.create();

        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int affected = 0;
        for (Entity e : targets) if (giveHatToEntity(e, def)) affected++;

        final int count = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Gave '" + def.name + "' to " + count + " target(s)")
            .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static int giveAccessory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        String accName = StringArgumentType.getString(ctx, "name");
        HatDefinition accDef = findAccessory(accName);
        if (accDef == null) throw UNKNOWN_ACCESSORY.create();

        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int affected = 0;
        for (Entity e : targets) {
            if (!(e instanceof ServerPlayer player)) continue;
            HatsSavedData data = HatsSavedData.get(player.serverLevel());
            PlayerHatData pd = data.getOrCreatePlayer(player.getUUID());
            HatPart owning = findOwningHat(pd, accDef);
            if (owning == null) {
                HatDefinition parentHat = findMainHat(accDef.meta.accessoryFor);
                if (parentHat == null) continue;
                HatPart parentPart = parentHat.asHatPart(1);
                parentPart.isShowing = false;
                for (HatPart acc : parentPart.accessories) acc.isShowing = false;
                data.addHatToInventory(player.getUUID(), parentPart);
                owning = findOwningHat(pd, accDef);
                if (owning == null) continue;
            }
            if (!owning.hasAccessory(accDef.name)) {
                HatPart accPart = new HatPart(accDef.name);
                accPart.registryKey = accDef.getFullName();
                accPart.isShowing = false;
                owning.accessories.add(accPart);
            }
            NetworkHelper.syncInventory(player, data);
            affected++;
        }
        final int count = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Gave accessory '" + accDef.name + "' to " + count + " player(s)")
            .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static int removeAll(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int affected = 0;
        for (Entity e : targets) {
            if (e instanceof ServerPlayer player) {
                HatsSavedData data = HatsSavedData.get(player.serverLevel());
                PlayerHatData pd = data.getPlayer(player.getUUID());
                if (pd != null) {
                    pd.inventory.clear();
                    pd.equippedHat = null;
                    data.setEntityHat(player.getUUID(), null);
                    NetworkHelper.syncInventory(player, data);
                    NetworkHelper.broadcastEntityHat(player);
                }
                affected++;
            } else if (e instanceof LivingEntity living && e.level() instanceof ServerLevel sl) {
                HatsSavedData data = HatsSavedData.get(sl);
                data.setEntityHat(living.getUUID(), null);
                NetworkHelper.broadcastEntityHat(living);
                affected++;
            }
        }
        final int count = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Cleared hats from " + count + " target(s)")
            .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static int removeHat(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        String name = StringArgumentType.getString(ctx, "name");
        HatDefinition def = findMainHat(name);
        if (def == null) throw UNKNOWN_HAT.create();

        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int affected = 0;
        for (Entity e : targets) {
            if (e instanceof ServerPlayer player) {
                HatsSavedData data = HatsSavedData.get(player.serverLevel());
                if (data.removeOneFromInventory(player.getUUID(), def.name)) {
                    NetworkHelper.syncInventory(player, data);
                    affected++;
                }
            } else if (e instanceof LivingEntity living && e.level() instanceof ServerLevel sl) {
                HatsSavedData data = HatsSavedData.get(sl);
                HatPart worn = data.getEntityHat(living.getUUID());
                if (worn != null && worn.name.equals(def.name)) {
                    data.setEntityHat(living.getUUID(), null);
                    NetworkHelper.broadcastEntityHat(living);
                    affected++;
                }
            }
        }
        final int count = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Removed '" + def.name + "' from " + count + " target(s)")
            .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static int removeAccessory(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        String accName = StringArgumentType.getString(ctx, "name");
        HatDefinition accDef = findAccessory(accName);
        if (accDef == null) throw UNKNOWN_ACCESSORY.create();

        Collection<? extends Entity> targets = EntityArgument.getEntities(ctx, "targets");
        int affected = 0;
        for (Entity e : targets) {
            if (!(e instanceof ServerPlayer player)) continue;
            HatsSavedData data = HatsSavedData.get(player.serverLevel());
            PlayerHatData pd = data.getPlayer(player.getUUID());
            if (pd == null) continue;
            HatPart owning = findOwningHat(pd, accDef);
            if (owning == null) continue;
            if (owning.accessories.removeIf(a -> a.name.equals(accDef.name))) {
                NetworkHelper.syncInventory(player, data);
                affected++;
            }
        }
        final int count = affected;
        ctx.getSource().sendSuccess(() -> Component.literal("Removed accessory '" + accDef.name + "' from " + count + " player(s)")
            .withStyle(ChatFormatting.GREEN), true);
        return affected;
    }

    private static boolean giveHatToEntity(Entity e, HatDefinition def)
    {
        if (e instanceof ServerPlayer player) {
            HatsSavedData data = HatsSavedData.get(player.serverLevel());
            HatPart part = def.asHatPart(1);
            part.isShowing = false;
            for (HatPart acc : part.accessories) acc.isShowing = false;
            data.addHatToInventory(player.getUUID(), part);
            NetworkHelper.syncInventory(player, data);
            return true;
        }
        if (e instanceof LivingEntity living && e.level() instanceof ServerLevel sl) {
            HatsSavedData data = HatsSavedData.get(sl);
            HatPart part = def.asHatPart(1);
            part.isShowing = true;
            data.setEntityHat(living.getUUID(), part);
            NetworkHelper.broadcastEntityHat(living);
            return true;
        }
        return false;
    }

    private static HatDefinition findMainHat(String name)
    {
        for (HatDefinition def : HatRegistry.getAll()) {
            if (!def.isAccessory() && def.name.equalsIgnoreCase(name)) return def;
        }
        return null;
    }

    private static HatDefinition findAccessory(String name)
    {
        for (HatDefinition def : HatRegistry.getAll()) {
            if (def.isAccessory() && def.name.equalsIgnoreCase(name)) return def;
        }
        return null;
    }

    private static HatPart findOwningHat(PlayerHatData pd, HatDefinition accDef)
    {
        if (accDef.meta.accessoryFor == null) return null;
        for (HatPart inv : pd.inventory) {
            if (inv.name.equals(accDef.meta.accessoryFor)) return inv;
        }
        return null;
    }
}
