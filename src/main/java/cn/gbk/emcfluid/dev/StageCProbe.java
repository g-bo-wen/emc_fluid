package cn.gbk.emcfluid.dev;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.MachineTileEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import cn.gbk.emcfluid.content.menu.EmcCrafterMenu;
import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import cn.gbk.emcfluid.gui.ModGuiHandler;
import cn.gbk.emcfluid.network.ChangeConvertLiquefierTierPacket;
import cn.gbk.emcfluid.network.ToggleConvertLiquefierModePacket;
import cn.gbk.emcfluid.network.ToggleConverterModePacket;
import cn.gbk.emcfluid.network.ToggleLiquefierModePacket;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import cn.gbk.emcfluid.util.ProjectEAccess;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.items.ItemStackHandler;
import moze_intel.projecte.api.item.IItemEmc;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public final class StageCProbe {
    private static final int TEST_MB = 2;
    private static final Item LARGE_EMC_HOLDER = new ProbeEmcItem();

    private StageCProbe() {
    }

    public static void run(MinecraftServer server) {
        validateRecipes();
        validateStandalonePersistenceAndCache();

        WorldServer world = server.getWorld(0);
        BlockPos top = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());
        int y = Math.min(world.getHeight() - 2, top.getY() + 4);
        BlockPos pos = new BlockPos(top.getX(), y, top.getZ());
        IBlockState originalState = world.getBlockState(pos);
        TileEntity originalTile = world.getTileEntity(pos);
        NBTTagCompound originalTileTag = originalTile == null ? null : originalTile.writeToNBT(new NBTTagCompound());

        try {
            validateGuiAndPackets(world, pos);
            validateLiquefier(world, pos);
            validateConvertLiquefier(world, pos);
            validateConverter(world, pos);
        } finally {
            restore(world, pos, originalState, originalTileTag);
        }

        EmcFluid.logger.info("Validated Stage C machine conversions, conservation, persistence, cache backpressure, and recipes");
    }

    private static void validateGuiAndPackets(WorldServer world, BlockPos pos) {
        ModGuiHandler guiHandler = new ModGuiHandler();
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        player.setPosition(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);

        EmcLiquefierBlockEntity liquefier = place(
                world, pos, ModContent.emcLiquefier, EmcLiquefierBlockEntity.class);
        Object liquefierGui = guiHandler.getServerGuiElement(0, player, world, pos.getX(), pos.getY(), pos.getZ());
        require(liquefierGui instanceof EmcLiquefierMenu, "Liquefier server GUI did not create its Container");
        player.openContainer = (Container) liquefierGui;
        new ToggleLiquefierModePacket(pos.add(1, 0, 0)).handle(player);
        require(liquefier.getMode() == EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID,
                "Liquefier packet accepted a mismatched position");
        roundTrip(new ToggleLiquefierModePacket(pos)).handle(player);
        require(liquefier.getMode() == EmcLiquefierBlockEntity.Mode.FLUID_TO_EMC,
                "Liquefier packet did not update the open server Container");

        EmcConvertLiquefierBlockEntity convert = place(
                world, pos, ModContent.emcConvertLiquefier, EmcConvertLiquefierBlockEntity.class);
        Object convertGui = guiHandler.getServerGuiElement(1, player, world, pos.getX(), pos.getY(), pos.getZ());
        require(convertGui instanceof EmcConvertLiquefierMenu, "Convert Liquefier server GUI did not create its Container");
        player.openContainer = (Container) convertGui;
        roundTrip(new ToggleConvertLiquefierModePacket(pos)).handle(player);
        require(convert.getMode() == EmcLiquefierBlockEntity.Mode.FLUID_TO_EMC,
                "Convert Liquefier mode packet did not update the open server Container");
        if (EmcFluidTierConfig.enabledTiers() > 1) {
            roundTrip(new ChangeConvertLiquefierTierPacket(pos, 1)).handle(player);
            require(convert.getSelectedTier() == 1,
                    "Convert Liquefier tier packet did not update the open server Container");
        }

        EmcConverterBlockEntity converter = place(
                world, pos, ModContent.emcConverter, EmcConverterBlockEntity.class);
        Object converterGui = guiHandler.getServerGuiElement(3, player, world, pos.getX(), pos.getY(), pos.getZ());
        require(converterGui instanceof EmcConverterMenu, "Converter server GUI did not create its Container");
        player.openContainer = (Container) converterGui;
        roundTrip(new ToggleConverterModePacket(pos)).handle(player);
        require(converter.getMode() == EmcConverterBlockEntity.Mode.DOWNGRADE,
                "Converter packet did not update the open server Container");

        place(world, pos, ModContent.emcCrafter, EmcCrafterBlockEntity.class);
        Object crafterGui = guiHandler.getServerGuiElement(2, player, world, pos.getX(), pos.getY(), pos.getZ());
        require(crafterGui instanceof EmcCrafterMenu, "Crafter server GUI did not create its Container");
        require(guiHandler.getClientGuiElement(2, player, world, pos.getX(), pos.getY(), pos.getZ()) == null,
                "Dedicated server proxy unexpectedly constructed a client GUI");
    }

    private static ToggleLiquefierModePacket roundTrip(ToggleLiquefierModePacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            packet.toBytes(buffer);
            ToggleLiquefierModePacket decoded = new ToggleLiquefierModePacket();
            decoded.fromBytes(buffer);
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static ToggleConvertLiquefierModePacket roundTrip(ToggleConvertLiquefierModePacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            packet.toBytes(buffer);
            ToggleConvertLiquefierModePacket decoded = new ToggleConvertLiquefierModePacket();
            decoded.fromBytes(buffer);
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static ChangeConvertLiquefierTierPacket roundTrip(ChangeConvertLiquefierTierPacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            packet.toBytes(buffer);
            ChangeConvertLiquefierTierPacket decoded = new ChangeConvertLiquefierTierPacket();
            decoded.fromBytes(buffer);
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static ToggleConverterModePacket roundTrip(ToggleConverterModePacket packet) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            packet.toBytes(buffer);
            ToggleConverterModePacket decoded = new ToggleConverterModePacket();
            decoded.fromBytes(buffer);
            return decoded;
        } finally {
            buffer.release();
        }
    }

    private static void validateLiquefier(WorldServer world, BlockPos pos) {
        EmcLiquefierBlockEntity tile = place(world, pos, ModContent.emcLiquefier, EmcLiquefierBlockEntity.class);
        ItemStack star = createChargedKleinStar(1_000L);
        require(tile.getItems().insertItem(0, star, false).isEmpty(), "Liquefier rejected Klein Star");

        tile.update();
        require(tile.getFluidAmount() == 1_000, "Liquefier EMC-to-fluid result mismatch");
        require(ProjectEAccess.getStoredEmc(tile.getItems().getStackInSlot(0)) == 0L,
                "Liquefier did not extract the expected EMC");

        tile.toggleMode();
        tile.update();
        require(tile.getFluidAmount() == 0, "Liquefier fluid-to-EMC did not drain its tank");
        require(ProjectEAccess.getStoredEmc(tile.getItems().getStackInSlot(0)) == 1_000L,
                "Liquefier round trip did not conserve EMC");

        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        EmcLiquefierBlockEntity restored = new EmcLiquefierBlockEntity();
        restored.readFromNBT(saved);
        require(restored.getMode() == EmcLiquefierBlockEntity.Mode.FLUID_TO_EMC,
                "Liquefier mode was not persisted");
        require(ProjectEAccess.getStoredEmc(restored.getItems().getStackInSlot(0)) == 1_000L,
                "Liquefier inventory was not persisted");
    }

    private static void validateConvertLiquefier(WorldServer world, BlockPos pos) {
        for (int tier = 0; tier < EmcFluidTierConfig.enabledTiers(); tier++) {
            EmcConvertLiquefierBlockEntity tile = place(
                    world, pos, ModContent.emcConvertLiquefier, EmcConvertLiquefierBlockEntity.class);
            for (int step = 0; step < tier; step++) {
                tile.changeTier(1);
            }
            require(tile.getSelectedTier() == tier, "Convert Liquefier tier selection mismatch for T" + (tier + 1));

            long value = EmcFluidTierConfig.value(tier);
            long testValue = multiplyExact(value, TEST_MB, "Convert Liquefier test value overflow");
            ItemStack holder = createChargedEmcHolder(testValue);
            require(tile.getItems().insertItem(0, holder, false).isEmpty(),
                    "Convert Liquefier rejected Klein Star at T" + (tier + 1));
            tile.update();
            require(tile.getFluidAmount() == TEST_MB, "Convert Liquefier forward amount mismatch at T" + (tier + 1));
            require(ProjectEAccess.getStoredEmc(tile.getItems().getStackInSlot(0)) == 0L,
                    "Convert Liquefier forward EMC mismatch at T" + (tier + 1));

            ItemStack nonSerializableHolder = ItemStack.EMPTY;
            if (tile.getItems().getStackInSlot(0).getItem().getRegistryName() == null) {
                nonSerializableHolder = tile.getItems().extractItem(0, 1, false);
            }
            NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
            EmcConvertLiquefierBlockEntity restored = new EmcConvertLiquefierBlockEntity();
            restored.readFromNBT(saved);
            require(restored.getSelectedTier() == tier && restored.getFluidAmount() == TEST_MB,
                    "Convert Liquefier tier or tank was not persisted at T" + (tier + 1));
            if (!nonSerializableHolder.isEmpty()) {
                require(tile.getItems().insertItem(0, nonSerializableHolder, false).isEmpty(),
                        "Unable to restore probe EMC holder at T" + (tier + 1));
            }

            tile.toggleMode();
            tile.update();
            require(tile.getFluidAmount() == 0, "Convert Liquefier reverse drain mismatch at T" + (tier + 1));
            require(ProjectEAccess.getStoredEmc(tile.getItems().getStackInSlot(0)) == testValue,
                    "Convert Liquefier round trip did not conserve EMC at T" + (tier + 1));
        }
    }

    private static void validateConverter(WorldServer world, BlockPos pos) {
        int ticks = EmcFluidConfig.getConverterTicksPerBatch();
        for (int tier = 0; tier + 1 < EmcFluidTierConfig.enabledTiers(); tier++) {
            int ratio = EmcFluidTierConfig.upgradeInputAmount(tier);
            require(ratio > 0, "Adjacent tier values are not exactly convertible at T" + (tier + 1));

            EmcConverterBlockEntity tile = place(world, pos, ModContent.emcConverter, EmcConverterBlockEntity.class);
            require(tile.getInputTank().fill(new FluidStack(ModContent.getEmcFluid(tier), ratio), true) == ratio,
                    "Converter rejected upgrade input at T" + (tier + 1));
            tick(tile, ticks);
            require(tile.getInputAmount() == 0 && tile.getOutputAmount() == 1 && tile.getOutputTier() == tier + 1,
                    "Converter upgrade result mismatch at T" + (tier + 1));
            require(multiplyExact(ratio, EmcFluidTierConfig.value(tier), "Upgrade conservation overflow")
                            == EmcFluidTierConfig.value(tier + 1),
                    "Converter upgrade did not conserve EMC at T" + (tier + 1));

            tile.getOutputTank().drain(Integer.MAX_VALUE, true);
            tile.toggleMode();
            require(tile.getInputTank().fill(new FluidStack(ModContent.getEmcFluid(tier + 1), 1), true) == 1,
                    "Converter rejected downgrade input at T" + (tier + 2));
            tick(tile, ticks);
            require(tile.getInputAmount() == 0 && tile.getOutputAmount() == ratio && tile.getOutputTier() == tier,
                    "Converter downgrade result mismatch at T" + (tier + 2));
            require(multiplyExact(tile.getOutputAmount(), EmcFluidTierConfig.value(tier), "Downgrade conservation overflow")
                            == EmcFluidTierConfig.value(tier + 1),
                    "Converter downgrade did not conserve EMC at T" + (tier + 2));
        }

        if (EmcFluidTierConfig.enabledTiers() > 1) {
            validateConverterBackpressure(world, pos, ticks);
            validateConverterProgressPersistence(world, pos, ticks);
        }
    }

    private static void validateConverterBackpressure(WorldServer world, BlockPos pos, int ticks) {
        EmcConverterBlockEntity tile = place(world, pos, ModContent.emcConverter, EmcConverterBlockEntity.class);
        int ratio = EmcFluidTierConfig.upgradeInputAmount(0);
        require(tile.getInputTank().fill(new FluidStack(ModContent.getEmcFluid(0), ratio), true) == ratio,
                "Converter backpressure input setup failed");
        require(tile.getOutputTank().fill(
                        new FluidStack(ModContent.getEmcFluid(1), EmcConverterBlockEntity.TANK_CAPACITY), true)
                        == EmcConverterBlockEntity.TANK_CAPACITY,
                "Converter backpressure output setup failed");
        tick(tile, ticks + 1);
        require(tile.getInputAmount() == ratio && tile.getOutputAmount() == EmcConverterBlockEntity.TANK_CAPACITY,
                "Converter consumed input while its output was full");
    }

    private static void validateConverterProgressPersistence(WorldServer world, BlockPos pos, int ticks) {
        EmcConverterBlockEntity tile = place(world, pos, ModContent.emcConverter, EmcConverterBlockEntity.class);
        int ratio = EmcFluidTierConfig.upgradeInputAmount(0);
        tile.getInputTank().fill(new FluidStack(ModContent.getEmcFluid(0), ratio), true);
        if (ticks > 1) {
            tile.update();
        }
        tile.toggleMode();
        tile.toggleMode();
        if (ticks > 1) {
            tile.update();
        }
        NBTTagCompound saved = tile.writeToNBT(new NBTTagCompound());
        EmcConverterBlockEntity restored = new EmcConverterBlockEntity();
        restored.readFromNBT(saved);
        NBTTagCompound resaved = restored.writeToNBT(new NBTTagCompound());
        require(restored.getMode() == tile.getMode(), "Converter mode was not persisted");
        require(restored.getInputAmount() == tile.getInputAmount(), "Converter input tank was not persisted");
        require(resaved.getInteger("ConversionProgress") == saved.getInteger("ConversionProgress"),
                "Converter progress was not persisted");
    }

    private static void validateStandalonePersistenceAndCache() {
        EmcCrafterBlockEntity crafter = new EmcCrafterBlockEntity();
        require(crafter.getPattern().insertItem(0, new ItemStack(ModContent.knowledgePattern), false).isEmpty(),
                "Crafter rejected Knowledge Pattern");
        require(crafter.queueOutput(new ItemStack(Items.IRON_INGOT, 32), false),
                "Crafter rejected an output that fits in its cache");

        NBTTagCompound saved = crafter.writeToNBT(new NBTTagCompound());
        EmcCrafterBlockEntity restored = new EmcCrafterBlockEntity();
        restored.readFromNBT(saved);
        require(restored.getPattern().getStackInSlot(0).getItem() == ModContent.knowledgePattern,
                "Crafter pattern was not persisted");
        require(restored.getOutputCache().getStackInSlot(0).getCount() == 32,
                "Crafter output cache was not persisted");

        for (int slot = 0; slot < restored.getOutputCache().getSlots(); slot++) {
            restored.getOutputCache().setStackInSlot(slot, new ItemStack(Blocks.COBBLESTONE, 64));
        }
        require(!restored.queueOutput(new ItemStack(Items.DIAMOND), true),
                "Crafter simulated space in a full output cache");
        require(!restored.queueOutput(new ItemStack(Items.DIAMOND), false),
                "Crafter accepted output into a full cache");
        for (int slot = 0; slot < restored.getOutputCache().getSlots(); slot++) {
            ItemStack stack = restored.getOutputCache().getStackInSlot(slot);
            require(stack.getItem() == Item.getItemFromBlock(Blocks.COBBLESTONE) && stack.getCount() == 64,
                    "Crafter changed a full cache after rejecting output");
        }
    }

    private static void validateRecipes() {
        requireRecipe("emc_liquefier", ModContent.emcLiquefier);
        requireRecipe("emc_convert_liquefier", ModContent.emcConvertLiquefier);
        requireRecipe("emc_converter", ModContent.emcConverter);
        requireRecipe("emc_crafter", ModContent.emcCrafter);
    }

    private static void requireRecipe(String name, Block output) {
        IRecipe recipe = CraftingManager.REGISTRY.getObject(new ResourceLocation(EmcFluid.MODID, name));
        require(recipe != null && !recipe.getRecipeOutput().isEmpty()
                        && recipe.getRecipeOutput().getItem() == Item.getItemFromBlock(output),
                "Missing or invalid machine recipe: " + name);
    }

    private static ItemStack createChargedKleinStar(long emc) {
        Item item = Item.REGISTRY.getObject(new ResourceLocation("projecte", "item.pe_klein_star"));
        require(item != null && item != Items.AIR, "ProjectE Klein Star is not registered");
        ItemStack star = new ItemStack(item, 1, 5);
        require(ProjectEAccess.isEmcHolder(star), "ProjectE Klein Star does not expose IItemEmc");
        require(ProjectEAccess.insertEmc(star, emc, true) == emc,
                "Klein Star capacity is too small for Stage C probe value " + emc);
        return star;
    }

    private static ItemStack createChargedEmcHolder(long emc) {
        ItemStack kleinStar = createChargedKleinStar(Math.min(emc, 51_200_000L));
        if (ProjectEAccess.getMaximumEmc(kleinStar) >= emc) {
            if (ProjectEAccess.getStoredEmc(kleinStar) < emc) {
                require(ProjectEAccess.insertEmc(kleinStar, emc - ProjectEAccess.getStoredEmc(kleinStar), true)
                                == emc - ProjectEAccess.getStoredEmc(kleinStar),
                        "Unable to finish charging Klein Star");
            }
            return kleinStar;
        }
        ItemStack holder = new ItemStack(LARGE_EMC_HOLDER);
        require(ProjectEAccess.insertEmc(holder, emc, true) == emc,
                "Unable to charge large-capacity probe EMC holder");
        return holder;
    }

    private static void tick(EmcConverterBlockEntity tile, int ticks) {
        for (int i = 0; i < ticks; i++) {
            tile.update();
        }
    }

    private static <T extends TileEntity> T place(WorldServer world, BlockPos pos, Block block, Class<T> type) {
        clearProbeMachineContents(world.getTileEntity(pos));
        world.setBlockToAir(pos);
        require(world.setBlockState(pos, block.getDefaultState(), 3), "Unable to place probe block " + block.getRegistryName());
        TileEntity tile = world.getTileEntity(pos);
        require(type.isInstance(tile), "Wrong TileEntity for probe block " + block.getRegistryName());
        return type.cast(tile);
    }

    private static void restore(WorldServer world, BlockPos pos, IBlockState state, NBTTagCompound tileTag) {
        clearProbeMachineContents(world.getTileEntity(pos));
        world.setBlockToAir(pos);
        world.setBlockState(pos, state, 3);
        if (tileTag != null) {
            TileEntity restored = world.getTileEntity(pos);
            require(restored != null, "Unable to restore original TileEntity at probe position");
            restored.readFromNBT(tileTag);
            restored.markDirty();
        }
    }

    private static void clearProbeMachineContents(TileEntity tile) {
        if (!(tile instanceof MachineTileEntity)) {
            return;
        }
        for (ItemStackHandler handler : ((MachineTileEntity) tile).getDroppableItemHandlers()) {
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                handler.extractItem(slot, Integer.MAX_VALUE, false);
            }
        }
    }

    private static long multiplyExact(long left, long right, String message) {
        if (left != 0L && right > Long.MAX_VALUE / left) {
            throw new IllegalStateException(message);
        }
        return left * right;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class ProbeEmcItem extends Item implements IItemEmc {
        private static final String STORED = "StoredEMC";

        @Override
        public long addEmc(ItemStack stack, long toAdd) {
            long stored = getStoredEmc(stack);
            long added = Math.min(toAdd, Long.MAX_VALUE - stored);
            tag(stack).setLong(STORED, stored + added);
            return added;
        }

        @Override
        public long extractEmc(ItemStack stack, long toRemove) {
            long removed = Math.min(toRemove, getStoredEmc(stack));
            tag(stack).setLong(STORED, getStoredEmc(stack) - removed);
            return removed;
        }

        @Override
        public long getStoredEmc(ItemStack stack) {
            return stack.hasTagCompound() ? Math.max(0L, stack.getTagCompound().getLong(STORED)) : 0L;
        }

        @Override
        public long getMaximumEmc(ItemStack stack) {
            return Long.MAX_VALUE;
        }

        private static NBTTagCompound tag(ItemStack stack) {
            if (!stack.hasTagCompound()) {
                stack.setTagCompound(new NBTTagCompound());
            }
            return stack.getTagCompound();
        }
    }
}
