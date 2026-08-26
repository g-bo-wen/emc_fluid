package cn.gbk.emcfluid.content.recipe;

import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

public final class KnowledgePatternUnbindRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inventory, World world) {
        return findBoundPattern(inventory) >= 0;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inventory) {
        return findBoundPattern(inventory) >= 0 ? new ItemStack(ModContent.knowledgePattern) : ItemStack.EMPTY;
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(ModContent.knowledgePattern);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        return NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private int findBoundPattern(InventoryCrafting inventory) {
        int foundSlot = -1;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            if (!KnowledgePatternData.isPattern(stack) || !KnowledgePatternData.isBound(stack) || foundSlot >= 0) {
                return -1;
            }
            foundSlot = slot;
        }
        return foundSlot;
    }
}
