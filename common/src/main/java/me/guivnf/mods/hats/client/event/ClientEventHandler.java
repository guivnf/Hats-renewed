package me.guivnf.mods.hats.client.event;

import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.client.gui.HatsScreen;
import me.guivnf.mods.hats.client.render.HatTextureManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class ClientEventHandler
{
    public static KeyMapping KEY_OPEN_HATS;

    public static float hatMenuProgress     = 0f;
    public static float hatMenuProgressPrev = 0f;

    public static float guiYaw   = 0f;
    public static float guiPitch = 0f;
    public static float guiZoom  = 0f;
    public static float guiPanX  = 0f;
    public static float guiPanY  = 0f;

    private static CameraType savedCameraType = null;
    private static final float SPEED = 0.07f;

    public static void register()
    {
        KEY_OPEN_HATS = new KeyMapping("key.hats.open_menu", GLFW.GLFW_KEY_H, "key.categories.hats");
        KeyMappingRegistry.register(KEY_OPEN_HATS);

        ClientTickEvent.CLIENT_PRE.register(ClientEventHandler::onClientTick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> onDisconnect());
    }

    private static void onClientTick(Minecraft mc)
    {
        if (KEY_OPEN_HATS.consumeClick() && mc.screen == null) {
            HatsScreen.open();
        }

        hatMenuProgressPrev = hatMenuProgress;

        boolean isOpen = mc.screen instanceof HatsScreen;

        if (isOpen) {
            if (savedCameraType == null) {
                savedCameraType = mc.options.getCameraType();
                mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            }
            hatMenuProgress = Math.min(1f, hatMenuProgress + SPEED);
        } else {
            if (hatMenuProgress > 0f || savedCameraType != null) {
                hatMenuProgress = 0f;
                hatMenuProgressPrev = 0f;
                if (savedCameraType != null) {
                    mc.options.setCameraType(savedCameraType);
                    savedCameraType = null;
                }
            }
        }
    }

    private static void onDisconnect()
    {
        hatMenuProgress     = 0f;
        hatMenuProgressPrev = 0f;
        guiYaw              = 0f;
        guiPitch            = 0f;
        guiZoom             = 0f;
        guiPanX             = 0f;
        guiPanY             = 0f;
        savedCameraType     = null;
        ClientHatCache.clear();
        HatTextureManager.clearAll();
        me.guivnf.mods.hats.client.trade.ClientTradeState.reset();
    }
}
