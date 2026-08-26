package cn.gbk.emcfluid.integration.jei;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.registry.ModContent;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.item.ItemStack;

public final class EmcConverterRecipeCategory implements IRecipeCategory<EmcConverterJeiRecipe> {
    private static final int WIDTH = 140;
    private static final int HEIGHT = 52;

    private final IDrawable background;
    private final IDrawable icon;

    EmcConverterRecipeCategory(IGuiHelper guiHelper) {
        background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        icon = guiHelper.createDrawableIngredient(new ItemStack(ModContent.emcConverter));
    }

    @Override
    public String getUid() {
        return EmcFluidJeiPlugin.CONVERTER_UID;
    }

    @Override
    public String getTitle() {
        return net.minecraft.client.resources.I18n.format("category.emcfluid.emc_converter");
    }

    @Override
    public String getModName() {
        return EmcFluid.NAME;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayout recipeLayout, EmcConverterJeiRecipe recipeWrapper,
                          IIngredients ingredients) {
        IGuiFluidStackGroup fluids = recipeLayout.getFluidStacks();
        fluids.init(0, true, 20, 2, 16, 32, recipeWrapper.getInputAmount(), true, null);
        fluids.init(1, false, 104, 2, 16, 32, recipeWrapper.getOutputAmount(), true, null);
        fluids.set(ingredients);
    }
}
