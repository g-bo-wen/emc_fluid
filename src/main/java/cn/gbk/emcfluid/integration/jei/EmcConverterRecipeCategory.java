package cn.gbk.emcfluid.integration.jei;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.recipe.EmcConverterRecipe;
import cn.gbk.emcfluid.registry.ModContent;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public class EmcConverterRecipeCategory implements IRecipeCategory<EmcConverterRecipe> {
    public static final RecipeType<EmcConverterRecipe> RECIPE_TYPE =
            RecipeType.create(EmcFluid.MODID, "emc_converter", EmcConverterRecipe.class);
    private static final int WIDTH = 140;
    private static final int HEIGHT = 48;

    private final IDrawable icon;
    private final IDrawable arrow;

    public EmcConverterRecipeCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModContent.EMC_CONVERTER_ITEM.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<EmcConverterRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("category.emcfluid.emc_converter");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, EmcConverterRecipe recipe, IFocusGroup focuses) {
        var input = recipe.getInput();
        var output = recipe.getOutput();
        builder.addInputSlot(20, 12)
                .setFluidRenderer(input.getAmount(), true, 16, 16)
                .setStandardSlotBackground()
                .addFluidStack(input.getFluid(), input.getAmount());
        builder.addOutputSlot(104, 12)
                .setFluidRenderer(output.getAmount(), true, 16, 16)
                .setOutputSlotBackground()
                .addFluidStack(output.getFluid(), output.getAmount());
    }

    @Override
    public void draw(EmcConverterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        arrow.draw(graphics, 58, 13);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, recipe.getInputAmount() + "mb", 13, 34, 0x404040, false);
        graphics.drawString(font, recipe.getOutputAmount() + "mb", 101, 34, 0x404040, false);
    }
}
