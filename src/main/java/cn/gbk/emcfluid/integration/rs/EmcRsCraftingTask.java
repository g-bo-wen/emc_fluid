package cn.gbk.emcfluid.integration.rs;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.craftingmonitor.ICraftingMonitorElement;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class EmcRsCraftingTask implements ICraftingTask {
    private static final String NBT_CHILD_TASK_IDS = "ChildTaskIds";
    private static final String NBT_CHILD_TASK_ID = "Id";

    private final INetwork network;
    private final ICraftingRequestInfo requested;
    private final int quantity;
    private final EmcRsPattern pattern;
    private final UUID id;
    private final long startTime;
    private final Set<UUID> childTaskIds = new HashSet<>();
    private boolean started;

    public EmcRsCraftingTask(INetwork network, ICraftingRequestInfo requested, int quantity, EmcRsPattern pattern) {
        this.network = network;
        this.requested = requested;
        this.quantity = quantity;
        this.pattern = pattern;
        this.id = UUID.randomUUID();
        this.startTime = System.currentTimeMillis();
    }

    public EmcRsCraftingTask(INetwork network, ICraftingRequestInfo requested, int quantity, EmcRsPattern pattern, CompoundTag tag) {
        this.network = network;
        this.requested = requested;
        this.quantity = quantity;
        this.pattern = pattern;
        this.id = tag.hasUUID("Id") ? tag.getUUID("Id") : UUID.randomUUID();
        this.startTime = tag.getLong("StartTime");
        this.started = tag.getBoolean("Started");
        ListTag childTasks = tag.getList(NBT_CHILD_TASK_IDS, Tag.TAG_COMPOUND);
        for (int i = 0; i < childTasks.size(); i++) {
            CompoundTag childTask = childTasks.getCompound(i);
            if (childTask.hasUUID(NBT_CHILD_TASK_ID)) {
                childTaskIds.add(childTask.getUUID(NBT_CHILD_TASK_ID));
            }
        }
    }

    @Override
    public boolean update() {
        if (!started) {
            return false;
        }
        List<FluidStack> requiredFluids;
        try {
            requiredFluids = pattern.target().fluidStacksForQuantity(quantity);
        } catch (ArithmeticException e) {
            return true;
        }
        ItemStack output;
        try {
            output = pattern.outputForQuantity(quantity);
        } catch (ArithmeticException e) {
            return true;
        }
        if (!canStoreOutput(output)) {
            return false;
        }
        if (!requestMissingFluids(requiredFluids)) {
            return false;
        }
        List<FluidStack> extractedFluids = new ArrayList<>();
        for (FluidStack required : requiredFluids) {
            FluidStack extracted = network.extractFluid(required, required.getAmount(), Action.PERFORM);
            if (extracted.getAmount() != required.getAmount()) {
                if (!extracted.isEmpty()) {
                    extractedFluids.add(extracted);
                }
                refundFluids(extractedFluids);
                return false;
            }
            extractedFluids.add(extracted);
        }
        if (storeOutput(output)) {
            return true;
        }
        refundFluids(extractedFluids);
        return false;
    }

    private boolean canStoreOutput(ItemStack output) {
        return network.insertItem(output, output.getCount(), Action.SIMULATE).isEmpty()
                || pattern.getContainer() instanceof EmcCrafterNetworkNode node
                && node.canCacheOutput(output, output.getCount());
    }

    private boolean storeOutput(ItemStack output) {
        if (!network.insertItem(output, output.getCount(), Action.SIMULATE).isEmpty()) {
            return pattern.getContainer() instanceof EmcCrafterNetworkNode node && node.cacheOutput(output);
        }
        ItemStack remainder = network.insertItem(output, output.getCount(), Action.PERFORM);
        if (remainder.isEmpty()) {
            return true;
        }
        return pattern.getContainer() instanceof EmcCrafterNetworkNode node && node.cacheOutput(remainder);
    }

    private boolean requestMissingFluids(List<FluidStack> requiredFluids) {
        boolean hasAllFluids = true;
        for (FluidStack required : requiredFluids) {
            int available = network.extractFluid(required, required.getAmount(), Action.SIMULATE).getAmount();
            int missing = required.getAmount() - available;
            if (missing <= 0) {
                continue;
            }
            hasAllFluids = false;
            if (network.getCraftingManager().getPattern(required) != null) {
                ICraftingTask childTask = network.getCraftingManager().request(this, required, missing);
                if (childTask != null) {
                    childTaskIds.add(childTask.getId());
                }
            }
        }
        return hasAllFluids;
    }

    private void refundFluids(List<FluidStack> fluids) {
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                network.insertFluid(fluid, fluid.getAmount(), Action.PERFORM);
            }
        }
    }

    @Override
    public void onCancelled() {
        for (UUID childTaskId : childTaskIds) {
            network.getCraftingManager().cancel(childTaskId);
        }
    }

    @Override
    public int getQuantity() {
        return quantity;
    }

    @Override
    public int getCompletionPercentage() {
        return started ? 50 : 0;
    }

    @Override
    public ICraftingRequestInfo getRequested() {
        return requested;
    }

    @Override
    public int onTrackedInsert(ItemStack stack, int size) {
        return size;
    }

    @Override
    public int onTrackedInsert(FluidStack stack, int size) {
        return size;
    }

    @Override
    public CompoundTag writeToNbt(CompoundTag tag) {
        tag.putUUID("Id", id);
        tag.putLong("StartTime", startTime);
        tag.putInt("Quantity", quantity);
        tag.putBoolean("Started", started);
        tag.put("Requested", requested.writeToNbt());
        tag.put("Target", pattern.target().info().write(new CompoundTag()));
        tag.putLong("EmcValue", pattern.target().emcValue());
        ListTag childTasks = new ListTag();
        for (UUID childTaskId : childTaskIds) {
            CompoundTag childTask = new CompoundTag();
            childTask.putUUID(NBT_CHILD_TASK_ID, childTaskId);
            childTasks.add(childTask);
        }
        tag.put(NBT_CHILD_TASK_IDS, childTasks);
        return tag;
    }

    @Override
    public List<ICraftingMonitorElement> getCraftingMonitorElements() {
        return List.of();
    }

    @Override
    public ICraftingPattern getPattern() {
        return pattern;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public void start() {
        started = true;
    }
}
