package cn.gbk.emcfluid.client.screen;

import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import cn.gbk.emcfluid.network.ChangeConvertLiquefierTierPacket;
import cn.gbk.emcfluid.network.ModNetwork;
import cn.gbk.emcfluid.network.ToggleConvertLiquefierModePacket;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;

import java.io.IOException;

public class EmcConvertLiquefierScreen extends GuiContainer {
    private static final int MODE_ARROW_X = 78;
    private static final int MODE_ARROW_Y = 36;
    private static final int MODE_ARROW_W = 22;
    private static final int MODE_ARROW_H = 14;
    private static final int TIER_X = 104;
    private static final int UP_Y = 24;
    private static final int DOWN_Y = 50;
    private static final int TIER_BUTTON_W = 18;
    private static final int TIER_BUTTON_H = 10;
    private static final int COLOR_X = 105;
    private static final int COLOR_Y = 36;
    private static final int COLOR_SIZE = 16;
    private static final int[] TIER_COLORS = {0xFF8E35D1, 0xFF3264C8, 0xFF35B6D1, 0xFFD135C6, 0xFFD13535};

    private final EmcConvertLiquefierMenu menu;
    private final InventoryPlayer inventory;

    public EmcConvertLiquefierScreen(EmcConvertLiquefierMenu menu, InventoryPlayer inventory) {
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

        int tankX = x + 132;
        int tankY = y + 22;
        drawRect(tankX - 1, tankY - 1, tankX + 18, tankY + 54, 0xFF3A3A3A);
        drawRect(tankX, tankY, tankX + 17, tankY + 53, 0xFF101010);
        int fill = menu.getFluidAmount() * 53 / EmcConvertLiquefierBlockEntity.TANK_CAPACITY;
        drawRect(tankX + 1, tankY + 52 - fill, tankX + 16, tankY + 52, tierColor());

        boolean toFluid = menu.getMode() == EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID;
        int color = toFluid ? 0xFFE04444 : 0xFF37B96D;
        drawRect(x + MODE_ARROW_X, y + MODE_ARROW_Y, x + MODE_ARROW_X + MODE_ARROW_W, y + MODE_ARROW_Y + MODE_ARROW_H, color);
        drawCenteredString(fontRenderer, toFluid ? ">" : "<", x + MODE_ARROW_X + MODE_ARROW_W / 2, y + MODE_ARROW_Y + 3, 0xFFFFFFFF);

        drawTierButton(x + TIER_X, y + UP_Y, true, canTierUp());
        drawRect(x + COLOR_X - 1, y + COLOR_Y - 1, x + COLOR_X + COLOR_SIZE + 1, y + COLOR_Y + COLOR_SIZE + 1, 0xFF3A3A3A);
        drawRect(x + COLOR_X, y + COLOR_Y, x + COLOR_X + COLOR_SIZE, y + COLOR_Y + COLOR_SIZE, tierColor());
        drawTierButton(x + TIER_X, y + DOWN_Y, false, canTierDown());
    }

    private void drawTierButton(int x, int y, boolean up, boolean enabled) {
        drawRect(x, y, x + TIER_BUTTON_W, y + TIER_BUTTON_H, enabled ? 0xFF4A7FD1 : 0xFF777777);
        drawCenteredString(fontRenderer, up ? "^" : "v", x + TIER_BUTTON_W / 2, y + 1, 0xFFFFFFFF);
    }

    private int tierColor() {
        return TIER_COLORS[Math.max(0, Math.min(menu.getSelectedTier(), TIER_COLORS.length - 1))];
    }

    private boolean canTierUp() {
        return menu.getFluidAmount() == 0 && menu.getSelectedTier() + 1 < menu.getEnabledTiers();
    }

    private boolean canTierDown() {
        return menu.getFluidAmount() == 0 && menu.getSelectedTier() > 0;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        fontRenderer.drawString(I18n.format("container.emcfluid.emc_convert_liquefier"), 8, 6, 0x404040);
        fontRenderer.drawString(menu.getFluidAmount() + " / 10000 mB", 116, 78, 0x404040);
        fontRenderer.drawString("T" + (menu.getSelectedTier() + 1), 106, 65, 0x404040);
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
        if (button == 0 && localX >= MODE_ARROW_X && localX < MODE_ARROW_X + MODE_ARROW_W && localY >= MODE_ARROW_Y && localY < MODE_ARROW_Y + MODE_ARROW_H) {
            ModNetwork.CHANNEL.sendToServer(new ToggleConvertLiquefierModePacket(menu.getBlockPos()));
            return;
        }
        if (button == 0 && localX >= TIER_X && localX < TIER_X + TIER_BUTTON_W) {
            if (localY >= UP_Y && localY < UP_Y + TIER_BUTTON_H && canTierUp()) {
                ModNetwork.CHANNEL.sendToServer(new ChangeConvertLiquefierTierPacket(menu.getBlockPos(), 1));
                return;
            }
            if (localY >= DOWN_Y && localY < DOWN_Y + TIER_BUTTON_H && canTierDown()) {
                ModNetwork.CHANNEL.sendToServer(new ChangeConvertLiquefierTierPacket(menu.getBlockPos(), -1));
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }
}
