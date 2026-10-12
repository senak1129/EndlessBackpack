package com.endlessbackpack.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.item.ItemStack;

/**
 * The actual storage of a backpack: a <b>sparse</b> slot -> stack map.
 *
 * <p>Two things matter here:</p>
 * <ul>
 *   <li><b>Size.</b> {@link #MAX_SLOTS} is 27,648 slots (512 pages of 54). That is roughly
 *       512 double chests worth of space. Nothing is pre-allocated, so an empty backpack
 *       costs nothing in memory or on disk.</li>
 *   <li><b>Serialisation.</b> Only occupied slots are written to NBT, each entry carrying
 *       its own index. Saving an almost empty backpack therefore produces a tiny tag even
 *       though the logical size is enormous.</li>
 * </ul>
 *
 * <p>Instances are immutable; {@link #with(int, ItemStack)} returns a new snapshot. That keeps
 * item stack comparison (used by the container sync code) correct and predictable.</p>
 */
public final class BackpackContents {

    /** Slots per page, matching a double chest layout (6 rows of 9). */
    public static final int PAGE_SLOTS = 54;

    /** Number of pages. Raise this if 27,648 slots ever stops being enough. */
    public static final int MAX_PAGES = 512;

    /** Total addressable slots. */
    public static final int MAX_SLOTS = MAX_PAGES * PAGE_SLOTS;

    public static final BackpackContents EMPTY = new BackpackContents(Map.of());

    private final Map<Integer, ItemStack> items;

    private BackpackContents(Map<Integer, ItemStack> items) {
        this.items = items;
    }

    // ------------------------------------------------------------------ codec

    private record Entry(int slot, ItemStack item) {
    }

    private static final Codec<Entry> ENTRY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("slot").forGetter(Entry::slot),
            ItemStack.OPTIONAL_CODEC.fieldOf("item").forGetter(Entry::item)
    ).apply(instance, Entry::new));

    /** Serialises as a list of {@code {slot, item}} pairs - empty slots are skipped entirely. */
    public static final Codec<BackpackContents> CODEC =
            ENTRY_CODEC.listOf().xmap(BackpackContents::fromEntries, BackpackContents::toEntries);

    private static BackpackContents fromEntries(List<Entry> entries) {
        Map<Integer, ItemStack> map = new HashMap<>();
        for (Entry entry : entries) {
            if (entry.slot() < 0 || entry.slot() >= MAX_SLOTS) {
                continue;
            }
            if (entry.item().isEmpty()) {
                continue;
            }
            map.put(entry.slot(), entry.item());
        }
        return map.isEmpty() ? EMPTY : new BackpackContents(map);
    }

    private static List<Entry> toEntries(BackpackContents contents) {
        if (contents.items.isEmpty()) {
            return List.of();
        }
        // Sorted so that the produced NBT is stable, which keeps stack comparison reliable.
        Map<Integer, ItemStack> sorted = new TreeMap<>(contents.items);
        List<Entry> entries = new ArrayList<>(sorted.size());
        sorted.forEach((slot, stack) -> entries.add(new Entry(slot, stack)));
        return entries;
    }

    // ------------------------------------------------------------------ access

    /** @return the stack in {@code slot}, or {@link ItemStack#EMPTY} when the slot is free. */
    public ItemStack get(int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            return ItemStack.EMPTY;
        }
        return this.items.getOrDefault(slot, ItemStack.EMPTY);
    }

    /** @return a new snapshot with {@code slot} set to {@code stack} (an empty stack frees the slot). */
    public BackpackContents with(int slot, ItemStack stack) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            return this;
        }
        Map<Integer, ItemStack> copy = new HashMap<>(this.items);
        if (stack.isEmpty()) {
            if (copy.remove(slot) == null) {
                return this;
            }
        } else {
            copy.put(slot, stack.copy());
        }
        return new BackpackContents(copy);
    }

    public boolean isEmpty() {
        return this.items.isEmpty();
    }

    /** @return how many slots are currently occupied. */
    public int usedSlots() {
        return this.items.size();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        return other instanceof BackpackContents contents && this.items.equals(contents.items);
    }

    @Override
    public int hashCode() {
        return this.items.hashCode();
    }

    @Override
    public String toString() {
        return "BackpackContents[" + this.items.size() + "/" + MAX_SLOTS + " slots used]";
    }
}
