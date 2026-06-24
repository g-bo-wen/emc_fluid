package cn.gbk.emcfluid.client.screen;

import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import cn.gbk.emcfluid.network.ModNetwork;
import cn.gbk.emcfluid.network.ToggleConverterModePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EmcConverterScreen extends AbstractContainerScreen<EmcConverterMenu> {
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

    public EmcConverterScreen(EmcConverterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 176;
        imageHeight = 190;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFB8B8B8);
        drawTank(graphics, x + RED_X, y + TANK_Y, 0xFFB83232, menu.getRedAmount(), getRedDisplayTier());
        drawTank(graphics, x + BLUE_X, y + TANK_Y, 0xFF3264C8, menu.getBlueAmount(), getBlueDisplayTier());
        int arrowColor = menu.getModeId() == EmcConverterBlockEntity.Mode.UPGRADE.ordinal() ? 0xFFE04444 : 0xFF3A7DE0;
        graphics.fill(x + ARROW_X, y + ARROW_Y + 4, x + ARROW_X + 14, y + ARROW_Y + 8, arrowColor);
        graphics.fill(x + ARROW_X + 14, y + ARROW_Y + 2, x + ARROW_X + ARROW_W, y + ARROW_Y + 10, arrowColor);
    }

    private void drawTank(GuiGraphics graphics, int x, int y, int borderColor, int amount, int tier) {
        graphics.fill(x - 2, y - 2, x + TANK_W + 2, y + TANK_H + 2, borderColor);
        graphics.fill(x, y, x + TANK_W, y + TANK_H, 0xFF202020);
        int fill = Math.min(TANK_H, amount * TANK_H / EmcConverterBlockEntity.TANK_CAPACITY);
        if (fill > 0) {
            graphics.fill(x + 2, y + TANK_H - fill, x + TANK_W - 2, y + TANK_H - 2, borderColor);
        }
        drawTankLabels(graphics, x + TANK_W / 2, y + TANK_H + 8, amount, tier);
    }

    private void drawTankLabels(GuiGraphics graphics, int centerX, int y, int amount, int tier) {
        String fluidName = tier > 0
                ? Component.translatable("fluid.emcfluid.emc_fluid_t" + tier).getString()
                : Component.translatable("container.emcfluid.empty_fluid").getString();
        drawCenteredPlainString(graphics, fluidName, centerX, y, tier > 0 ? 0x404040 : 0x606060);
        drawCenteredPlainString(graphics, amount + "/" + EmcConverterBlockEntity.TANK_CAPACITY + "mb", centerX, y + 10, 0x404040);
    }

    private void drawCenteredPlainString(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawString(font, text, centerX - font.width(text) / 2, y, color, false);
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
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, PLAYER_INVENTORY_LABEL_Y, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int localX = (int) mouseX - leftPos;
            int localY = (int) mouseY - topPos;
            if (localX >= ARROW_X && localX < ARROW_X + ARROW_W && localY >= ARROW_Y && localY < ARROW_Y + ARROW_H) {
                ModNetwork.CHANNEL.sendToServer(new ToggleConverterModePacket(menu.getBlockPos()));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
