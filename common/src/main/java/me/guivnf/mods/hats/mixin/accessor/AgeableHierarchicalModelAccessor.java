package me.guivnf.mods.hats.mixin.accessor;

import net.minecraft.client.model.AgeableHierarchicalModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(AgeableHierarchicalModel.class)
public interface AgeableHierarchicalModelAccessor
{
    @Accessor("youngScaleFactor")
    float hats$getYoungScaleFactor();

    @Accessor("bodyYOffset")
    float hats$getBodyYOffset();
}
