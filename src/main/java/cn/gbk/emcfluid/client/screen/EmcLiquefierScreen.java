package cn.gbk.emcfluid.client.screen;

import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import cn.gbk.emcfluid.network.ModNetwork;
import cn.gbk.emcfluid.network.ToggleLiquefierModePacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class EmcLiquefierScreen extends AbstractContainerScreen<EmcLiquefierMenu> {
    private static final int ARROW_X = 78;
    private static final int ARROW_Y = 36;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 14;

    public EmcLiquefierScreen(EmcLiquefierMenu menu, Inventory inventory, Component title) {
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

        int tankX = x + 118;
        int tankY = y + 22;
        graphics.fill(tankX - 1, tankY - 1, tankX + 18, tankY + 54, 0xFF3A3A3A);
        graphics.fill(tankX, tankY, tankX + 17, tankY + 53, 0xFF101010);
        int fill = (int) (53.0F * menu.getFluidAmount() / EmcLiquefierBlockEntity.TANK_CAPACITY);
        graphics.fill(tankX + 1, tankY + 52 - fill, tankX + 16, tankY + 52, 0xFF8E35D1);

        boolean toFluid = menu.getMode() == EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID;
        int color = toFluid ? 0xFFE04444 : 0xFF37B96D;
        graphics.fill(x + ARROW_X, y + ARROW_Y, x + ARROW_X + ARROW_W, y + ARROW_Y + ARROW_H, color);
        graphics.drawCenteredString(font, toFluid ? ">" : "<", x + ARROW_X + ARROW_W / 2, y + ARROW_Y + 3, 0xFFFFFFFF);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, 8, 6, 0x404040, false);
        graphics.drawString(font, Component.literal(menu.getFluidAmount() + " / 10000 mB"), 108, 78, 0x404040, false);
        graphics.drawString(font, playerInventoryTitle, 8, 72, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int localX = (int) mouseX - leftPos;
        int localY = (int) mouseY - topPos;
        if (button == 0 && localX >= ARROW_X && localX < ARROW_X + ARROW_W && localY >= ARROW_Y && localY < ARROW_Y + ARROW_H) {
            ModNetwork.CHANNEL.sendToServer(new ToggleLiquefierModePacket(menu.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
