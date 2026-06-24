package cn.gbk.emcfluid.integration.ae2;

import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import appeng.api.crafting.IPatternDetails;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

public class EmcAePattern implements IPatternDetails {
    private final EmcCraftingTarget target;
    private final AEItemKey definition;
    private final IInput[] inputs;
    private final GenericStack[] outputs;

    public EmcAePattern(EmcCraftingTarget target) {
        this.target = target;
        ItemStack definitionStack = ModContent.KNOWLEDGE_PATTERN.get().getDefaultInstance();
        CompoundTag tag = definitionStack.getOrCreateTag();
        tag.put("AeTarget", target.info().write(new CompoundTag()));
        this.definition = AEItemKey.of(definitionStack);
        this.inputs = target.fluidInputs().stream().map(EmcInput::new).toArray(IInput[]::new);
        this.outputs = new GenericStack[]{Objects.requireNonNull(GenericStack.fromItemStack(target.output()))};
    }

    public EmcCraftingTarget target() {
        return target;
    }

    @Override
    public AEItemKey getDefinition() {
        return definition;
    }

    @Override
    public IInput[] getInputs() {
        return inputs;
    }

    @Override
    public GenericStack[] getOutputs() {
        return outputs;
    }

    @Override
    public boolean supportsPushInputsToExternalInventory() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EmcAePattern other
                && target.info().equals(other.target.info())
                && target.fluidInputs().equals(other.target.fluidInputs())
                && target.tierConfigHash() == other.target.tierConfigHash();
    }

    @Override
    public int hashCode() {
        return Objects.hash(target.info(), target.fluidInputs(), target.tierConfigHash());
    }

    private record EmcInput(GenericStack[] possibleInputs) implements IInput {
        private EmcInput(EmcFluidInput input) {
            this(new GenericStack[]{new GenericStack(AEFluidKey.of(ModContent.getEmcFluidSource(input.tierIndex()).get()), input.amount())});
        }

        @Override
        public GenericStack[] getPossibleInputs() {
            return possibleInputs;
        }

        @Override
        public long getMultiplier() {
            return 1;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            return Arrays.stream(possibleInputs).anyMatch(stack -> stack.what().equals(input));
        }

        @Nullable
        @Override
        public AEKey getRemainingKey(AEKey template) {
            return null;
        }
    }
}
