package com.endlessbackpack;

import com.endlessbackpack.registry.ModDataComponents;
import com.endlessbackpack.registry.ModItems;
import com.endlessbackpack.registry.ModMenus;

import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/**
 * Entry point of the Endless Backpack mod.
 *
 * <p>Everything the mod adds is a plain item plus a menu, so there is no block,
 * no block entity and no world generation involved. Backpack contents are stored
 * inside the item itself through a data component, which is what allows the
 * storage to grow far beyond anything a chest could hold.</p>
 */
@Mod(EndlessBackpack.MODID)
public class EndlessBackpack {

    public static final String MODID = "endlessbackpack";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EndlessBackpack(IEventBus modEventBus, ModContainer modContainer) {
        // Registries have to be hooked to the mod event bus before registration happens.
        ModDataComponents.DATA_COMPONENT_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        modEventBus.addListener(this::addCreativeTabItems);
    }

    /** Puts the backpack into the vanilla "Tools & Utilities" creative tab. */
    private void addCreativeTabItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.BACKPACK.get());
        }
    }
}
