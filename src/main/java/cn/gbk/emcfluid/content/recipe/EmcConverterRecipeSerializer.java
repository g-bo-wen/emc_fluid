package cn.gbk.emcfluid.content.recipe;

import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;

public class EmcConverterRecipeSerializer implements RecipeSerializer<EmcConverterRecipe> {
    @Override
    public EmcConverterRecipe fromJson(ResourceLocation id, JsonObject json) {
        int inputTier = readTier(json, "input_tier");
        int outputTier = readTier(json, "output_tier");
        validateTiers(inputTier, outputTier);
        return new EmcConverterRecipe(id, inputTier, outputTier);
    }

    @Nullable
    @Override
    public EmcConverterRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        int inputTier = buffer.readVarInt();
        int outputTier = buffer.readVarInt();
        return new EmcConverterRecipe(id, inputTier, outputTier);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buffer, EmcConverterRecipe recipe) {
        buffer.writeVarInt(recipe.getInputTier());
        buffer.writeVarInt(recipe.getOutputTier());
    }

    private static int readTier(JsonObject json, String key) {
        int tier = GsonHelper.getAsInt(json, key);
        if (tier < 1 || tier > EmcFluidTierConfig.MAX_TIERS) {
            throw new JsonSyntaxException(key + " must be between 1 and " + EmcFluidTierConfig.MAX_TIERS);
        }
        return tier - 1;
    }

    private static void validateTiers(int inputTier, int outputTier) {
        if (Math.abs(inputTier - outputTier) != 1) {
            throw new JsonSyntaxException("EMC Converter recipes must convert between adjacent tiers");
        }
    }
}
