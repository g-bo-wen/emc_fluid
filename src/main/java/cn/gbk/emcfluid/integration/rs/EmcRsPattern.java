package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.EmcStackIdentity;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmcRsPattern implements ICraftingPattern {
    private static final String NBT_TARGET = "Target";
    private static final String NBT_EMC = "Emc";
    private static final String NBT_TIER_HASH = "TierHash";
    private static final String NBT_INPUTS = "Inputs";
    private static final String NBT_INPUT_TIER = "Tier";
    private static final String NBT_INPUT_AMOUNT = "Amount";

    private final ItemStack stack;
    private final EmcCraftingTarget target;
    private final ICraftingPatternContainer container;

    EmcRsPattern(EmcCraftingTarget target, ICraftingPatternContainer container) {
        this(createStack(target), target, container);
    }

    private EmcRsPattern(ItemStack stack, EmcCraftingTarget target,
                         ICraftingPatternContainer container) {
        this.stack = stack.copy();
        this.target = target;
        this.container = container;
    }

    static EmcRsPattern fromStack(ItemStack stack, ICraftingPatternContainer container) {
        if (stack.isEmpty() || stack.getItem() != RsIntegration.virtualPatternItem
                || !stack.hasTagCompound()) {
            throw new IllegalArgumentException("Invalid EMC Fluid RS virtual pattern");
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.hasKey(NBT_TARGET, Constants.NBT.TAG_COMPOUND)
                || !tag.hasKey(NBT_INPUTS, Constants.NBT.TAG_LIST)) {
            throw new IllegalArgumentException("Incomplete EMC Fluid RS virtual pattern");
        }

        EmcStackIdentity identity = EmcStackIdentity.readFromNBT(tag.getCompoundTag(NBT_TARGET));
        ItemStack output = identity.createStack();
        if (output.isEmpty()) {
            throw new IllegalArgumentException("EMC Fluid RS virtual pattern output no longer exists");
        }

        List<EmcFluidInput> inputs = new ArrayList<EmcFluidInput>();
        NBTTagList inputTags = tag.getTagList(NBT_INPUTS, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < inputTags.tagCount(); i++) {
            NBTTagCompound inputTag = inputTags.getCompoundTagAt(i);
            inputs.add(new EmcFluidInput(
                    inputTag.getInteger(NBT_INPUT_TIER),
                    inputTag.getInteger(NBT_INPUT_AMOUNT)));
        }
        EmcCraftingTarget target = new EmcCraftingTarget(
                identity, output, tag.getLong(NBT_EMC), inputs, tag.getInteger(NBT_TIER_HASH));
        return new EmcRsPattern(stack, target, container);
    }

    private static ItemStack createStack(EmcCraftingTarget target) {
        if (RsIntegration.virtualPatternItem == null) {
            throw new IllegalStateException("RS virtual pattern item is not registered");
        }
        ItemStack stack = new ItemStack(RsIntegration.virtualPatternItem);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(NBT_TARGET, target.info().writeToNBT(new NBTTagCompound()));
        tag.setLong(NBT_EMC, target.emcValue());
        tag.setInteger(NBT_TIER_HASH, target.tierConfigHash());
        NBTTagList inputs = new NBTTagList();
        for (EmcFluidInput input : target.fluidInputs()) {
            NBTTagCompound inputTag = new NBTTagCompound();
            inputTag.setInteger(NBT_INPUT_TIER, input.tierIndex());
            inputTag.setInteger(NBT_INPUT_AMOUNT, input.amount());
            inputs.appendTag(inputTag);
        }
        tag.setTag(NBT_INPUTS, inputs);
        stack.setTagCompound(tag);
        return stack;
    }

    EmcCraftingTarget target() {
        return target;
    }

    boolean isAuthorized() {
        return container instanceof EmcCrafterNetworkNode
                && ((EmcCrafterNetworkNode) container).isTargetAuthorized(target);
    }

    @Override
    public ICraftingPatternContainer getContainer() {
        if (container instanceof EmcCrafterNetworkNode) {
            ((EmcCrafterNetworkNode) container).selectPattern(this);
        }
        return container;
    }

    @Override
    public ItemStack getStack() {
        return stack.copy();
    }

    @Override
    public boolean isValid() {
        return target.isValid() && isAuthorized();
    }

    @Override
    public boolean isProcessing() {
        return true;
    }

    @Override
    public boolean isOredict() {
        return false;
    }

    @Override
    public List<NonNullList<ItemStack>> getInputs() {
        return Collections.emptyList();
    }

    @Override
    public NonNullList<ItemStack> getOutputs() {
        NonNullList<ItemStack> outputs = NonNullList.create();
        outputs.add(target.output());
        return outputs;
    }

    @Override
    public ItemStack getOutput(NonNullList<ItemStack> took) {
        return target.output();
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
    public NonNullList<FluidStack> getFluidInputs() {
        NonNullList<FluidStack> inputs = NonNullList.create();
        for (EmcFluidInput input : target.fluidInputs()) {
            inputs.add(input.stack());
        }
        return inputs;
    }

    @Override
    public NonNullList<FluidStack> getFluidOutputs() {
        return NonNullList.create();
    }

    @Override
    public String getId() {
        return RsIntegration.TASK_ID;
    }

    @Override
    public boolean canBeInChainWith(ICraftingPattern other) {
        if (!(other instanceof EmcRsPattern)) {
            return false;
        }
        EmcRsPattern pattern = (EmcRsPattern) other;
        return target.info().equals(pattern.target.info())
                && target.fluidInputs().equals(pattern.target.fluidInputs())
                && target.tierConfigHash() == pattern.target.tierConfigHash()
                && target.emcValue() == pattern.target.emcValue();
    }

    @Override
    public int getChainHashCode() {
        int result = target.info().hashCode();
        result = 31 * result + target.fluidInputs().hashCode();
        result = 31 * result + target.tierConfigHash();
        result = 31 * result + (int) (target.emcValue() ^ (target.emcValue() >>> 32));
        return result;
    }
}
