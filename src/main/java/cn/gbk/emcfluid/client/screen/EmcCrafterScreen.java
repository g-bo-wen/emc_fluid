package cn.gbk.emcfluid.client.screen;

import cn.gbk.emcfluid.content.menu.EmcCrafterMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EmcCrafterScreen extends AbstractContainerScreen<EmcCrafterMenu> {
    private static final int SLOT_SIZE = 18;

    public EmcCrafterScreen(EmcCrafterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFB8B8B8);
        graphics.fill(x + 6, y + 6, x + imageWidth - 6, y + imageHeight - 6, 0xFFCFCFCF);
        drawPatternSlotBorder(graphics, x + EmcCrafterMenu.PATTERN_SLOT_X, y + EmcCrafterMenu.PATTERN_SLOT_Y);
    }

    private void drawPatternSlotBorder(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 2, y - 2, x + 20, y + 20, 0xFF707070);
        graphics.fill(x - 1, y - 1, x + 19, y + 19, 0xFF8F8F8F);
        graphics.fill(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFC7C7C7);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.translatable("label.emcfluid.knowledge_pattern_slot"), 8, 20, 0x404040, false);
        graphics.drawString(font, Component.translatable("label.emcfluid.output_cache"), 96, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
