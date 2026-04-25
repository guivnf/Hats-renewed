package me.guivnf.mods.hats.mixin.accessor;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AgeableListModel.class)
public interface AgeableListModelAccessor
{
    @Invoker("headParts")
    Iterable<ModelPart> hats$headParts();

    @Invoker("bodyParts")
    Iterable<ModelPart> hats$bodyParts();

    @Accessor("scaleHead")
    boolean hats$getScaleHead();

    @Accessor("babyYHeadOffset")
    float hats$getBabyYHeadOffset();

    @Accessor("babyZHeadOffset")
    float hats$getBabyZHeadOffset();

    @Accessor("babyHeadScale")
    float hats$getBabyHeadScale();

    @Accessor("babyBodyScale")
    float hats$getBabyBodyScale();

    @Accessor("bodyYOffset")
    float hats$getBodyYOffset();
}
