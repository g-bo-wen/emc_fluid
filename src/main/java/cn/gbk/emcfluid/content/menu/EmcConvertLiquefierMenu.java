package cn.gbk.emcfluid.content.menu;

import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.ProjectEAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class EmcConvertLiquefierMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final BlockPos blockPos;

    public static EmcConvertLiquefierMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (blockEntity instanceof EmcConvertLiquefierBlockEntity liquefier) {
            return new EmcConvertLiquefierMenu(id, inventory, liquefier, new SimpleContainerData(4));
        }
        return new EmcConvertLiquefierMenu(id, inventory, new ItemStackHandler(1), ContainerLevelAccess.NULL, new SimpleContainerData(4), BlockPos.ZERO);
    }

    public EmcConvertLiquefierMenu(int id, Inventory inventory, EmcConvertLiquefierBlockEntity blockEntity, ContainerData data) {
        this(id, inventory, blockEntity.getItems(), ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()), data, blockEntity.getBlockPos());
    }

    private EmcConvertLiquefierMenu(int id, Inventory inventory, IItemHandler handler, ContainerLevelAccess access, ContainerData data, BlockPos blockPos) {
        super(ModContent.EMC_CONVERT_LIQUEFIER_MENU.get(), id);
        this.access = access;
        this.data = data;
        this.blockPos = blockPos;

        addSlot(new SlotItemHandler(handler, 0, 44, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ProjectEAccess.isEmcHolder(stack);
            }
        });
        addPlayerInventory(inventory);
        addDataSlots(data);
    }

    public int getFluidAmount() {
        return data.get(0);
    }

    public EmcLiquefierBlockEntity.Mode getMode() {
        return EmcLiquefierBlockEntity.Mode.byId(data.get(1));
    }

    public int getSelectedTier() {
        return data.get(2);
    }

    public int getEnabledTiers() {
        return data.get(3);
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (ProjectEAccess.isEmcHolder(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 28) {
                if (!moveItemStackTo(stack, 28, 37, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 1, 28, false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return moved;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, ModContent.EMC_CONVERT_LIQUEFIER.get());
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }
}
