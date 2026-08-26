package cn.gbk.emcfluid.integration.ae2;

import appeng.api.AEApi;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import xyz.phanta.ae2fc.item.ItemFluidDrop;
import xyz.phanta.ae2fc.item.ItemFluidPacket;

import java.util.List;
import java.util.Objects;

/**
 * A processing pattern whose item-shaped inputs are AE2FC Fluid Drops. One
 * Fluid Drop represents one mB, and IAEItemStack keeps the full long amount.
 */
public final class EmcAePattern implements ICraftingPatternDetails {
    private static final String NBT_TARGET = "AeTarget";
    private static final String NBT_EMC = "AeEmc";
    private static final String NBT_TIER_HASH = "AeTierHash";
    private static final String NBT_INPUTS = "AeInputs";

    private final EmcCraftingTarget target;
    private final ItemStack definition;
    private final IAEItemStack[] inputs;
    private final IAEItemStack[] outputs;
    private int priority;

    public EmcAePattern(EmcCraftingTarget target) {
        this.target = target;
        this.definition = createDefinition(target);
        this.inputs = createInputs(target.fluidInputs());
        IAEItemStack output = itemChannel().createStack(target.output());
        if (output == null) {
            throw new IllegalArgumentException("Unable to encode AE2 EMC output " + target.info());
        }
        this.outputs = new IAEItemStack[]{output};
    }

    public EmcCraftingTarget target() {
        return target;
    }

    @Override
    public ItemStack getPattern() {
        return definition.copy();
    }

    @Override
    public boolean isValidItemForSlot(int slotIndex, ItemStack itemStack, World world) {
        if (slotIndex < 0 || slotIndex >= target.fluidInputs().size()) {
            return false;
        }
        FluidStack supplied = fluidFromItem(itemStack);
        EmcFluidInput expected = target.fluidInputs().get(slotIndex);
        return supplied != null && expected.matches(supplied.getFluid())
                && supplied.amount == expected.amount();
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    @Override
    public IAEItemStack[] getInputs() {
        return copy(inputs);
    }

    @Override
    public IAEItemStack[] getCondensedInputs() {
        return copy(inputs);
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        return copy(outputs);
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return copy(outputs);
    }

    @Override
    public boolean canSubstitute() {
        return false;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting craftingInv, World world) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof EmcAePattern)) {
            return false;
        }
        EmcAePattern other = (EmcAePattern) object;
        return target.emcValue() == other.target.emcValue()
                && target.tierConfigHash() == other.target.tierConfigHash()
                && target.info().equals(other.target.info())
                && target.fluidInputs().equals(other.target.fluidInputs());
    }

    @Override
    public int hashCode() {
        return Objects.hash(target.info(), target.emcValue(),
                target.fluidInputs(), target.tierConfigHash());
    }

    static FluidStack fluidFromItem(ItemStack stack) {
        FluidStack fluid = ItemFluidDrop.getFluidStack(stack);
        return fluid != null ? fluid : ItemFluidPacket.getFluidStack(stack);
    }

    private static ItemStack createDefinition(EmcCraftingTarget target) {
        ItemStack stack = new ItemStack(ModContent.knowledgePattern);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(NBT_TARGET, target.info().writeToNBT(new NBTTagCompound()));
        tag.setLong(NBT_EMC, target.emcValue());
        tag.setInteger(NBT_TIER_HASH, target.tierConfigHash());
        NBTTagList inputs = new NBTTagList();
        for (EmcFluidInput input : target.fluidInputs()) {
            NBTTagCompound inputTag = new NBTTagCompound();
            inputTag.setInteger("Tier", input.tierIndex());
            inputTag.setInteger("Amount", input.amount());
            inputs.appendTag(inputTag);
        }
        tag.setTag(NBT_INPUTS, inputs);
        stack.setTagCompound(tag);
        return stack;
    }

    private static IAEItemStack[] createInputs(List<EmcFluidInput> fluidInputs) {
        IAEItemStack[] result = new IAEItemStack[fluidInputs.size()];
        for (int i = 0; i < fluidInputs.size(); i++) {
            IAEItemStack input = ItemFluidDrop.newAeStack(fluidInputs.get(i).stack());
            if (input == null) {
                throw new IllegalArgumentException("Unable to encode AE2FC EMC fluid input");
            }
            result[i] = input;
        }
        return result;
    }

    private static IAEItemStack[] copy(IAEItemStack[] stacks) {
        IAEItemStack[] result = new IAEItemStack[stacks.length];
        for (int i = 0; i < stacks.length; i++) {
            result[i] = stacks[i] == null ? null : stacks[i].copy();
        }
        return result;
    }

    private static IItemStorageChannel itemChannel() {
        return AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    }
}
