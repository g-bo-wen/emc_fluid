package cn.gbk.emcfluid.dev;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import cn.gbk.emcfluid.network.ChangeConvertLiquefierTierPacket;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.CostResolver;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;
import java.util.Optional;

public final class StageHProbe {
    private StageHProbe() {
    }

    public static void run(MinecraftServer server) {
        validateCostBoundaries();
        validateMalformedNbt();
        validatePacketBoundaries(server.getWorld(0));
        EmcFluid.logger.info("Validated Stage H NBT sanitization, packet boundary clamping, and EMC cost overflow guards");
    }

    private static void validateCostBoundaries() {
        require(!CostResolver.resolve(0L).isPresent(), "Zero EMC unexpectedly produced a fluid cost");
        require(!CostResolver.resolve(-1L).isPresent(), "Negative EMC unexpectedly produced a fluid cost");
        assertExactCost(1L);
        assertExactCost(Integer.MAX_VALUE);

        Optional<List<EmcFluidInput>> maximum = CostResolver.resolve(Long.MAX_VALUE);
        if (maximum.isPresent()) {
            require(sumCost(maximum.get()) == Long.MAX_VALUE,
                    "Long.MAX_VALUE EMC cost overflowed while being represented");
        }
    }

    private static void assertExactCost(long value) {
        Optional<List<EmcFluidInput>> resolved = CostResolver.resolve(value);
        require(resolved.isPresent(), "Representable EMC cost was rejected: " + value);
        require(sumCost(resolved.get()) == value, "EMC cost did not round-trip exactly: " + value);
    }

    private static long sumCost(List<EmcFluidInput> inputs) {
        long total = 0L;
        for (EmcFluidInput input : inputs) {
            require(input.amount() > 0, "EMC cost contained a non-positive fluid amount");
            long component = Math.multiplyExact(
                    EmcFluidTierConfig.value(input.tierIndex()), (long) input.amount());
            total = Math.addExact(total, component);
        }
        return total;
    }

    private static void validateMalformedNbt() {
        NBTTagCompound liquefierTag = new EmcLiquefierBlockEntity().writeToNBT(new NBTTagCompound());
        liquefierTag.setInteger("Mode", Integer.MAX_VALUE);
        liquefierTag.setTag("Tank", new FluidStack(
                ModContent.getEmcFluid(0), Integer.MAX_VALUE).writeToNBT(new NBTTagCompound()));
        ItemStackHandler invalidInput = new ItemStackHandler(1);
        invalidInput.setStackInSlot(0, new ItemStack(Items.DIAMOND, 127));
        liquefierTag.setTag("Items", invalidInput.serializeNBT());
        EmcLiquefierBlockEntity liquefier = new EmcLiquefierBlockEntity();
        liquefier.readFromNBT(liquefierTag);
        require(liquefier.getMode() == EmcLiquefierBlockEntity.Mode.EMC_TO_FLUID,
                "Invalid Liquefier mode was not reset");
        require(liquefier.getFluidAmount() == EmcLiquefierBlockEntity.TANK_CAPACITY,
                "Oversized Liquefier tank was not clamped");
        require(liquefier.getItems().getStackInSlot(0).isEmpty(),
                "Invalid Liquefier input survived NBT loading");

        NBTTagCompound emptyTank = new FluidStack(
                ModContent.getEmcFluid(0), -1).writeToNBT(new NBTTagCompound());
        liquefierTag.setTag("Tank", emptyTank);
        EmcLiquefierBlockEntity negativeTank = new EmcLiquefierBlockEntity();
        negativeTank.readFromNBT(liquefierTag);
        require(negativeTank.getFluidAmount() == 0,
                "Negative Liquefier tank amount survived NBT loading");

        liquefierTag.setTag("Tank", new FluidStack(
                FluidRegistry.WATER, 1000).writeToNBT(new NBTTagCompound()));
        liquefierTag.setInteger("Mode", EmcLiquefierBlockEntity.Mode.FLUID_TO_EMC.ordinal());
        EmcLiquefierBlockEntity foreignLiquefier = new EmcLiquefierBlockEntity();
        foreignLiquefier.readFromNBT(liquefierTag);
        require(foreignLiquefier.getFluidAmount() == 0,
                "Non-EMC fluid survived Liquefier NBT loading");

        NBTTagCompound converterTag = new EmcConverterBlockEntity().writeToNBT(new NBTTagCompound());
        converterTag.setInteger("Mode", Integer.MIN_VALUE);
        converterTag.setInteger("ConversionProgress", Integer.MAX_VALUE);
        converterTag.setTag("RedTank", new FluidStack(
                ModContent.getEmcFluid(0), Integer.MAX_VALUE).writeToNBT(new NBTTagCompound()));
        converterTag.setTag("BlueTank", new FluidStack(
                ModContent.getEmcFluid(Math.min(1, EmcFluidTierConfig.enabledTiers() - 1)),
                Integer.MAX_VALUE).writeToNBT(new NBTTagCompound()));
        EmcConverterBlockEntity converter = new EmcConverterBlockEntity();
        converter.readFromNBT(converterTag);
        NBTTagCompound converterResaved = converter.writeToNBT(new NBTTagCompound());
        require(converter.getMode() == EmcConverterBlockEntity.Mode.UPGRADE,
                "Invalid Converter mode was not reset");
        require(converter.getInputAmount() == EmcConverterBlockEntity.TANK_CAPACITY
                        && converter.getOutputAmount() == EmcConverterBlockEntity.TANK_CAPACITY,
                "Oversized Converter tank was not clamped");
        require(converterResaved.getInteger("ConversionProgress")
                        == Math.max(0, EmcFluidConfig.getConverterTicksPerBatch() - 1),
                "Oversized Converter progress was not clamped");

        converterTag.setTag("RedTank", new FluidStack(
                FluidRegistry.WATER, 1000).writeToNBT(new NBTTagCompound()));
        converterTag.setTag("BlueTank", new FluidStack(
                FluidRegistry.LAVA, 1000).writeToNBT(new NBTTagCompound()));
        EmcConverterBlockEntity foreignConverter = new EmcConverterBlockEntity();
        foreignConverter.readFromNBT(converterTag);
        require(foreignConverter.getInputAmount() == 0 && foreignConverter.getOutputAmount() == 0,
                "Non-EMC fluid survived Converter NBT loading");

        NBTTagCompound convertTag = new EmcConvertLiquefierBlockEntity()
                .writeToNBT(new NBTTagCompound());
        convertTag.setInteger("SelectedTier", Integer.MAX_VALUE);
        EmcConvertLiquefierBlockEntity convert = new EmcConvertLiquefierBlockEntity();
        convert.readFromNBT(convertTag);
        require(convert.getSelectedTier() == EmcFluidTierConfig.enabledTiers() - 1,
                "Oversized Convert Liquefier tier was not clamped");

        convertTag.setTag("Tank", new FluidStack(
                FluidRegistry.WATER, 1000).writeToNBT(new NBTTagCompound()));
        EmcConvertLiquefierBlockEntity foreignConvert = new EmcConvertLiquefierBlockEntity();
        foreignConvert.readFromNBT(convertTag);
        require(foreignConvert.getFluidAmount() == 0,
                "Non-EMC fluid survived Convert Liquefier NBT loading");

        EmcCrafterBlockEntity sourceCrafter = new EmcCrafterBlockEntity();
        sourceCrafter.getPattern().setStackInSlot(
                0, new ItemStack(ModContent.knowledgePattern, 127));
        sourceCrafter.getOutputCache().setStackInSlot(0, new ItemStack(Items.DIAMOND, 127));
        EmcCrafterBlockEntity crafter = new EmcCrafterBlockEntity();
        crafter.readFromNBT(sourceCrafter.writeToNBT(new NBTTagCompound()));
        require(crafter.getPattern().getStackInSlot(0).getCount() == 1,
                "Oversized Knowledge Pattern stack was not clamped");
        require(crafter.getOutputCache().getStackInSlot(0).getCount() == 64,
                "Oversized Crafter output stack was not clamped");
    }

