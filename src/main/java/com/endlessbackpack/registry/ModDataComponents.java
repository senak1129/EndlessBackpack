package com.endlessbackpack.registry;

import com.endlessbackpack.EndlessBackpack;
import com.endlessbackpack.storage.BackpackContents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, EndlessBackpack.MODID);

    /**
     * Holds the whole backpack inventory.
     *
     * <p>Only {@code persistent} is set on purpose: the contents are deliberately
     * <b>not</b> synchronised as part of the item stack. A backpack can hold tens of
     * thousands of slots, so shipping it over the network on every inventory update
     * would be a disaster. The 54 slots the player can actually see are synchronised
     * the normal way, through the container menu.</p>
     */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<BackpackContents>> BACKPACK_CONTENTS =
            DATA_COMPONENT_TYPES.register("contents", () -> DataComponentType.<BackpackContents>builder()
                    .persistent(BackpackContents.CODEC)
                    .build());

    private ModDataComponents() {
    }
}
