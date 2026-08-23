package com.storyengine.client;

import com.storyengine.StoryEngineMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Клиентская инициализация модуля кастомизации меню:
 * - при запуске клиента копирует исходные PNG-шаблоны в config/story_engine/menu/;
 * - при перезагрузке конфига (редактирование toml в живую или через экран Mods → Config)
 *   сбрасывает кэш загруженных текстур, чтобы правки подхватились.
 */
@Mod.EventBusSubscriber(modid = StoryEngineMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientInit {

    private ClientInit() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        MenuAssetsManager.copyDefaultsIfMissing();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (StoryEngineMod.MOD_ID.equals(event.getConfig().getModId())) {
            MenuAssetsManager.clearCache();
        }
    }
}