    private static void validatePacketBoundaries(WorldServer world) {
        BlockPos top = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());
        BlockPos pos = new BlockPos(top.getX() - 8,
                Math.min(world.getHeight() - 2, top.getY() + 4), top.getZ());
        IBlockState savedState = world.getBlockState(pos);
        TileEntity savedTile = world.getTileEntity(pos);
        NBTTagCompound savedTileTag = savedTile == null
                ? null : savedTile.writeToNBT(new NBTTagCompound());
        FakePlayer player = FakePlayerFactory.getMinecraft(world);
        try {
            require(world.setBlockState(pos, ModContent.emcConvertLiquefier.getDefaultState(), 3),
                    "Unable to place Convert Liquefier for packet boundary probe");
            TileEntity tile = world.getTileEntity(pos);
            require(tile instanceof EmcConvertLiquefierBlockEntity,
                    "Convert Liquefier packet boundary TileEntity is missing");
            EmcConvertLiquefierBlockEntity convert = (EmcConvertLiquefierBlockEntity) tile;
            player.setPosition(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
            player.openContainer = new EmcConvertLiquefierMenu(player.inventory, convert);

            roundTrip(new ChangeConvertLiquefierTierPacket(pos, Integer.MAX_VALUE)).handle(player);
            require(convert.getSelectedTier() == Math.min(1, EmcFluidTierConfig.enabledTiers() - 1),
                    "Positive packet boundary changed more than one tier");
            roundTrip(new ChangeConvertLiquefierTierPacket(pos, Integer.MIN_VALUE)).handle(player);
            require(convert.getSelectedTier() == 0,
                    "Negative packet boundary changed more than one tier");
            roundTrip(new ChangeConvertLiquefierTierPacket(pos, 0)).handle(player);
            require(convert.getSelectedTier() == 0, "Zero tier delta changed machine state");
            roundTrip(new ChangeConvertLiquefierTierPacket(pos.east(), Integer.MAX_VALUE)).handle(player);
            require(convert.getSelectedTier() == 0,
                    "Packet boundary probe accepted a mismatched BlockPos");
        } finally {
            player.openContainer = player.inventoryContainer;
            restore(world, pos, savedState, savedTileTag);
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

    private static void restore(WorldServer world, BlockPos pos, IBlockState state,
                                NBTTagCompound tileTag) {
        world.setBlockState(pos, state, 3);
        if (tileTag != null) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile != null) {
                tile.readFromNBT(tileTag);
                tile.markDirty();
            }
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
