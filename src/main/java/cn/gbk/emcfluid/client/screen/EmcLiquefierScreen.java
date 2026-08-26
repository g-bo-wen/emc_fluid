package cn.gbk.emcfluid.client.screen;

import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import cn.gbk.emcfluid.network.ModNetwork;
import cn.gbk.emcfluid.network.ToggleLiquefierModePacket;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.io.IOException;

public class EmcLiquefierScreen extends GuiContainer {
    private static final int ARROW_X = 78;
    private static final int ARROW_Y = 36;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 14;

    protected final EmcLiquefierMenu menu;
    protected final InventoryPlayer inventory;

    public EmcLiquefierScreen(EmcLiquefierMenu menu, InventoryPlayer inventory) {
        super(menu);
        this.menu = menu;
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

        int tankX = x + 118;
        int tankY = y + 22;
        drawRect(tankX - 1, tankY - 1, tankX + 18, tankY + 54, 0xFF3A3A3A);
        drawRect(tankX, tankY, tankX + 17, tankY + 53, 0xFF101010);
        int fill = (int) (53.0F * menu.getFluidAmount() / EmcLiquefierBlockEntity.TANK_CAPACITY);
        drawRect(tankX + 1, tankY + 52 - fill, tankX + 16, tankY + 52, 0xFF8E35D1);

        boolean toFluid = menu.getMode() == EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID;
        int color = toFluid ? 0xFFE04444 : 0xFF37B96D;
        drawRect(x + ARROW_X, y + ARROW_Y, x + ARROW_X + ARROW_W, y + ARROW_Y + ARROW_H, color);
        drawCenteredString(fontRenderer, toFluid ? ">" : "<", x + ARROW_X + ARROW_W / 2, y + ARROW_Y + 3, 0xFFFFFFFF);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.emcfluid.emc_liquefier"), 8, 6, 0x404040);
        fontRenderer.drawString(menu.getFluidAmount() + " / 10000 mB", 108, 78, 0x404040);
        fontRenderer.drawString(inventory.getDisplayName().getUnformattedText(), 8, 72, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        int localX = mouseX - guiLeft;
        int localY = mouseY - guiTop;
        if (button == 0 && localX >= ARROW_X && localX < ARROW_X + ARROW_W && localY >= ARROW_Y && localY < ARROW_Y + ARROW_H) {
            ModNetwork.CHANNEL.sendToServer(new ToggleLiquefierModePacket(menu.getBlockPos()));
            return;
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
