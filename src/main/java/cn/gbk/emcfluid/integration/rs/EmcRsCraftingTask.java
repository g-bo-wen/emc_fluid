package cn.gbk.emcfluid.integration.rs;

import com.refinedmods.refinedstorage.api.autocrafting.ICraftingPattern;
import com.refinedmods.refinedstorage.api.autocrafting.craftingmonitor.ICraftingMonitorElement;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.refinedmods.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.refinedmods.refinedstorage.api.network.INetwork;
import com.refinedmods.refinedstorage.api.util.Action;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmcRsCraftingTask implements ICraftingTask {
    private final INetwork network;
    private final ICraftingRequestInfo requested;
    private final int quantity;
    private final EmcRsPattern pattern;
    private final UUID id;
    private final long startTime;
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
        ItemStack output = pattern.target().output();
        boolean networkAcceptsOutput = network.insertItem(output, quantity, Action.SIMULATE).isEmpty();
        boolean cacheAcceptsOutput = pattern.getContainer() instanceof EmcCrafterNetworkNode node
                && node.canCacheOutput(output, quantity);
        if (!networkAcceptsOutput && !cacheAcceptsOutput) {
            return false;
        }
        for (FluidStack required : requiredFluids) {
            if (network.extractFluid(required, required.getAmount(), Action.SIMULATE).getAmount() != required.getAmount()) {
                return false;
            }
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
        ItemStack remainder = network.insertItem(output, quantity, Action.PERFORM);
        if (remainder.isEmpty()) {
            return true;
        }
        return pattern.getContainer() instanceof EmcCrafterNetworkNode node && node.cacheOutput(remainder);
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
