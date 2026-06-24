package cn.gbk.emcfluid.integration.jei;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.recipe.EmcConverterRecipe;
import cn.gbk.emcfluid.registry.ModContent;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@JeiPlugin
public class EmcFluidJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(EmcFluid.MODID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new EmcConverterRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<EmcConverterRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModContent.EMC_CONVERTER_RECIPE_TYPE.get())
                .stream()
                .filter(EmcConverterRecipe::isValid)
                .toList();
        registration.addRecipes(EmcConverterRecipeCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(EmcConverterRecipeCategory.RECIPE_TYPE, ModContent.EMC_CONVERTER_ITEM.get());
    }
}
