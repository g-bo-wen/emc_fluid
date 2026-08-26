package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.integration.EmcCrafterIntegration;
import cn.gbk.emcfluid.integration.EmcCrafterIntegrations;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcCraftingTargets;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import cn.gbk.emcfluid.util.KnowledgePatternSync;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class EmcCrafterBlockEntity extends MachineTileEntity {
    private final ItemStackHandler pattern = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return !stack.isEmpty() && stack.getItem() == ModContent.knowledgePattern;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndNotify();
            targetCacheDirty = true;
            KnowledgePatternSync.update(EmcCrafterBlockEntity.this);
            refreshCraftingProviders();
        }
    };

    private List<EmcCraftingTarget> cachedTargets = Collections.emptyList();
    private int cachedPatternHash;
    private int cachedTierHash;
    private int cachedKnowledgeVersion;
    private boolean targetCacheDirty = true;
    private List<EmcCrafterIntegration> integrations = Collections.emptyList();
    private boolean integrationsCreated;
    private boolean chunkUnloading;
    private final ItemStackHandler outputCache = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            markDirtyAndNotify();
        }
    };

    public ItemStackHandler getPattern() {
        return pattern;
    }

    public ItemStackHandler getItems() {
        return pattern;
    }

    public ItemStackHandler getOutputCache() {
        return outputCache;
    }

    public boolean queueOutput(ItemStack output, boolean simulate) {
        if (output.isEmpty()) {
            return true;
        }
        ItemStack remaining = output.copy();
        for (int slot = 0; slot < outputCache.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = outputCache.insertItem(slot, remaining, true);
        }
        if (!remaining.isEmpty() || simulate) {
            return remaining.isEmpty();
        }
        remaining = output.copy();
        for (int slot = 0; slot < outputCache.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = outputCache.insertItem(slot, remaining, false);
        }
        return remaining.isEmpty();
    }

    public List<EmcCraftingTarget> getTargets() {
        ItemStack patternStack = pattern.getStackInSlot(0);
        int patternHash = patternHash(patternStack);
        int tierHash = EmcFluidTierConfig.hash();
        int knowledgeVersion = knowledgeVersion(patternStack);
        if (targetCacheDirty || cachedPatternHash != patternHash || cachedTierHash != tierHash
                || cachedKnowledgeVersion != knowledgeVersion) {
            cachedTargets = EmcCraftingTargets.fromPattern(patternStack);
            cachedPatternHash = patternHash;
            cachedTierHash = tierHash;
            cachedKnowledgeVersion = knowledgeVersion;
            targetCacheDirty = false;
        }
        return cachedTargets;
    }

    public void refreshForKnowledgeOwner(UUID owner) {
        if (!KnowledgePatternData.isBoundTo(pattern.getStackInSlot(0), owner)) {
            KnowledgePatternSync.update(this);
            return;
        }
        targetCacheDirty = true;
        getTargets();
        refreshCraftingProviders();
        markDirtyAndNotify();
    }

    public void refreshCraftingProviders() {
        for (EmcCrafterIntegration integration : integrations()) {
            integration.refreshCraftingProviders();
        }
    }

    public boolean isBusy() {
        for (EmcCrafterIntegration integration : integrations()) {
            if (integration.isBusy()) {
                return true;
            }
        }
        return false;
    }

    public void onBlockBroken() {
        for (EmcCrafterIntegration integration : integrations()) {
            integration.onBlockBroken();
        }
    }

    @Override
    public List<ItemStackHandler> getDroppableItemHandlers() {
        return Arrays.asList(pattern, outputCache);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setTag("Items", pattern.serializeNBT());
        compound.setTag("OutputCache", outputCache.serializeNBT());
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        pattern.deserializeNBT(compound.getCompoundTag("Items"));
        outputCache.deserializeNBT(compound.getCompoundTag("OutputCache"));
        sanitizeItemHandler(pattern);
        sanitizeItemHandler(outputCache);
        targetCacheDirty = true;
        KnowledgePatternSync.update(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        chunkUnloading = false;
        targetCacheDirty = true;
        KnowledgePatternSync.update(this);
        for (EmcCrafterIntegration integration : integrations()) {
            integration.onLoad();
        }
    }

    @Override
    public void onChunkUnload() {
        chunkUnloading = true;
        KnowledgePatternSync.unregister(this);
        for (EmcCrafterIntegration integration : integrations()) {
            integration.onChunkUnload();
        }
        super.onChunkUnload();
    }

    @Override
    public void invalidate() {
        KnowledgePatternSync.unregister(this);
        if (!chunkUnloading) {
            for (EmcCrafterIntegration integration : integrations()) {
                integration.invalidate();
            }
        }
        super.invalidate();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return true;
        }
        for (EmcCrafterIntegration integration : integrations()) {
            if (integration.hasCapability(capability, facing)) {
                return true;
            }
        }
        return super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) pattern;
        }
        for (EmcCrafterIntegration integration : integrations()) {
            T value = integration.getCapability(capability, facing);
            if (value != null) {
                return value;
            }
        }
        return super.getCapability(capability, facing);
    }

    private List<EmcCrafterIntegration> integrations() {
        if (!integrationsCreated) {
            integrationsCreated = true;
            integrations = EmcCrafterIntegrations.create(this);
        }
        return integrations;
    }

    private static int patternHash(ItemStack patternStack) {
        return patternStack.isEmpty() ? 0 : patternStack.serializeNBT().hashCode();
    }

    private static int knowledgeVersion(ItemStack patternStack) {
        java.util.Optional<UUID> owner = KnowledgePatternData.getOwner(patternStack);
        return owner.isPresent() ? KnowledgePatternSync.getKnowledgeVersion(owner.get()) : 0;
    }
}
