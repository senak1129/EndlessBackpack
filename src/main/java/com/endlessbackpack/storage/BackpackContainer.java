package com.endlessbackpack.storage;

import java.util.function.IntSupplier;

import com.endlessbackpack.item.BackpackItem;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Server side {@link Container} implementation for an open backpack.
 *
 * <p>It is a <b>windowed view</b>: the menu only ever exposes {@link BackpackContents#PAGE_SLOTS}
 * slots, and index {@code i} maps onto the global slot {@code page * 54 + i}. Nothing is cached -
 * every access re-reads the item stack out of the player's inventory, so external changes (death,
 * inventory sync, another player, commands) can never desynchronise the view from the data.</p>
 */
public final class BackpackContainer implements Container {

    private final Player player;
    private final int inventorySlot;
    private final IntSupplier pageSupplier;

    /**
     * @param player        the player the backpack belongs to
     * @param inventorySlot index in {@link net.minecraft.world.entity.player.Inventory} holding the backpack
     * @param pageSupplier  supplies the currently displayed page (owned by the menu)
     */
    public BackpackContainer(Player player, int inventorySlot, IntSupplier pageSupplier) {
        this.player = player;
        this.inventorySlot = inventorySlot;
        this.pageSupplier = pageSupplier;
    }

    private ItemStack backpack() {
        ItemStack stack = this.player.getInventory().getItem(this.inventorySlot);
        return BackpackItem.isBackpack(stack) ? stack : ItemStack.EMPTY;
    }

    private int globalSlot(int windowSlot) {
        return this.pageSupplier.getAsInt() * BackpackContents.PAGE_SLOTS + windowSlot;
    }

    // ------------------------------------------------------------------ Container

    @Override
    public int getContainerSize() {
        return BackpackContents.PAGE_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        ItemStack backpack = backpack();
        return backpack.isEmpty() || BackpackItem.getContents(backpack).isEmpty();
    }

    @Override
    public ItemStack getItem(int slot) {
        if (slot < 0 || slot >= BackpackContents.PAGE_SLOTS) {
            return ItemStack.EMPTY;
        }
        ItemStack backpack = backpack();
        if (backpack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return BackpackItem.getContents(backpack).get(globalSlot(slot));
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack current = getItem(slot);
        if (current.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = current.copyWithCount(Math.min(amount, current.getCount()));
        ItemStack leftover = current.copy();
        leftover.shrink(taken.getCount());
        setItem(slot, leftover);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack current = getItem(slot);
        setItem(slot, ItemStack.EMPTY);
        return current;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < 0 || slot >= BackpackContents.PAGE_SLOTS) {
            return;
        }
        ItemStack backpack = backpack();
        if (backpack.isEmpty()) {
            return;
        }
        BackpackContents contents = BackpackItem.getContents(backpack);
        BackpackContents updated = contents.with(globalSlot(slot), stack);
        if (updated != contents && !updated.equals(contents)) {
            BackpackItem.setContents(backpack, updated);
        }
    }

    /** Backpacks cannot be nested, otherwise the storage could recurse without bound. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return !(stack.getItem() instanceof BackpackItem);
    }

    @Override
    public void setChanged() {
        // The real data lives on an item stack inside the player inventory,
        // so that is what has to be flagged as dirty for saving.
        this.player.getInventory().setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player
                && BackpackItem.isBackpack(player.getInventory().getItem(this.inventorySlot));
    }

    @Override
    public void clearContent() {
        ItemStack backpack = backpack();
        if (!backpack.isEmpty()) {
            BackpackItem.setContents(backpack, BackpackContents.EMPTY);
        }
    }
}
