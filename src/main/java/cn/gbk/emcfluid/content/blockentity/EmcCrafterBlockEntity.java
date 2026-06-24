package cn.gbk.emcfluid.content.blockentity;

import cn.gbk.emcfluid.content.menu.EmcCrafterMenu;
import cn.gbk.emcfluid.integration.EmcCrafterIntegration;
import cn.gbk.emcfluid.integration.EmcCrafterIntegrations;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcCraftingTargets;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import cn.gbk.emcfluid.util.KnowledgePatternSync;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class EmcCrafterBlockEntity extends BlockEntity implements MenuProvider {
    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return KnowledgePatternData.isPattern(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            KnowledgePatternSync.update(EmcCrafterBlockEntity.this);
            targetCacheDirty = true;
            refreshCraftingProviders();
        }
    };
    private final ItemStackHandler outputCache = new ItemStackHandler(9) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<ItemStackHandler> itemCapability = LazyOptional.of(() -> items);
    private final List<EmcCrafterIntegration> integrations = EmcCrafterIntegrations.create(this);

    private boolean chunkUnloaded;
    private List<EmcCraftingTarget> cachedTargets = List.of();
    private int cachedPatternHash;
    private int cachedTierHash;
    private int cachedKnowledgeVersion;
    private boolean targetCacheDirty = true;
    private int outputFlushDelay;

    public EmcCrafterBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.EMC_CRAFTER_BE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EmcCrafterBlockEntity blockEntity) {
        if (!level.isClientSide) {
            blockEntity.refreshCraftingProvidersIfTierConfigChanged();
            blockEntity.integrations.forEach(EmcCrafterIntegration::serverTick);
            blockEntity.flushOutputCache();
        }
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        chunkUnloaded = false;
        if (level != null && !level.isClientSide) {
            integrations.forEach(EmcCrafterIntegration::clearRemoved);
            KnowledgePatternSync.update(this);
        }
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        chunkUnloaded = true;
        KnowledgePatternSync.unregister(this);
        integrations.forEach(EmcCrafterIntegration::onChunkUnloaded);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        integrations.forEach(integration -> integration.setRemoved(chunkUnloaded));
        KnowledgePatternSync.unregister(this);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemCapability.invalidate();
        integrations.forEach(EmcCrafterIntegration::invalidateCaps);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        items.deserializeNBT(tag.getCompound("Items"));
        outputCache.deserializeNBT(tag.getCompound("OutputCache"));
        integrations.forEach(integration -> integration.load(tag));
        targetCacheDirty = true;
        KnowledgePatternSync.update(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("Items", items.serializeNBT());
        tag.put("OutputCache", outputCache.serializeNBT());
        integrations.forEach(integration -> integration.saveAdditional(tag));
    }

    public List<EmcCraftingTarget> getTargets() {
        ItemStack pattern = items.getStackInSlot(0);
        int patternHash = patternHash();
        int tierHash = EmcFluidTierConfig.hash();
        int knowledgeVersion = knowledgeVersion(pattern);
        if (targetCacheDirty || cachedPatternHash != patternHash || cachedTierHash != tierHash
                || cachedKnowledgeVersion != knowledgeVersion) {
            cachedTargets = EmcCraftingTargets.fromPattern(pattern);
            cachedPatternHash = patternHash;
            cachedTierHash = tierHash;
            cachedKnowledgeVersion = knowledgeVersion;
            targetCacheDirty = false;
        }
        return cachedTargets;
    }

    public ItemStackHandler getItems() {
        return items;
    }

    public ItemStackHandler getOutputCache() {
        return outputCache;
    }

    public void refreshCraftingProviders() {
        if (level != null && !level.isClientSide) {
            integrations.forEach(EmcCrafterIntegration::refreshCraftingProviders);
        }
    }

    public void refreshForKnowledgeOwner(UUID owner) {
        if (!KnowledgePatternData.isBoundTo(items.getStackInSlot(0), owner)) {
            KnowledgePatternSync.update(this);
            return;
        }
        targetCacheDirty = true;
        getTargets();
        refreshCraftingProviders();
        setChanged();
    }

    private void refreshCraftingProvidersIfTierConfigChanged() {
        int tierHash = EmcFluidTierConfig.hash();
        if (cachedTierHash != 0 && cachedTierHash != tierHash) {
            targetCacheDirty = true;
            getTargets();
            refreshCraftingProviders();
        }
    }

    public boolean queueOutput(ItemStack output, int flushDelay) {
        if (!insertIntoOutputCache(output, true)) {
            return false;
        }
        if (!insertIntoOutputCache(output, false)) {
            return false;
        }
        outputFlushDelay = Math.max(outputFlushDelay, flushDelay);
        return true;
    }

    public boolean insertIntoOutputCache(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < outputCache.getSlots() && !remaining.isEmpty(); i++) {
            remaining = outputCache.insertItem(i, remaining, simulate);
        }
        return remaining.isEmpty();
    }

    private void flushOutputCache() {
        if (outputFlushDelay > 0) {
            outputFlushDelay--;
            return;
        }
        for (int i = 0; i < outputCache.getSlots(); i++) {
            ItemStack remaining = outputCache.getStackInSlot(i);
            if (remaining.isEmpty()) {
                continue;
            }
            for (EmcCrafterIntegration integration : integrations) {
                remaining = integration.insertIntoNetwork(remaining);
                if (remaining.isEmpty()) {
                    break;
                }
            }
            outputCache.setStackInSlot(i, remaining);
        }
    }

    private int patternHash() {
        ItemStack pattern = items.getStackInSlot(0);
        if (pattern.isEmpty()) {
            return 0;
        }
        return pattern.save(new CompoundTag()).hashCode();
    }

    private int knowledgeVersion(ItemStack pattern) {
        return KnowledgePatternData.getOwner(pattern)
                .map(KnowledgePatternSync::getKnowledgeVersion)
                .orElse(0);
    }

    public boolean isBusy() {
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.emcfluid.emc_crafter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EmcCrafterMenu(containerId, inventory, this);
    }

    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemCapability.cast();
        }
        for (EmcCrafterIntegration integration : integrations) {
            LazyOptional<T> capability = integration.getCapability(cap, side);
            if (capability.isPresent()) {
                return capability;
            }
        }
        return super.getCapability(cap, side);
    }
}
