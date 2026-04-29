package me.guivnf.mods.hats.common.item;

import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.common.network.HatsNetwork;
import me.guivnf.mods.hats.common.network.packet.PacketLaunchHat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

public class HatLauncherItem
        extends Item
{
    private static final String HAT_PART_TAG = "hats_hatPart";

    public HatLauncherItem(Properties properties)
    {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                me.guivnf.mods.hats.client.gui.HatLaunchSelectScreen.open();
            } else {
                String pending = me.guivnf.mods.hats.client.gui.HatLaunchSelectScreen.pendingHatName;
                String toSend = pending != null ? pending : "";
                NetworkManager.sendToServer(HatsNetwork.LAUNCH_HAT, PacketLaunchHat.encode(toSend));
                me.guivnf.mods.hats.client.gui.HatLaunchSelectScreen.pendingHatName = null;
            }
        }

        return InteractionResultHolder.success(stack);
    }

    @Nullable
    public static me.guivnf.mods.hats.common.hat.HatPart getStoredHat(ItemStack stack)
    {
        net.minecraft.world.item.component.CustomData custom =
            stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (custom == null) return null;
        CompoundTag tag = custom.copyTag();
        if (!tag.contains(HAT_PART_TAG)) return null;
        return me.guivnf.mods.hats.common.hat.HatPart.load(tag.getCompound(HAT_PART_TAG));
    }

    public static void storeHat(ItemStack stack, @Nullable me.guivnf.mods.hats.common.hat.HatPart hat)
    {
        net.minecraft.world.item.component.CustomData.update(
            net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> {
                if (hat == null) tag.remove(HAT_PART_TAG);
                else tag.put(HAT_PART_TAG, hat.save());
            });
    }
}
