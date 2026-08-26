package cn.gbk.emcfluid.integration.jei;

import cn.gbk.emcfluid.registry.ModContent;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fluids.FluidStack;

public final class EmcConverterJeiRecipe implements IRecipeWrapper {
    private final int inputTier;
    private final int inputAmount;
    private final int outputTier;
    private final int outputAmount;
    private final FluidStack input;
    private final FluidStack output;

    EmcConverterJeiRecipe(int inputTier, int inputAmount, int outputTier, int outputAmount) {
        this.inputTier = inputTier;
        this.inputAmount = inputAmount;
        this.outputTier = outputTier;
        this.outputAmount = outputAmount;
        this.input = new FluidStack(ModContent.getEmcFluid(inputTier), inputAmount);
        this.output = new FluidStack(ModContent.getEmcFluid(outputTier), outputAmount);
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInput(VanillaTypes.FLUID, input.copy());
        ingredients.setOutput(VanillaTypes.FLUID, output.copy());
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String arrow = "\u2192";
        int arrowWidth = minecraft.fontRenderer.getStringWidth(arrow);
        minecraft.fontRenderer.drawString(arrow, (recipeWidth - arrowWidth) / 2, 12, 0x404040);
        minecraft.fontRenderer.drawString(inputAmount + " mB", 12, 39, 0x404040);
        minecraft.fontRenderer.drawString(outputAmount + " mB", 94, 39, 0x404040);
        String tiers = I18n.format("category.emcfluid.tier_conversion", inputTier + 1, outputTier + 1);
        int tiersWidth = minecraft.fontRenderer.getStringWidth(tiers);
        minecraft.fontRenderer.drawString(tiers, (recipeWidth - tiersWidth) / 2, 27, 0x606060);
    }

    int getInputAmount() {
        return inputAmount;
    }

    int getOutputAmount() {
        return outputAmount;
    }

    int getInputTier() {
        return inputTier;
    }

    int getOutputTier() {
        return outputTier;
    }
}
