package com.endlessbackpack.registry;

import com.endlessbackpack.EndlessBackpack;
import com.endlessbackpack.menu.BackpackMenu;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, EndlessBackpack.MODID);

    /**
     * A regular container menu type, but with the "extra data" factory so the server can
     * tell the client which inventory slot the opened backpack is sitting in.
     */
    public static final DeferredHolder<MenuType<?>, MenuType<BackpackMenu>> BACKPACK_MENU =
            MENUS.register("backpack", () -> IMenuTypeExtension.create(BackpackMenu::new));

    private ModMenus() {
    }
}
