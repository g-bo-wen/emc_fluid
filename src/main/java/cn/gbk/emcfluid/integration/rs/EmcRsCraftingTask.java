package cn.gbk.emcfluid.integration.rs;

import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.craftingmonitor.ICraftingMonitorElement;
import com.raoulvdberge.refinedstorage.api.autocrafting.preview.ICraftingPreviewElement;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.CraftingTaskErrorType;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.CraftingTaskReadException;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTaskError;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.util.IStackList;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.task.CraftingTask;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.task.CraftingTaskError;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Delegates calculation and execution to RS 1.6.16's proven task engine while
 * adding a fail-closed authorization check for ProjectE knowledge/config
 * changes. The delegate owns recursive calculation, missing previews,
 * cancellation refunds and full task NBT state.
 */
final class EmcRsCraftingTask implements ICraftingTask {
    private final CraftingTask delegate;
    private boolean cancelledForInvalidTarget;

    EmcRsCraftingTask(INetwork network, ICraftingRequestInfo requested,
                      int quantity, ICraftingPattern pattern) {
        this.delegate = new CraftingTask(network, requested, quantity, pattern);
    }

    EmcRsCraftingTask(INetwork network, NBTTagCompound tag)
            throws CraftingTaskReadException {
        try {
            this.delegate = new CraftingTask(network, tag);
        } catch (RuntimeException exception) {
            throw new CraftingTaskReadException(
                    "Could not restore EMC Fluid crafting task: " + exception.getMessage());
        }
    }

    @Nullable
    @Override
    public ICraftingTaskError calculate() {
        if (!isAuthorized()) {
            return new CraftingTaskError(CraftingTaskErrorType.TOO_COMPLEX);
        }
        return delegate.calculate();
    }

    @Override
    public boolean update() {
        if (!isAuthorized()) {
            if (!cancelledForInvalidTarget) {
                delegate.onCancelled();
                cancelledForInvalidTarget = true;
            }
            return true;
        }
        return delegate.update();
    }

    private boolean isAuthorized() {
        ICraftingPattern pattern = delegate.getPattern();
        return pattern instanceof EmcRsPattern && ((EmcRsPattern) pattern).isAuthorized();
    }

    @Override
    public void onCancelled() {
        if (!cancelledForInvalidTarget) {
            delegate.onCancelled();
            cancelledForInvalidTarget = true;
        }
    }

    @Override
    public int getQuantity() {
        return delegate.getQuantity();
    }

    @Override
    public int getQuantityPerCraft() {
        return delegate.getQuantityPerCraft();
    }

    @Override
    public int getCompletionPercentage() {
        return delegate.getCompletionPercentage();
    }

    @Override
    public ICraftingRequestInfo getRequested() {
        return delegate.getRequested();
    }

    @Override
    public int onTrackedInsert(ItemStack stack, int size) {
        return delegate.onTrackedInsert(stack, size);
    }

    @Override
    public int onTrackedInsert(FluidStack stack, int size) {
        return delegate.onTrackedInsert(stack, size);
    }

    @Override
    public NBTTagCompound writeToNbt(NBTTagCompound tag) {
        return delegate.writeToNbt(tag);
    }

    @Override
    public List<ICraftingMonitorElement> getCraftingMonitorElements() {
        return delegate.getCraftingMonitorElements();
    }

    @Override
    public List<ICraftingPreviewElement> getPreviewStacks() {
        return delegate.getPreviewStacks();
    }

    @Override
    public ICraftingPattern getPattern() {
        return delegate.getPattern();
    }

    @Override
    public long getExecutionStarted() {
        return delegate.getExecutionStarted();
    }

    @Override
    public IStackList<ItemStack> getMissing() {
        return delegate.getMissing();
    }

    @Override
    public IStackList<FluidStack> getMissingFluids() {
        return delegate.getMissingFluids();
    }

    @Override
    public UUID getId() {
        return delegate.getId();
    }
}
