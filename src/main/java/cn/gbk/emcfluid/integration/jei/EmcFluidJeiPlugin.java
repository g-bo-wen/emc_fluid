package cn.gbk.emcfluid.integration.jei;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.client.screen.EmcConverterScreen;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JEIPlugin
public final class EmcFluidJeiPlugin implements IModPlugin {
    public static final String CONVERTER_UID = EmcFluid.MODID + ".emc_converter";

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new EmcConverterRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registry) {
        List<EmcConverterJeiRecipe> recipes = createConverterRecipes();
        registry.addRecipes(recipes, CONVERTER_UID);
        registry.addRecipeCatalyst(new ItemStack(ModContent.emcConverter), CONVERTER_UID);
        registry.addRecipeClickArea(EmcConverterScreen.class, 76, 29, 24, 17, CONVERTER_UID);
        EmcFluid.logger.info("Registered {} EMC Converter recipes with JEI", recipes.size());
    }

    static List<EmcConverterJeiRecipe> createConverterRecipes() {
        List<EmcConverterJeiRecipe> recipes = new ArrayList<EmcConverterJeiRecipe>();
        for (int lowerTier = 0; lowerTier + 1 < EmcFluidTierConfig.enabledTiers(); lowerTier++) {
            int upgradeAmount = EmcFluidTierConfig.upgradeInputAmount(lowerTier);
            if (upgradeAmount <= 0) {
                continue;
            }
            recipes.add(new EmcConverterJeiRecipe(lowerTier, upgradeAmount, lowerTier + 1, 1));
            recipes.add(new EmcConverterJeiRecipe(lowerTier + 1, 1, lowerTier, upgradeAmount));
        }
        return Collections.unmodifiableList(recipes);
    }
}
