package com.endlessbackpack.client;

import com.endlessbackpack.EndlessBackpack;
import com.endlessbackpack.menu.BackpackMenu;
import com.endlessbackpack.registry.ModMenus;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = EndlessBackpack.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {

    @SubscribeEvent
    public static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.BACKPACK_MENU.get(), BackpackScreen::new);
    }

    private ClientSetup() {
    }
}
