package com.endlessbackpack.client;

import org.lwjgl.glfw.GLFW;

import com.endlessbackpack.menu.BackpackMenu;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Screen for the backpack.
 *
 * <p>It reuses the vanilla double chest background, so the mod ships no GUI texture at all.
 * Page navigation lives in the title bar: {@code <} and {@code >} are drawn with the regular
 * font and made clickable, and the mouse wheel and arrow keys work too.</p>
 */
public class BackpackScreen extends AbstractContainerScreen<BackpackMenu> {

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");

    private static final int LABEL_COLOR = 0x404040;
    private static final int ARROW_HOVER_COLOR = 0xFFFFFF;

    /** GUI relative geometry of the two arrows and the page readout. */
    private static final int ARROW_Y = 6;
    private static final int ARROW_WIDTH = 6;

    private int leftArrowX;
    private int rightArrowX;

    public BackpackScreen(BackpackMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageHeight = 114 + BackpackMenu.ROWS * 18;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void init() {
        super.init();
        this.rightArrowX = this.imageWidth - 16;
        this.leftArrowX = this.rightArrowX - 30;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draws the title and the "Inventory" label.
        super.renderLabels(guiGraphics, mouseX, mouseY);

        // "page / total" right aligned in the title bar.
        String pageText = (this.menu.getPage() + 1) + " / " + this.menu.getMaxPage();
        guiGraphics.drawString(this.font, pageText,
                this.leftArrowX - 6 - this.font.width(pageText), ARROW_Y, LABEL_COLOR, false);

        boolean hoverLeft = isOverArrow(mouseX, mouseY, this.leftArrowX);
        boolean hoverRight = isOverArrow(mouseX, mouseY, this.rightArrowX);

        drawArrow(guiGraphics, "<", this.leftArrowX, isAtFirstPage(), hoverLeft);
        drawArrow(guiGraphics, ">", this.rightArrowX, isAtLastPage(), hoverRight);
    }

    private void drawArrow(GuiGraphics guiGraphics, String glyph, int x, boolean disabled, boolean hovered) {
        int color = disabled ? 0xA0A0A0 : (hovered ? ARROW_HOVER_COLOR : LABEL_COLOR);
        guiGraphics.drawString(this.font, glyph, x, ARROW_Y, color, hovered);
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.minecraft != null) {
            if (isOverArrow(mouseX, mouseY, this.leftArrowX)) {
                sendButton(BackpackMenu.BUTTON_PREV_PAGE);
                return true;
            }
            if (isOverArrow(mouseX, mouseY, this.rightArrowX)) {
                sendButton(BackpackMenu.BUTTON_NEXT_PAGE);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Only scroll while the pointer is actually over the backpack, otherwise the
        // screen would swallow scrolling meant for JEI or the rest of the UI.
        if (this.isHovering(0, 0, this.imageWidth, this.imageHeight, mouseX, mouseY)) {
            if (scrollY > 0) {
                sendButton(BackpackMenu.BUTTON_PREV_PAGE);
                return true;
            }
            if (scrollY < 0) {
                sendButton(BackpackMenu.BUTTON_NEXT_PAGE);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            sendButton(BackpackMenu.BUTTON_PREV_PAGE);
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            sendButton(BackpackMenu.BUTTON_NEXT_PAGE);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ------------------------------------------------------------------ helpers

    /** Sends a page change request to the server through the vanilla menu button packet. */
    private void sendButton(int buttonId) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, buttonId);
        }
    }

    private boolean isAtFirstPage() {
        return this.menu.getPage() <= 0;
    }

    private boolean isAtLastPage() {
        return this.menu.getPage() >= this.menu.getMaxPage() - 1;
    }

    private boolean isOverArrow(double mouseX, double mouseY, int arrowX) {
        double relativeX = mouseX - this.leftPos - arrowX;
        double relativeY = mouseY - this.topPos - ARROW_Y;
        return relativeX >= -3 && relativeX <= ARROW_WIDTH + 3
                && relativeY >= -2 && relativeY <= 11;
    }
}
