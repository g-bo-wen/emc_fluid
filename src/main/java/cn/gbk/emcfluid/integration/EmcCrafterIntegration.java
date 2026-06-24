package cn.gbk.emcfluid.integration;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

public interface EmcCrafterIntegration {
    default void clearRemoved() {
    }

    default void onChunkUnloaded() {
    }

    default void setRemoved(boolean chunkUnloaded) {
    }

    default void invalidateCaps() {
    }

    default void load(CompoundTag tag) {
    }

    default void saveAdditional(CompoundTag tag) {
    }

    default void serverTick() {
    }

    default void refreshCraftingProviders() {
    }

    default ItemStack insertIntoNetwork(ItemStack stack) {
        return stack;
    }

    default <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return LazyOptional.empty();
    }
}
