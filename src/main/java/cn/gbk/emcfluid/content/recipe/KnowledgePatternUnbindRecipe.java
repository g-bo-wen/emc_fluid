package cn.gbk.emcfluid.content.recipe;

import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class KnowledgePatternUnbindRecipe extends CustomRecipe {
    public KnowledgePatternUnbindRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return findBoundPattern(container) >= 0;
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        return findBoundPattern(container) >= 0 ? ModContent.KNOWLEDGE_PATTERN.get().getDefaultInstance() : ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 1;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ModContent.KNOWLEDGE_PATTERN.get().getDefaultInstance();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModContent.KNOWLEDGE_PATTERN_UNBIND_RECIPE.get();
    }

    private int findBoundPattern(CraftingContainer container) {
        int foundSlot = -1;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!KnowledgePatternData.isPattern(stack) || !KnowledgePatternData.isBound(stack) || foundSlot >= 0) {
                return -1;
            }
            foundSlot = i;
        }
        return foundSlot;
    }
}
