package cn.gbk.emcfluid.integration;

import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;

/**
 * Hooks used by the EMC Crafter core. Optional integrations implement this
 * interface in packages that are only loaded when their mod is present.
 */
public interface EmcCrafterIntegration {
    void onLoad();

    void onChunkUnload();

    void onBlockBroken();

    void invalidate();

    void refreshCraftingProviders();

    boolean isBusy();

    boolean hasCapability(Capability<?> capability, @Nullable EnumFacing side);

    @Nullable
    <T> T getCapability(Capability<T> capability, @Nullable EnumFacing side);
}
