package com.endlessbackpack.menu;

import com.endlessbackpack.item.BackpackItem;
import com.endlessbackpack.registry.ModMenus;
import com.endlessbackpack.storage.BackpackContainer;
import com.endlessbackpack.storage.BackpackContents;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The menu of an open backpack.
 *
 * <p>Layout is a plain double chest: 6 rows of 9 storage slots on top of the 3 player
 * inventory rows and the hotbar. Navigation between pages is done with the vanilla
 * "menu button" mechanism, which means no custom networking code is needed at all.</p>
 *
 * <p>Symmetry is worth pointing out: the server builds this menu against a live
 * {@link BackpackContainer} view, while the client builds it against an empty
 * {@link SimpleContainer} that the server then fills slot by slot. That is exactly how
 * vanilla handles chests, and it keeps a 27,648 slot inventory from ever being sent
 * over the wire in one piece.</p>
 */
public class BackpackMenu extends AbstractContainerMenu {

    public static final int ROWS = 6;
    public static final int COLUMNS = 9;
    /** Slots visible at once - equals {@link BackpackContents#PAGE_SLOTS}. */
    public static final int PAGE_SLOTS = ROWS * COLUMNS;

    public static final int BUTTON_PREV_PAGE = 0;
    public static final int BUTTON_NEXT_PAGE = 1;
    public static final int BUTTON_FIRST_PAGE = 2;
    public static final int BUTTON_LAST_PAGE = 3;

    private final Player owner;
    private final int inventorySlot;
    private final Container container;
    private final PageData pageData;

    /** Client side factory, referenced by {@link ModMenus#BACKPACK_MENU}. */
    public BackpackMenu(int windowId, Inventory playerInventory, FriendlyByteBuf data) {
        this(windowId, playerInventory, data.readVarInt());
    }

    public BackpackMenu(int windowId, Inventory playerInventory, int inventorySlot) {
        super(ModMenus.BACKPACK_MENU.get(), windowId);

        this.owner = playerInventory.player;
        this.inventorySlot = inventorySlot;

        PageData pageData = new PageData();
        this.pageData = pageData;

        this.container = this.owner.level().isClientSide()
                // The client must never read the real contents - the server pushes them.
                ? new SimpleContainer(PAGE_SLOTS)
                // The server sees a live window over the backpack currently in that slot.
                : new BackpackContainer(this.owner, inventorySlot, pageData::getPage);

        // Backpack storage: 6 x 9
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                this.addSlot(new BackpackSlot(this.container, col + row * COLUMNS,
                        8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory: 3 x 9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                this.addPlayerSlot(playerInventory, col + (row + 1) * COLUMNS,
                        8 + col * 18, 84 + row * 18);
            }
        }

        // Hotbar
        for (int col = 0; col < COLUMNS; col++) {
            this.addPlayerSlot(playerInventory, col, 8 + col * 18, 142);
        }

        // Slot 0 carries the current page, slot 1 the page count, and both are synced for free.
        this.addDataSlots(pageData);
    }

    private void addPlayerSlot(Inventory playerInventory, int index, int x, int y) {
        // The slot holding the open backpack itself is frozen so it cannot be moved away
        // from underneath the menu.
        Slot slot = index == this.inventorySlot
                ? new LockedSlot(playerInventory, index, x, y)
                : new Slot(playerInventory, index, x, y);
        this.addSlot(slot);
    }

    // ------------------------------------------------------------------ state

    /** Current page, always read from the synced data slot so both sides agree. */
    public int getPage() {
        return this.pageData.getPage();
    }

    public int getMaxPage() {
        return BackpackContents.MAX_PAGES;
    }

    // ------------------------------------------------------------------ buttons

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (player != this.owner) {
            return false;
        }

        int lastPage = BackpackContents.MAX_PAGES - 1;
        int current = this.pageData.getPage();

        int target = switch (id) {
            case BUTTON_PREV_PAGE -> current - 1;
            case BUTTON_NEXT_PAGE -> current + 1;
            case BUTTON_FIRST_PAGE -> 0;
            case BUTTON_LAST_PAGE -> lastPage;
            default -> Integer.MIN_VALUE;
        };

        if (target == Integer.MIN_VALUE) {
            return false;
        }

        target = Mth.clamp(target, 0, lastPage);
        if (target != current) {
            this.pageData.setPage(target);
            // Push the new page's contents out immediately instead of waiting for the next tick.
            this.broadcastChanges();
        }
        return true;
    }

    // ------------------------------------------------------------------ slots

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();

        if (index < PAGE_SLOTS) {
            // Backpack -> player inventory
            if (!this.moveItemStackTo(stack, PAGE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Player inventory -> backpack
            if (!this.moveItemStackTo(stack, 0, PAGE_SLOTS, false)) {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        // On the server this returns false the moment the backpack leaves its slot,
        // which makes the menu close itself instead of showing garbage.
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            player.getInventory().setChanged();
        }
    }

    // ------------------------------------------------------------------ inner types

    /** Storage slot. Backpacks are refused so they cannot be nested into each other. */
    private static final class BackpackSlot extends Slot {

        BackpackSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !(stack.getItem() instanceof BackpackItem);
        }
    }

    /** Player inventory slot holding the open backpack: visible, but immovable. */
    private static final class LockedSlot extends Slot {

        LockedSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    /** Two synced integers: the current page and the page count. */
    public static final class PageData implements ContainerData {

        private int page;

        @Override
        public int get(int index) {
            return index == 0 ? this.page : BackpackContents.MAX_PAGES;
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                this.page = Mth.clamp(value, 0, BackpackContents.MAX_PAGES - 1);
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        public int getPage() {
            return this.page;
        }

        public void setPage(int page) {
            this.page = Mth.clamp(page, 0, BackpackContents.MAX_PAGES - 1);
        }
    }
}
