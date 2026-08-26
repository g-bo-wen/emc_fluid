package cn.gbk.emcfluid.client.screen;

import cn.gbk.emcfluid.content.menu.EmcCrafterMenu;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

public class EmcCrafterScreen extends GuiContainer {
    private static final int SLOT_SIZE = 18;

    private final InventoryPlayer inventory;

    public EmcCrafterScreen(EmcCrafterMenu menu, InventoryPlayer inventory) {
        super(menu);
        this.inventory = inventory;
        xSize = 176;
        ySize = 166;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft;
        int y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, 0xFFB8B8B8);
        drawRect(x + 6, y + 6, x + xSize - 6, y + ySize - 6, 0xFFCFCFCF);
        drawPatternSlotBorder(x + EmcCrafterMenu.PATTERN_SLOT_X, y + EmcCrafterMenu.PATTERN_SLOT_Y);
    }

    private void drawPatternSlotBorder(int x, int y) {
        drawRect(x - 2, y - 2, x + 20, y + 20, 0xFF707070);
        drawRect(x - 1, y - 1, x + 19, y + 19, 0xFF8F8F8F);
        drawRect(x, y, x + SLOT_SIZE, y + SLOT_SIZE, 0xFFC7C7C7);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.emcfluid.emc_crafter"), 8, 6, 0x404040);
        fontRenderer.drawString(I18n.format("label.emcfluid.knowledge_pattern_slot"), 8, 20, 0x404040);
        fontRenderer.drawString(I18n.format("label.emcfluid.output_cache"), 96, 6, 0x404040);
        fontRenderer.drawString(inventory.getDisplayName().getUnformattedText(), 8, 72, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }
}
