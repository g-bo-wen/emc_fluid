package cn.gbk.emcfluid.client.screen;

import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import cn.gbk.emcfluid.network.ModNetwork;
import cn.gbk.emcfluid.network.ToggleConverterModePacket;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.io.IOException;

public class EmcConverterScreen extends GuiContainer {
    private static final int RED_X = 36;
    private static final int BLUE_X = 124;
    private static final int TANK_Y = 18;
    private static final int TANK_W = 24;
    private static final int TANK_H = 54;
    private static final int PLAYER_INVENTORY_LABEL_Y = 100;
    private static final int ARROW_X = 78;
    private static final int ARROW_Y = 36;
    private static final int ARROW_W = 20;
    private static final int ARROW_H = 12;

    private final EmcConverterMenu menu;
    private final InventoryPlayer inventory;

    public EmcConverterScreen(EmcConverterMenu menu, InventoryPlayer inventory) {
        super(menu);
        this.menu = menu;
        this.inventory = inventory;
        xSize = 176;
        ySize = 190;
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        int x = guiLeft;
        int y = guiTop;
        drawRect(x, y, x + xSize, y + ySize, 0xFFB8B8B8);
        drawTank(x + RED_X, y + TANK_Y, 0xFFB83232, menu.getRedAmount(), getRedDisplayTier());
        drawTank(x + BLUE_X, y + TANK_Y, 0xFF3264C8, menu.getBlueAmount(), getBlueDisplayTier());
        int arrowColor = menu.getModeId() == EmcConverterBlockEntity.Mode.UPGRADE.ordinal() ? 0xFFE04444 : 0xFF3A7DE0;
        drawRect(x + ARROW_X, y + ARROW_Y + 4, x + ARROW_X + 14, y + ARROW_Y + 8, arrowColor);
        drawRect(x + ARROW_X + 14, y + ARROW_Y + 2, x + ARROW_X + ARROW_W, y + ARROW_Y + 10, arrowColor);
    }

    private void drawTank(int x, int y, int borderColor, int amount, int tier) {
        drawRect(x - 2, y - 2, x + TANK_W + 2, y + TANK_H + 2, borderColor);
        drawRect(x, y, x + TANK_W, y + TANK_H, 0xFF202020);
        int fill = Math.min(TANK_H, amount * TANK_H / EmcConverterBlockEntity.TANK_CAPACITY);
        if (fill > 0) {
            drawRect(x + 2, y + TANK_H - fill, x + TANK_W - 2, y + TANK_H - 2, borderColor);
        }
        drawTankLabels(x + TANK_W / 2, y + TANK_H + 8, amount, tier);
    }

    private void drawTankLabels(int centerX, int y, int amount, int tier) {
        String fluidName = tier > 0
                ? I18n.format("fluid.emcfluid.emc_fluid_t" + tier)
                : I18n.format("container.emcfluid.empty_fluid");
        drawCenteredPlainString(fluidName, centerX, y, tier > 0 ? 0x404040 : 0x606060);
        drawCenteredPlainString(amount + "/" + EmcConverterBlockEntity.TANK_CAPACITY + "mb", centerX, y + 10, 0x404040);
    }

    private void drawCenteredPlainString(String text, int centerX, int y, int color) {
        fontRenderer.drawString(text, centerX - fontRenderer.getStringWidth(text) / 2, y, color);
    }

    private int getRedDisplayTier() {
        int redTier = menu.getRedTier();
        if (redTier > 0) {
            return redTier;
        }
        int blueTier = menu.getBlueTier();
        if (blueTier <= 0) {
            return 0;
        }
        int inferredTier = isUpgradeMode() ? blueTier - 1 : blueTier + 1;
        return isEnabledTier(inferredTier) ? inferredTier : 0;
    }

    private int getBlueDisplayTier() {
        int blueTier = menu.getBlueTier();
        if (blueTier > 0) {
            return blueTier;
        }
        int redTier = menu.getRedTier();
        if (redTier <= 0) {
            return 0;
        }
        int inferredTier = isUpgradeMode() ? redTier + 1 : redTier - 1;
        return isEnabledTier(inferredTier) ? inferredTier : 0;
    }

    private boolean isUpgradeMode() {
        return menu.getModeId() == EmcConverterBlockEntity.Mode.UPGRADE.ordinal();
    }

    private boolean isEnabledTier(int tier) {
        return tier > 0 && tier <= menu.getEnabledTiers();
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.emcfluid.emc_converter"), 8, 6, 0x404040);
        fontRenderer.drawString(inventory.getDisplayName().getUnformattedText(), 8, PLAYER_INVENTORY_LABEL_Y, 0x404040);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
        if (button == 0) {
            int localX = mouseX - guiLeft;
            int localY = mouseY - guiTop;
            if (localX >= ARROW_X && localX < ARROW_X + ARROW_W && localY >= ARROW_Y && localY < ARROW_Y + ARROW_H) {
                ModNetwork.CHANNEL.sendToServer(new ToggleConverterModePacket(menu.getBlockPos()));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
