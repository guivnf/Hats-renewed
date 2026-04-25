package me.guivnf.mods.hats.common.hat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public class HatPart
{
    public String name;
    public String registryKey = null;
    public int count = 1;
    public boolean isFavourite = false;
    public boolean isNew = false;
    public boolean isShowing = false;
    public boolean enchanted = false;

    public float[] colour = {0f, 0f, 0f, 0f};

    public float[] hsb = {0f, 0f, 0f};

    public final List<HatPart> accessories = new ArrayList<>();

    public HatPart(String name)
    {
        this.name = name;
    }

    public String getRegistryKey()
    {
        return registryKey != null ? registryKey : name;
    }

    public CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        if (registryKey != null) tag.putString("registryKey", registryKey);
        tag.putInt("count", count);
        tag.putBoolean("favourite", isFavourite);
        tag.putBoolean("new", isNew);
        tag.putBoolean("showing", isShowing);
        tag.putBoolean("enchanted", enchanted);
        tag.putFloat("colR", colour[0]);
        tag.putFloat("colG", colour[1]);
        tag.putFloat("colB", colour[2]);
        tag.putFloat("colA", colour[3]);
        tag.putFloat("hsbH", hsb[0]);
        tag.putFloat("hsbS", hsb[1]);
        tag.putFloat("hsbB", hsb[2]);

        if (!accessories.isEmpty()) {
            ListTag list = new ListTag();
            for (HatPart acc : accessories) {
                list.add(acc.save());
            }
            tag.put("accessories", list);
        }

        return tag;
    }

    public static HatPart load(CompoundTag tag)
    {
        HatPart part = new HatPart(tag.getString("name"));
        if (tag.contains("registryKey")) part.registryKey = tag.getString("registryKey");
        part.count = tag.getInt("count");
        part.isFavourite = tag.getBoolean("favourite");
        part.isNew = tag.getBoolean("new");
        part.isShowing = tag.getBoolean("showing");
        part.enchanted = tag.getBoolean("enchanted");
        part.colour[0] = tag.getFloat("colR");
        part.colour[1] = tag.getFloat("colG");
        part.colour[2] = tag.getFloat("colB");
        part.colour[3] = tag.getFloat("colA");
        part.hsb[0] = tag.getFloat("hsbH");
        part.hsb[1] = tag.getFloat("hsbS");
        part.hsb[2] = tag.getFloat("hsbB");

        if (tag.contains("accessories", Tag.TAG_LIST)) {
            ListTag list = tag.getList("accessories", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                part.accessories.add(load(list.getCompound(i)));
            }
        }

        return part;
    }

    public HatPart copy()
    {
        HatPart copy = new HatPart(name);
        copy.registryKey = registryKey;
        copy.count = count;
        copy.isFavourite = isFavourite;
        copy.isNew = isNew;
        copy.isShowing = isShowing;
        copy.enchanted = enchanted;
        System.arraycopy(colour, 0, copy.colour, 0, colour.length);
        System.arraycopy(hsb, 0, copy.hsb, 0, hsb.length);
        for (HatPart acc : accessories) {
            copy.accessories.add(acc.copy());
        }
        return copy;
    }

    public boolean hasAccessory(String accessoryName)
    {
        for (HatPart acc : accessories) {
            if (acc.name.equals(accessoryName)) return true;
        }
        return false;
    }
}
