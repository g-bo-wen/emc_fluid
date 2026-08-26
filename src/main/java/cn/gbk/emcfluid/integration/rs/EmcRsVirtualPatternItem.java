package cn.gbk.emcfluid.integration.rs;

import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternProvider;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

/**
 * Hidden persistence carrier used by RS crafting tasks. It is registered only
 * when RS is installed and is never exposed in the creative inventory.
 */
final class EmcRsVirtualPatternItem extends Item implements ICraftingPatternProvider {
    @Nonnull
    @Override
    public ICraftingPattern create(World world, ItemStack stack, ICraftingPatternContainer container) {
        return EmcRsPattern.fromStack(stack, container);
    }
}
