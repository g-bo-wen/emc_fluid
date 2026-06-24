package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.util.CostResolver;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import cn.gbk.emcfluid.util.ProjectEAccess;
import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.preview.ICraftingPreviewElement;
import com.refinedmods.refinedstorage.api.autocrafting.task.CalculationResultType;
import com.refinedmods.refinedstorage.api.autocrafting.task.CraftingTaskReadException;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICalculationResult;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTaskFactory;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.preview.FluidCraftingPreviewElement;
import com.refinedmods.refinedstorage.apiimpl.autocrafting.preview.ItemCraftingPreviewElement;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

public class EmcRsCraftingTaskFactory implements ICraftingTaskFactory {
    public static final ResourceLocation ID = new ResourceLocation(EmcFluid.MODID, "emc");

    @Override
    public ICalculationResult create(INetwork network, ICraftingRequestInfo requested, int quantity, ICraftingPattern pattern) {
        if (!(pattern instanceof EmcRsPattern emcPattern)) {
            return new EmcRsCalculationResult(CalculationResultType.NO_PATTERN, null);
        }
        List<FluidStack> requiredFluids;
        try {
            requiredFluids = emcPattern.target().fluidStacksForQuantity(quantity);
        } catch (ArithmeticException e) {
            return new EmcRsCalculationResult(CalculationResultType.TOO_COMPLEX, null);
        }
        List<ICraftingPreviewElement> previewElements = new ArrayList<>();
        int outputCount;
        try {
            outputCount = Math.toIntExact(Math.multiplyExact((long) emcPattern.target().output().getCount(), quantity));
        } catch (ArithmeticException e) {
            return new EmcRsCalculationResult(CalculationResultType.TOO_COMPLEX, null);
        }
        ItemStack outputIcon = ItemHandlerHelper.copyStackWithSize(emcPattern.target().output(), 1);
        previewElements.add(new ItemCraftingPreviewElement(outputIcon, 0, false, outputCount));
        boolean missingFluid = false;
        for (FluidStack required : requiredFluids) {
            FluidStack extracted = network.extractFluid(required, required.getAmount(), Action.SIMULATE);
            int available = extracted.getAmount();
            int missing = required.getAmount() - available;
            previewElements.add(new FluidCraftingPreviewElement(required, available, missing > 0, Math.max(0, missing)));
            missingFluid |= missing > 0;
        }
        if (missingFluid) {
            return new EmcRsCalculationResult(CalculationResultType.MISSING, previewElements, null);
        }
        boolean networkAcceptsOutput = network.insertItem(emcPattern.target().output(), quantity, Action.SIMULATE).isEmpty();
        boolean cacheAcceptsOutput = emcPattern.getContainer() instanceof EmcCrafterNetworkNode node
                && node.canCacheOutput(emcPattern.target().output(), quantity);
        if (!networkAcceptsOutput && !cacheAcceptsOutput) {
            return new EmcRsCalculationResult(CalculationResultType.MISSING, previewElements, null);
        }
        return new EmcRsCalculationResult(CalculationResultType.OK, previewElements, new EmcRsCraftingTask(network, requested, quantity, emcPattern));
    }

    @Override
    public ICraftingTask createFromNbt(INetwork network, CompoundTag tag) throws CraftingTaskReadException {
        try {
            ICraftingRequestInfo requested = RsIntegration.API.createCraftingRequestInfo(tag.getCompound("Requested"));
            ItemInfo info = ItemInfo.read(tag.getCompound("Target"));
            if (info == null) {
                throw new CraftingTaskReadException("Missing target");
            }
            long emc = ProjectEAccess.getEmcValue(info);
            long storedEmc = tag.contains("EmcValue") ? tag.getLong("EmcValue") : emc;
            if (emc != storedEmc) {
                throw new CraftingTaskReadException("EMC value changed");
            }
            var inputs = CostResolver.resolve(emc)
                    .orElseThrow(() -> new CraftingTaskReadException("Could not resolve EMC fluid inputs"));
            EmcCraftingTarget target = new EmcCraftingTarget(info, info.createStack(), emc, inputs, EmcFluidTierConfig.hash());
            EmcRsPattern pattern = new EmcRsPattern(net.minecraft.world.item.ItemStack.EMPTY, target, new RestoredPatternContainer(network));
            return new EmcRsCraftingTask(network, requested, tag.getInt("Quantity"), pattern, tag);
        } catch (Exception e) {
            throw new CraftingTaskReadException("Could not read EMC crafting task: " + e.getMessage());
        }
    }
}
