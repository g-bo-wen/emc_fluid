package cn.gbk.emcfluid.content.recipe;

import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

public class EmcConverterRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final int inputTier;
    private final int outputTier;

    public EmcConverterRecipe(ResourceLocation id, int inputTier, int outputTier) {
        this.id = id;
        this.inputTier = inputTier;
        this.outputTier = outputTier;
    }

    public int getInputTier() {
        return inputTier;
    }

    public int getOutputTier() {
        return outputTier;
    }

    public FluidStack getInput() {
        int amount = getInputAmount();
        if (amount <= 0 || !EmcFluidTierConfig.isEnabledTier(inputTier)) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(ModContent.getEmcFluidSource(inputTier).get(), amount);
    }

    public FluidStack getOutput() {
        int amount = getOutputAmount();
        if (amount <= 0 || !EmcFluidTierConfig.isEnabledTier(outputTier)) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(ModContent.getEmcFluidSource(outputTier).get(), amount);
    }

    public int getInputAmount() {
        if (!hasValidTiers()) {
            return 0;
        }
        if (outputTier == inputTier + 1) {
            return EmcFluidTierConfig.upgradeInputAmount(inputTier);
        }
        if (outputTier == inputTier - 1) {
            return 1;
        }
        return 0;
    }

    public int getOutputAmount() {
        if (!hasValidTiers()) {
            return 0;
        }
        if (outputTier == inputTier + 1) {
            return 1;
        }
        if (outputTier == inputTier - 1) {
            return EmcFluidTierConfig.downgradeOutputAmount(inputTier);
        }
        return 0;
    }

    public boolean isValid() {
        return !getInput().isEmpty() && !getOutput().isEmpty();
    }

    private boolean hasValidTiers() {
        return EmcFluidTierConfig.isEnabledTier(inputTier)
                && EmcFluidTierConfig.isEnabledTier(outputTier)
                && Math.abs(inputTier - outputTier) == 1;
    }

    @Override
    public boolean matches(Container container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModContent.EMC_CONVERTER_RECIPE.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModContent.EMC_CONVERTER_RECIPE_TYPE.get();
    }
}
