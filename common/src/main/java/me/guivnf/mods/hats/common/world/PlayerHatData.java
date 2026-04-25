package me.guivnf.mods.hats.common.world;

import me.guivnf.mods.hats.common.hat.HatDefinition;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.HatPool;
import me.guivnf.mods.hats.common.hat.HatRegistry;
import me.guivnf.mods.hats.common.hat.HatRarity;
import me.guivnf.mods.hats.HatsMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class PlayerHatData
{
    public final UUID owner;
    public int tokens = 0;

    @Nullable
    public HatPart equippedHat;

    public final List<HatPart> inventory = new ArrayList<>();

    public PlayerHatData(UUID owner)
    {
        this.owner = owner;
    }

    public boolean hasHat(String name)
    {
        for (HatPart part : inventory) {
            if (part.name.equals(name) && part.count > 0) return true;
        }
        return false;
    }

    public void addHat(HatPart incoming)
    {
        for (HatPart existing : inventory) {
            if (existing.name.equals(incoming.name)) {
                existing.count += incoming.count;
                existing.isNew = true;
                for (HatPart incomingAcc : incoming.accessories) {
                    if (!existing.hasAccessory(incomingAcc.name)) {
                        HatPart accCopy = new HatPart(incomingAcc.name);
                        accCopy.isShowing = false;
                        existing.accessories.add(accCopy);
                    }
                }
                return;
            }
        }
        for (HatPart acc : incoming.accessories) {
            acc.isShowing = false;
        }
        incoming.isNew = true;
        inventory.add(incoming);
    }

    public boolean removeOne(String name)
    {
        for (HatPart part : inventory) {
            if (part.name.equals(name) && part.count > 0) {
                part.count--;
                if (part.count <= 0) {
                    inventory.remove(part);
                }
                return true;
            }
        }
        return false;
    }

    @Nullable
    public HatPart takeRandom(Random rand)
    {
        List<HatPart> available = new ArrayList<>();
        for (HatPart part : inventory) {
            if (part.count > 0) available.add(part);
        }
        if (available.isEmpty()) return null;

        HatPart chosen = available.get(rand.nextInt(available.size()));
        HatPart result = chosen.copy();
        removeOne(chosen.name);
        result.count = 1;
        return result;
    }

    @Nullable
    public HatPart takeRandomNonFavourite(Random rand)
    {
        List<HatPart> available = new ArrayList<>();
        for (HatPart part : inventory) {
            if (part.count > 0 && !part.isFavourite) available.add(part);
        }
        if (available.isEmpty()) return null;

        HatPart chosen = available.get(rand.nextInt(available.size()));
        HatPart result = chosen.copy();
        removeOne(chosen.name);
        result.count = 1;
        return result;
    }

    @Nullable
    public HatPart takeSpecific(String name)
    {
        for (HatPart part : inventory) {
            if (part.name.equals(name) && part.count > 0 && !part.isFavourite) {
                HatPart result = part.copy();
                removeOne(name);
                result.count = 1;
                return result;
            }
        }
        return null;
    }

    public void equipHat(@Nullable HatPart hat)
    {
        equippedHat = hat;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner", owner.toString());
        tag.putInt("tokens", tokens);

        if (equippedHat != null) {
            tag.put("equipped", equippedHat.save());
        }

        ListTag inventoryTag = new ListTag();
        for (HatPart part : inventory) {
            inventoryTag.add(part.save());
        }
        tag.put("inventory", inventoryTag);

        return tag;
    }

    public static PlayerHatData load(CompoundTag tag)
    {
        UUID owner = UUID.fromString(tag.getString("owner"));
        PlayerHatData data = new PlayerHatData(owner);
        data.tokens = tag.getInt("tokens");

        if (tag.contains("equipped", Tag.TAG_COMPOUND)) {
            data.equippedHat = HatPart.load(tag.getCompound("equipped"));
        }

        if (tag.contains("inventory", Tag.TAG_LIST)) {
            ListTag inventoryTag = tag.getList("inventory", Tag.TAG_COMPOUND);
            for (int i = 0; i < inventoryTag.size(); i++) {
                data.inventory.add(HatPart.load(inventoryTag.getCompound(i)));
            }
        }

        return data;
    }
}
