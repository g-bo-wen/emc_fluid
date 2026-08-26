package cn.gbk.emcfluid.integration.rs;

import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.registry.ICraftingTaskFactory;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.CraftingTaskReadException;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nonnull;

final class EmcRsCraftingTaskFactory implements ICraftingTaskFactory {
    @Nonnull
    @Override
    public ICraftingTask create(INetwork network, ICraftingRequestInfo requested,
                                int quantity, ICraftingPattern pattern) {
        return new EmcRsCraftingTask(network, requested, quantity, pattern);
    }

    @Override
    public ICraftingTask createFromNbt(INetwork network, NBTTagCompound tag)
            throws CraftingTaskReadException {
        return new EmcRsCraftingTask(network, tag);
    }
}
