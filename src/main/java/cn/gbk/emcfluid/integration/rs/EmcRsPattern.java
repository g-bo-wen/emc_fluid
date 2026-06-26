package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class EmcRsPattern implements ICraftingPattern {
    private final ItemStack stack;
    private final EmcCraftingTarget target;
    private final ICraftingPatternContainer container;

    public EmcRsPattern(ItemStack stack, EmcCraftingTarget target, ICraftingPatternContainer container) {
        this.stack = stack.copy();
        this.target = target;
        this.container = container;
    }

    public EmcCraftingTarget target() {
        return target;
    }

    public ItemStack outputForQuantity(int quantity) {
        int outputCount = Math.toIntExact(Math.multiplyExact((long) target.output().getCount(), quantity));
        return ItemHandlerHelper.copyStackWithSize(target.output(), outputCount);
    }

    @Override
    public ICraftingPatternContainer getContainer() {
        return container;
    }

    @Override
    public ItemStack getStack() {
        return stack.copy();
    }

    @Override
    public boolean isValid() {
        return target.isValid();
    }

    @Nullable
    @Override
    public Component getErrorMessage() {
        return isValid() ? null : Component.translatable("message.emcfluid.pattern_too_large");
    }

    @Override
    public boolean isProcessing() {
        return true;
    }

    @Override
    public List<NonNullList<ItemStack>> getInputs() {
        return List.of();
    }

    @Override
    public NonNullList<ItemStack> getOutputs() {
        return NonNullList.of(ItemStack.EMPTY, target.output().copy());
    }

    @Override
    public ItemStack getOutput(NonNullList<ItemStack> took, RegistryAccess registryAccess) {
        return target.output().copy();
    }

    @Override
    public NonNullList<ItemStack> getByproducts() {
        return NonNullList.create();
    }

    @Override
    public NonNullList<ItemStack> getByproducts(NonNullList<ItemStack> took) {
        return NonNullList.create();
    }

    @Override
    public List<NonNullList<FluidStack>> getFluidInputs() {
        return target.fluidInputs().stream()
                .map(input -> NonNullList.of(FluidStack.EMPTY, input.stack()))
                .toList();
    }

    @Override
    public NonNullList<FluidStack> getFluidOutputs() {
        return NonNullList.create();
    }

    @Override
    public ResourceLocation getCraftingTaskFactoryId() {
        return EmcRsCraftingTaskFactory.ID;
    }

    @Override
    public boolean equals(Object obj) {
        boolean matches = obj instanceof EmcRsPattern other
                && target.info().equals(other.target.info())
                && target.fluidInputs().equals(other.target.fluidInputs())
                && target.tierConfigHash() == other.target.tierConfigHash()
                && container.getPosition().equals(other.container.getPosition());
        if (matches) {
            rememberResolvedTarget();
        }
        return matches;
    }

    @Override
    public int hashCode() {
        rememberResolvedTarget();
        return Objects.hash(target.info(), target.fluidInputs(), target.tierConfigHash(), container.getPosition());
    }

    private void rememberResolvedTarget() {
        // RS only passes the container to processing callbacks, so remember the pattern target during container lookup.
        if (container instanceof EmcCrafterNetworkNode node) {
            node.rememberResolvedTarget(target);
        }
    }
}
