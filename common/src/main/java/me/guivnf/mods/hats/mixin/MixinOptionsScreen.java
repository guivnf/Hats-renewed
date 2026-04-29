package me.guivnf.mods.hats.mixin;

import me.guivnf.mods.hats.client.gui.HatPlacementScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class MixinOptionsScreen extends Screen
{
    protected MixinOptionsScreen() { super(Component.empty()); }

    @Unique
    private Button hats$button;

    @Inject(method = "init", at = @At("TAIL"))
    private void hats$addButton(CallbackInfo ci)
    {
        OptionsScreen self = (OptionsScreen)(Object) this;

        // Vanilla 1.21.1 places "Done" at (width/2 - 100, height - 27) via HeaderAndFooterLayout.
        // Anchor our 60px-wide button just to its left with a 4px gap.
        hats$button = Button.builder(
            Component.literal("Hats"),
            b -> minecraft.setScreen(new HatPlacementScreen(self)))
            .bounds(width / 2 - 164, height - 27, 60, 20)
            .build();
        addRenderableWidget(hats$button);
    }

    // Screen.resize in 1.21.1 calls repositionElements rather than re-running init,
    // so the button must be re-anchored here when the window is resized.
    @Inject(method = "repositionElements", at = @At("TAIL"))
    private void hats$reposition(CallbackInfo ci)
    {
        if (hats$button != null) {
            hats$button.setX(width / 2 - 164);
            hats$button.setY(height - 27);
        }
    }
}
