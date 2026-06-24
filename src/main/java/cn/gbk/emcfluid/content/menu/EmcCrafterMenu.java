package cn.gbk.emcfluid.content.menu;

import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public class EmcCrafterMenu extends AbstractContainerMenu {
    public static final int PATTERN_SLOT_X = 26;
    public static final int PATTERN_SLOT_Y = 35;

    private final ContainerLevelAccess access;
    private final IItemHandler patternHandler;
    private final IItemHandler outputHandler;

    public static EmcCrafterMenu fromNetwork(int id, Inventory inventory, FriendlyByteBuf buffer) {
        BlockEntity blockEntity = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        if (blockEntity instanceof EmcCrafterBlockEntity crafter) {
            return new EmcCrafterMenu(id, inventory, crafter);
        }
        return new EmcCrafterMenu(id, inventory, new ItemStackHandler(1), new ItemStackHandler(9), ContainerLevelAccess.NULL);
    }

    public EmcCrafterMenu(int id, Inventory inventory, EmcCrafterBlockEntity blockEntity) {
        this(id, inventory, blockEntity.getItems(), blockEntity.getOutputCache(),
                ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos()));
    }

    private EmcCrafterMenu(int id, Inventory inventory, IItemHandler patternHandler, IItemHandler outputHandler,
                           ContainerLevelAccess access) {
        super(ModContent.EMC_CRAFTER_MENU.get(), id);
        this.access = access;
        this.patternHandler = patternHandler;
        this.outputHandler = outputHandler;

        addSlot(new SlotItemHandler(patternHandler, 0, PATTERN_SLOT_X, PATTERN_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return KnowledgePatternData.isPattern(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(outputHandler, col + row * 3, 98 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addPlayerInventory(inventory);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack moved = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            moved = stack.copy();
            if (index == 0) {
                if (!moveItemStackTo(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 10) {
                if (!moveItemStackTo(stack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (KnowledgePatternData.isPattern(stack)) {
                if (!moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < 37) {
                if (!moveItemStackTo(stack, 37, 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 10, 37, false)) {
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
        return stillValid(access, player, ModContent.EMC_CRAFTER.get());
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
