package me.guivnf.mods.hats.mixin;

import me.guivnf.mods.hats.client.gui.HatPlacementScreen;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public abstract class MixinOptionsScreen extends Screen
{
    protected MixinOptionsScreen() { super(Component.empty()); }

    @Inject(method = "init", at = @At("TAIL"))
    private void hats$addButton(CallbackInfo ci)
    {
        OptionsScreen self = (OptionsScreen)(Object) this;

        AbstractWidget doneButton = null;
        for (var widget : children()) {
            if (!(widget instanceof Button btn)) continue;
            if (btn.getMessage().getString().equals(
                    net.minecraft.network.chat.Component.translatable("gui.done").getString())) {
                doneButton = btn;
                break;
            }
            if (doneButton == null || btn.getY() > doneButton.getY()) {
                doneButton = btn;
            }
        }

        int hatsY = doneButton != null ? doneButton.getY() : height - 27;
        int hatsX = doneButton != null ? doneButton.getX() - 64 : width / 2 - 164;

        addRenderableWidget(Button.builder(
            Component.literal("Hats"),
            b -> minecraft.setScreen(new HatPlacementScreen(self)))
            .bounds(hatsX, hatsY, 60, 20)
            .build());
    }
}
