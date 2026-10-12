package com.endlessbackpack.item;

import java.util.List;

import com.endlessbackpack.menu.BackpackMenu;
import com.endlessbackpack.registry.ModDataComponents;
import com.endlessbackpack.storage.BackpackContents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The backpack item itself. It stores its whole inventory in a data component, so the item
 * is completely self contained: pick it up, drop it, put it in a chest or send it through a
 * hopper and the contents travel with it.
 */
public class BackpackItem extends Item {

    /** {@code Inventory} slot index of the off hand. */
    public static final int OFFHAND_SLOT = 40;

    public BackpackItem(Properties properties) {
        super(properties);
    }

    // ------------------------------------------------------------------ helpers

    public static BackpackContents getContents(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.BACKPACK_CONTENTS.get(), BackpackContents.EMPTY);
    }

    public static void setContents(ItemStack stack, BackpackContents contents) {
        stack.set(ModDataComponents.BACKPACK_CONTENTS.get(), contents);
    }

    public static boolean isBackpack(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BackpackItem;
    }

    /** @return the inventory slot a backpack held in the given hand occupies. */
    public static int slotForHand(Player player, InteractionHand hand) {
        return hand == InteractionHand.OFF_HAND ? OFFHAND_SLOT : player.getInventory().selected;
    }

    // ------------------------------------------------------------------ behaviour

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            int slot = slotForHand(player, hand);
            // The slot index is shipped to the client so both sides know which stack is open.
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (windowId, inventory, ignored) -> new BackpackMenu(windowId, inventory, slot),
                            Component.translatable("container.endlessbackpack.title")),
                    buffer -> buffer.writeVarInt(slot));
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BackpackContents contents = getContents(stack);
        tooltip.add(Component.translatable("tooltip.endlessbackpack.capacity", contents.usedSlots(),
                BackpackContents.MAX_SLOTS).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.endlessbackpack.hint", BackpackContents.MAX_PAGES)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
