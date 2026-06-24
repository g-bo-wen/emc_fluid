package cn.gbk.emcfluid.content.menu;

import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EmcConverterMenu extends AbstractContainerMenu {
    private static final int PLAYER_INVENTORY_Y = 110;
    private static final int HOTBAR_Y = 168;

    private final ContainerLevelAccess access;
    private final BlockPos blockPos;
    private final ContainerData data;

    public static EmcConverterMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (blockEntity instanceof EmcConverterBlockEntity converter) {
            return new EmcConverterMenu(id, inventory, converter, new SimpleData());
        }
        return new EmcConverterMenu(id, inventory, ContainerLevelAccess.NULL, BlockPos.ZERO, new SimpleData());
    }

    public EmcConverterMenu(int id, Inventory inventory, EmcConverterBlockEntity blockEntity, ContainerData data) {
        this(id, inventory, ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), blockEntity.getBlockPos(), data);
    }

    private EmcConverterMenu(int id, Inventory inventory, ContainerLevelAccess access, BlockPos blockPos, ContainerData data) {
        super(ModContent.EMC_CONVERTER_MENU.get(), id);
        this.access = access;
        this.blockPos = blockPos;
        this.data = data;
        for (int i = 0; i < data.getCount(); i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return data.get(index);
                }

                @Override
                public void set(int value) {
                    data.set(index, value);
                }
            });
        }
        addPlayerInventory(inventory);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public int getRedAmount() {
        return data.get(0);
    }

    public int getBlueAmount() {
        return data.get(1);
    }

    public int getModeId() {
        return data.get(2);
    }

    public int getRedTier() {
        return data.get(3);
    }

    public int getBlueTier() {
        return data.get(4);
    }

    public int getEnabledTiers() {
        return data.get(5);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModContent.EMC_CONVERTER.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, HOTBAR_Y));
        }
    }

    private static class SimpleData implements ContainerData {
        private final int[] values = new int[6];

        @Override
        public int get(int index) {
            return values[index];
        }

        @Override
        public void set(int index, int value) {
            values[index] = value;
        }

        @Override
        public int getCount() {
            return values.length;
        }
    }
}
