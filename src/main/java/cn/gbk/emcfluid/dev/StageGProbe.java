package cn.gbk.emcfluid.dev;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.pathing.IPathingGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.me.helpers.MachineSource;
import appeng.tile.storage.TileDrive;
import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.integration.ae2.EmcAePattern;
import cn.gbk.emcfluid.integration.ae2.EmcCrafterAe2Integration;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import cn.gbk.emcfluid.util.KnowledgePatternSync;
import com.google.common.collect.ImmutableCollection;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.impl.TransmutationOffline;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.IItemHandler;
import xyz.phanta.ae2fc.init.FcItems;
import xyz.phanta.ae2fc.init.FcBlocks;
import xyz.phanta.ae2fc.item.ItemFluidDrop;
import xyz.phanta.ae2fc.item.ItemFluidPacket;
import xyz.phanta.ae2fc.tile.TileFluidDiscretizer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/** Server-side acceptance probe for the fixed AE2 rv6 + AE2FC pair. */
public final class StageGProbe {
    private static final int BATCH_SIZE = 3;
    private static MinecraftServer pendingServer;
    private static ProbeContext context;
    private static int waitTicks;
    private static boolean registered;

    private StageGProbe() {
    }

    public static void schedule(MinecraftServer server) {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(StageGProbe.class);
            registered = true;
        }
        pendingServer = server;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingServer == null
                || !pendingServer.isServerRunning()) {
            return;
        }
        if (context == null) {
            context = setup(pendingServer);
            // AE2's ad-hoc channel recalculation is asynchronous and can take
            // more than one pathing update interval on a freshly placed grid.
            waitTicks = 100;
            return;
        }
        if (waitTicks-- > 0) {
            return;
        }

        ProbeContext probe = context;
        try {
            if (probe.phase == 0) {
                beginMissingCalculation(probe);
                probe.phase = 1;
                return;
            }
            if (probe.phase == 1) {
                if (!calculationComplete(probe, "missing-material")) {
                    return;
                }
                validateMissingCalculationAndConfigureBatch(probe);
                probe.phase = 2;
                waitTicks = 100;
                return;
            }
            if (probe.phase == 2) {
                seedBatchFluids(probe);
                probe.phase = 3;
                waitTicks = 5;
                return;
            }
            if (probe.phase == 3) {
                beginBatchCalculation(probe);
                probe.phase = 4;
                return;
            }
            if (probe.phase == 4) {
                if (!calculationComplete(probe, "batch")) {
                    return;
                }
                submitBatch(probe);
                probe.phase = 5;
                probe.asyncWait = 200;
                return;
            }
            if (probe.phase == 5) {
                if (itemCount(probe.crafter.getProxy().getNode(), probe.target.output())
                        < BATCH_SIZE && probe.asyncWait-- > 0) {
                    return;
                }
                validateBatchAndDirectExecution(probe);
                probe.phase = 6;
                waitTicks = 5;
                return;
            }
            if (probe.phase == 6) {
                validateBackpressure(probe);
                probe.phase = 7;
                waitTicks = 5;
                return;
            }
            validateRestoredStorageAndAuthorization(probe);
            EmcFluid.logger.info("Validated Stage G missing-material calculation, bounded crafting bytes, real Discretizer batch crafting, Fluid Drop/Packet execution, node persistence, provider visibility, safe authorization, and ME output backpressure");
            cleanup(probe);
            context = null;
            pendingServer = null;
        } catch (RuntimeException exception) {
            cleanup(probe);
            context = null;
            pendingServer = null;
            throw exception;
        } catch (Error error) {
            cleanup(probe);
            context = null;
            pendingServer = null;
            throw error;
        }
    }

    private static ProbeContext setup(MinecraftServer server) {
        WorldServer world = server.getWorld(0);
        UUID owner = UUID.randomUUID();
        File playerFile = new File(new File(
                DimensionManager.getCurrentSaveRootDirectory(), "playerdata"), owner + ".dat");
        writeKnowledge(playerFile, true);
        TransmutationOffline.clear(owner);

        BlockPos top = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());
        BlockPos energyPos = new BlockPos(top.getX() + 8,
                Math.min(world.getHeight() - 2, top.getY() + 4), top.getZ());
        BlockPos drivePos = energyPos.east();
        BlockPos crafterPos = drivePos.east();
        BlockPos discretizerPos = crafterPos.east();
        BlockPos cpuPos = energyPos.up();
        SavedBlock energySaved = SavedBlock.capture(world, energyPos);
        SavedBlock driveSaved = SavedBlock.capture(world, drivePos);
        SavedBlock crafterSaved = SavedBlock.capture(world, crafterPos);
        SavedBlock discretizerSaved = SavedBlock.capture(world, discretizerPos);
        SavedBlock cpuSaved = SavedBlock.capture(world, cpuPos);

        Block energyCell = AEApi.instance().definitions().blocks().energyCellCreative()
                .maybeBlock().orElseThrow(() -> new IllegalStateException("AE2 creative energy cell is unavailable"));
        Block driveBlock = AEApi.instance().definitions().blocks().drive()
                .maybeBlock().orElseThrow(() -> new IllegalStateException("AE2 drive is unavailable"));
        require(world.setBlockState(energyPos, energyCell.getDefaultState(), 3),
                "Unable to place AE2 creative energy cell");
        require(world.setBlockState(drivePos, driveBlock.getDefaultState(), 3),
                "Unable to place AE2 drive");
        require(world.setBlockState(crafterPos, ModContent.emcCrafter.getDefaultState(), 3),
                "Unable to place AE2 EMC Crafter");

        TileEntity driveTile = world.getTileEntity(drivePos);
        TileEntity crafterTile = world.getTileEntity(crafterPos);
        require(driveTile instanceof TileDrive, "AE2 drive TileEntity is missing");
        require(crafterTile instanceof EmcCrafterAe2Integration,
                "EMC Crafter did not use its conditional AE2 TileEntity");
        TileDrive drive = (TileDrive) driveTile;
        EmcCrafterAe2Integration crafter = (EmcCrafterAe2Integration) crafterTile;

        // An unconfigured creative storage cell rejects every item; a real 64K
        // cell is required to observe exact insertion counts and backpressure.
        ItemStack storageCell = AEApi.instance().definitions().items().cell64k()
                .maybeStack(1).orElseThrow(() -> new IllegalStateException("AE2 64K item cell is unavailable"));
        require(drive.getInternalInventory().insertItem(0, storageCell, false).isEmpty(),
                "AE2 drive rejected its creative item cell");

        ItemStack knowledgePattern = new ItemStack(ModContent.knowledgePattern);
        require(KnowledgePatternData.bind(knowledgePattern, owner, "StageGProbe"),
                "Unable to bind Stage G Knowledge Pattern");
        require(crafter.getPattern().insertItem(0, knowledgePattern, false).isEmpty(),
                "EMC Crafter rejected Stage G Knowledge Pattern");
        EmcCraftingTarget target = findTarget(crafter);
        return new ProbeContext(world, owner, playerFile, energyPos, drivePos, crafterPos,
                discretizerPos, cpuPos, energySaved, driveSaved, crafterSaved,
                discretizerSaved, cpuSaved, drive, crafter, target);
    }

    private static void beginMissingCalculation(ProbeContext probe) {
        IGridNode node = probe.crafter.getProxy().getNode();
        require(node != null && node.isActive(),
                "EMC Crafter AE2 node did not become active: " + describeNode(node));

        IItemStorageChannel itemChannel = AEApi.instance().storage()
                .getStorageChannel(IItemStorageChannel.class);
        IAEItemStack output = itemChannel.createStack(probe.target.output());
        require(output != null, "Unable to encode Stage G output");
        ICraftingGrid crafting = node.getGrid().getCache(ICraftingGrid.class);
        ImmutableCollection<ICraftingPatternDetails> options = crafting.getCraftingFor(
                output, null, 0, probe.world);
        EmcAePattern pattern = null;
        for (ICraftingPatternDetails option : options) {
            if (option instanceof EmcAePattern) {
                pattern = (EmcAePattern) option;
                break;
            }
        }
        require(pattern != null, "ME crafting grid cannot see the EMC knowledge target");
        validatePatternEncoding(pattern, probe.target);
        probe.pattern = pattern;
        probe.crafting = crafting;

        output.setStackSize(BATCH_SIZE);
        probe.calculation = crafting.beginCraftingJob(probe.world, node.getGrid(),
                new MachineSource(probe.crafter), output, null);
        probe.asyncWait = 200;
    }

    private static boolean calculationComplete(ProbeContext probe, String name) {
        if (probe.calculation != null && probe.calculation.isDone()) {
            return true;
        }
        require(probe.asyncWait-- > 0, "AE2 " + name + " crafting calculation timed out");
        return false;
    }

    private static ICraftingJob takeCalculation(ProbeContext probe) {
        Future<ICraftingJob> future = probe.calculation;
        probe.calculation = null;
        require(future != null && future.isDone(), "AE2 crafting calculation was not complete");
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while reading AE2 crafting calculation", exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException("AE2 crafting calculation failed", exception.getCause());
        }
    }

    private static void validateMissingCalculationAndConfigureBatch(ProbeContext probe) {
        ICraftingJob job = takeCalculation(probe);
        require(job.isSimulation(),
                "AE2 unexpectedly found Fluid Drops without a Fluid Discretizer");
        IItemStorageChannel itemChannel = AEApi.instance().storage()
                .getStorageChannel(IItemStorageChannel.class);
        IItemList<IAEItemStack> plan = itemChannel.createList();
        job.populatePlan(plan);
        for (EmcFluidInput input : probe.target.fluidInputs()) {
            IAEItemStack key = ItemFluidDrop.newAeStack(input.stack());
            IAEItemStack missing = key == null ? null : plan.findPrecise(key);
            long expected = (long) input.amount() * BATCH_SIZE;
            require(missing != null && missing.getStackSize() == expected,
                    "AE2 missing-material preview did not report the exact Fluid Drop amount");
        }

        ItemStack fluidCell = AEApi.instance().definitions().items().fluidCell64k()
                .maybeStack(1).orElseThrow(() -> new IllegalStateException("AE2 64K fluid cell is unavailable"));
        require(probe.drive.getInternalInventory().insertItem(1, fluidCell, false).isEmpty(),
                "AE2 drive rejected its 64K fluid cell");
        require(probe.world.setBlockState(probe.discretizerPos,
                        FcBlocks.FLUID_DISCRETIZER.getDefaultState(), 3),
                "Unable to place AE2FC Fluid Discretizer");
        Block cpuBlock = AEApi.instance().definitions().blocks().craftingStorage64k()
                .maybeBlock().orElseThrow(() -> new IllegalStateException("AE2 64K crafting storage is unavailable"));
        require(probe.world.setBlockState(probe.cpuPos, cpuBlock.getDefaultState(), 3),
                "Unable to place AE2 crafting CPU");
        TileEntity discretizerTile = probe.world.getTileEntity(probe.discretizerPos);
        require(discretizerTile instanceof TileFluidDiscretizer,
                "AE2FC Fluid Discretizer TileEntity is missing");
        probe.discretizer = (TileFluidDiscretizer) discretizerTile;
    }

    private static void seedBatchFluids(ProbeContext probe) {
        IGridNode node = probe.crafter.getProxy().getNode();
        require(node != null && node.isActive(), "EMC Crafter node became inactive before batch test");
        require(probe.discretizer != null && probe.discretizer.getProxy().isActive(),
                "AE2FC Fluid Discretizer did not become active");
        require(!probe.crafting.getCpus().isEmpty(), "AE2 crafting CPU did not form or become active");

        IFluidStorageChannel channel = AEApi.instance().storage()
                .getStorageChannel(IFluidStorageChannel.class);
        IStorageGrid storage = node.getGrid().getCache(IStorageGrid.class);
        IEnergyGrid energy = node.getGrid().getCache(IEnergyGrid.class);
        IMEMonitor<IAEFluidStack> inventory = storage.getInventory(channel);
        MachineSource source = new MachineSource(probe.crafter);
        for (EmcFluidInput input : probe.target.fluidInputs()) {
            FluidStack fluid = new FluidStack(input.fluid(), input.amount() * BATCH_SIZE);
            IAEFluidStack encoded = channel.createStack(fluid);
            require(encoded != null, "Unable to encode EMC fluid for AE2 batch test");
            IAEFluidStack remaining = AEApi.instance().storage().poweredInsert(
                    energy, inventory, encoded, source, Actionable.MODULATE);
            require(remaining == null || remaining.getStackSize() == 0L,
                    "AE2 fluid cell rejected batch-test EMC fluid");
        }
    }

    private static void beginBatchCalculation(ProbeContext probe) {
        IGridNode node = probe.crafter.getProxy().getNode();
        require(node != null && node.isActive(), "EMC Crafter node became inactive before batch calculation");
        IItemStorageChannel channel = AEApi.instance().storage()
                .getStorageChannel(IItemStorageChannel.class);
        IAEItemStack output = channel.createStack(probe.target.output());
        require(output != null, "Unable to encode Stage G batch output");
        output.setStackSize(BATCH_SIZE);
        probe.calculation = probe.crafting.beginCraftingJob(probe.world, node.getGrid(),
                new MachineSource(probe.crafter), output, null);
        probe.asyncWait = 200;
    }

    private static void submitBatch(ProbeContext probe) {
        ICraftingJob job = takeCalculation(probe);
        require(!job.isSimulation(),
                "AE2 still reported missing Fluid Drops with an active Fluid Discretizer");
        long totalFluid = 0L;
        for (EmcFluidInput input : probe.target.fluidInputs()) {
            totalFluid += (long) input.amount() * BATCH_SIZE;
        }
        require(job.getByteTotal() < totalFluid,
                "AE2FC did not condense real Fluid Drop crafting bytes: bytes="
                        + job.getByteTotal() + ", mB=" + totalFluid);
        ICraftingLink link = probe.crafting.submitJob(job, null, null, false,
                new MachineSource(probe.crafter));
        require(link != null, "AE2 rejected the real batch crafting job");
    }

    private static void validateBatchAndDirectExecution(ProbeContext probe) {
        IGridNode node = probe.crafter.getProxy().getNode();
        require(node != null && node.isActive(), "EMC Crafter node became inactive during batch crafting");
        require(itemCount(node, probe.target.output()) == BATCH_SIZE,
                "AE2 batch crafting did not produce the exact requested item count");
        for (EmcFluidInput input : probe.target.fluidInputs()) {
            require(fluidAmount(node, input.stack()) == 0L,
                    "AE2 batch crafting did not deduct the exact ME fluid amount");
        }
        require(itemCount(node, new ItemStack(FcItems.FLUID_PACKET)) == 0,
                "AE2FC Fluid Packet leaked during real batch crafting");

        int before = itemCount(node, probe.target.output());
        require(probe.crafter.pushPattern(probe.pattern, tableFor(probe.target, false)),
                "EMC Crafter rejected exact Fluid Drop inputs");
        probe.crafter.update();
        require(itemCount(node, probe.target.output()) == before + 1,
                "Fluid Drop execution produced the wrong ME output count");

        require(probe.crafter.pushPattern(probe.pattern, tableFor(probe.target, true)),
                "EMC Crafter rejected exact Fluid Packet inputs");
        probe.crafter.update();
        require(itemCount(node, probe.target.output()) == before + 2,
                "Fluid Packet execution produced the wrong ME output count");
        require(itemCount(node, new ItemStack(FcItems.FLUID_PACKET)) == 0,
                "AE2FC Fluid Packet leaked into ME item storage");

        NBTTagCompound saved = probe.crafter.writeToNBT(new NBTTagCompound());
        require(saved.hasKey("EmcFluidAeNode", 10), "AE2 node state was not written to machine NBT");

        IItemHandler driveInventory = probe.drive.getInternalInventory();
        probe.storageCell = driveInventory.extractItem(0, 1, false);
        require(!probe.storageCell.isEmpty(), "Unable to remove AE2 item cell for backpressure test");
        probe.before = before;
    }

    private static void validateBackpressure(ProbeContext probe) {
        IGridNode node = probe.crafter.getProxy().getNode();
        require(node != null && node.isActive(),
                "EMC Crafter node became inactive during storage backpressure test");
        require(itemCount(node, probe.target.output()) == 0,
                "Removed AE2 storage cell remained visible to the network");
        require(probe.crafter.pushPattern(probe.pattern, tableFor(probe.target, true)),
                "EMC Crafter rejected output while ME storage was unavailable");
        probe.crafter.update();
        require(hasCachedOutput(probe.crafter),
                "ME backpressure did not retain the crafted output in the machine cache");
        require(itemCount(node, probe.target.output()) == 0,
                "ME backpressure inserted an output without storage capacity");
        NBTTagCompound cachedNbt = probe.crafter.writeToNBT(new NBTTagCompound());
        EmcCrafterAe2Integration firstRestore = new EmcCrafterAe2Integration();
        firstRestore.readFromNBT(cachedNbt);
        require(cachedOutputCount(firstRestore, probe.target.output()) == 1,
                "AE2 cached output changed during the first NBT reconstruction");
        EmcCrafterAe2Integration secondRestore = new EmcCrafterAe2Integration();
        secondRestore.readFromNBT(firstRestore.writeToNBT(new NBTTagCompound()));
        require(cachedOutputCount(secondRestore, probe.target.output()) == 1,
                "AE2 cached output duplicated during repeated NBT reconstruction");

        IItemHandler driveInventory = probe.drive.getInternalInventory();
        require(driveInventory.insertItem(0, probe.storageCell, false).isEmpty(),
                "Unable to restore AE2 item cell after backpressure test");
        probe.storageCell = ItemStack.EMPTY;
    }

    private static void validateRestoredStorageAndAuthorization(ProbeContext probe) {
        IGridNode node = probe.crafter.getProxy().getNode();
        require(node != null && node.isActive(),
                "EMC Crafter node became inactive after storage returned");
        probe.crafter.update();
        require(!hasCachedOutput(probe.crafter),
                "Cached AE2 output did not flush after storage returned");
        require(itemCount(node, probe.target.output()) == probe.before + 3,
                "Cached AE2 output was lost or inserted more than once");

        probe.crafter.onChunkUnload();
        require(probe.crafter.getProxy().getNode() == null,
                "AE2 node survived chunk-unload destruction");
        require(!probe.crafter.pushPattern(probe.pattern, tableFor(probe.target, true)),
                "EMC Crafter accepted a pattern while its AE2 node was unloaded");
        require(!hasCachedOutput(probe.crafter),
                "Rejected disconnected AE2 execution changed the output cache");
        probe.crafter.onLoad();
        require(probe.crafter.getProxy().getNode() != null,
                "AE2 node did not reconstruct after machine reload");

        writeKnowledge(probe.playerFile, false);
        TransmutationOffline.clear(probe.owner);
        probe.crafter.refreshForKnowledgeOwner(probe.owner);
        require(!probe.crafter.pushPattern(probe.pattern, tableFor(probe.target, true)),
                "Stale AE2 knowledge target executed after authorization was removed");
    }

    private static void validatePatternEncoding(EmcAePattern pattern, EmcCraftingTarget target) {
        IAEItemStack[] inputs = pattern.getCondensedInputs();
        require(inputs.length == target.fluidInputs().size() && inputs.length <= 5,
                "AE2 pattern did not condense EMC fluid tiers into at most five inputs");
        for (int i = 0; i < inputs.length; i++) {
            EmcFluidInput expected = target.fluidInputs().get(i);
            require(inputs[i] != null && inputs[i].getItem() == FcItems.FLUID_DROP,
                    "AE2 EMC input is not an AE2FC Fluid Drop");
            require(inputs[i].getStackSize() == expected.amount(),
                    "AE2 Fluid Drop amount does not equal the exact mB cost");
        }
        require(!pattern.isCraftable(), "EMC AE2 pattern was not marked as processing");
    }

    private static InventoryCrafting tableFor(EmcCraftingTarget target, boolean packets) {
        InventoryCrafting table = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
                return false;
            }
        }, 3, 3);
        int slot = 0;
        for (EmcFluidInput input : target.fluidInputs()) {
            ItemStack stack = packets
                    ? ItemFluidPacket.newStack(input.stack())
                    : ItemFluidDrop.newStack(input.stack());
            table.setInventorySlotContents(slot++, stack);
        }
        return table;
    }

    private static int itemCount(IGridNode node, ItemStack prototype) {
        IItemStorageChannel channel = AEApi.instance().storage()
                .getStorageChannel(IItemStorageChannel.class);
        IAEItemStack key = channel.createStack(prototype);
        if (key == null) {
            return 0;
        }
        appeng.api.networking.storage.IStorageGrid storage = node.getGrid()
                .getCache(appeng.api.networking.storage.IStorageGrid.class);
        IMEMonitor<IAEItemStack> inventory = storage.getInventory(channel);
        IAEItemStack stored = inventory.getStorageList().findPrecise(key);
        return stored == null ? 0 : (int) stored.getStackSize();
    }

    private static long fluidAmount(IGridNode node, FluidStack prototype) {
        IFluidStorageChannel channel = AEApi.instance().storage()
                .getStorageChannel(IFluidStorageChannel.class);
        IAEFluidStack key = channel.createStack(prototype);
        if (key == null) {
            return 0L;
        }
        IStorageGrid storage = node.getGrid().getCache(IStorageGrid.class);
        IMEMonitor<IAEFluidStack> inventory = storage.getInventory(channel);
        IAEFluidStack stored = inventory.getStorageList().findPrecise(key);
        return stored == null ? 0L : stored.getStackSize();
    }

    private static boolean hasCachedOutput(EmcCrafterAe2Integration crafter) {
        for (int slot = 0; slot < crafter.getOutputCache().getSlots(); slot++) {
            if (!crafter.getOutputCache().getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static int cachedOutputCount(EmcCrafterAe2Integration crafter, ItemStack prototype) {
        int count = 0;
        for (int slot = 0; slot < crafter.getOutputCache().getSlots(); slot++) {
            ItemStack stack = crafter.getOutputCache().getStackInSlot(slot);
            if (!stack.isEmpty() && stack.isItemEqual(prototype)
                    && ItemStack.areItemStackTagsEqual(stack, prototype)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static String describeNode(@Nullable IGridNode node) {
        if (node == null) {
            return "node=null";
        }
        IEnergyGrid energy = node.getGrid().getCache(IEnergyGrid.class);
        IPathingGrid pathing = node.getGrid().getCache(IPathingGrid.class);
        return "connections=" + node.getConnections().size()
                + ", powered=" + energy.isNetworkPowered()
                + ", booting=" + pathing.isNetworkBooting()
                + ", meetsChannel=" + node.meetsChannelRequirements();
    }

    private static EmcCraftingTarget findTarget(EmcCrafterAe2Integration crafter) {
        for (EmcCraftingTarget target : crafter.getTargets()) {
            if (target.output().getItem() == Items.DIAMOND) {
                return target;
            }
        }
        throw new IllegalStateException("EMC Crafter did not resolve the Stage G knowledge target");
    }

    private static void writeKnowledge(File playerFile, boolean includeDiamond) {
        IKnowledgeProvider provider = ProjectEAPI.KNOWLEDGE_CAPABILITY.getDefaultInstance();
        if (includeDiamond) {
            require(provider.addKnowledge(new ItemStack(Items.DIAMOND)),
                    "Unable to prepare Stage G knowledge");
        }
        File parent = playerFile.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create Stage G playerdata directory");
        }
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound forgeCaps = new NBTTagCompound();
        forgeCaps.setTag("projecte:knowledge", provider.serializeNBT());
        root.setTag("ForgeCaps", forgeCaps);
        try (FileOutputStream output = new FileOutputStream(playerFile)) {
            CompressedStreamTools.writeCompressed(root, output);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write Stage G offline knowledge", exception);
        }
    }

    private static void cleanup(ProbeContext probe) {
        probe.crafter.getPattern().extractItem(0, Integer.MAX_VALUE, false);
        for (int slot = 0; slot < probe.crafter.getOutputCache().getSlots(); slot++) {
            probe.crafter.getOutputCache().setStackInSlot(slot, ItemStack.EMPTY);
        }
        for (int slot = 0; slot < probe.drive.getInternalInventory().getSlots(); slot++) {
            probe.drive.getInternalInventory().extractItem(slot, Integer.MAX_VALUE, false);
        }
        probe.world.setBlockToAir(probe.cpuPos);
        probe.world.setBlockToAir(probe.discretizerPos);
        probe.world.setBlockToAir(probe.crafterPos);
        probe.world.setBlockToAir(probe.drivePos);
        probe.world.setBlockToAir(probe.energyPos);
        probe.cpuSaved.restore(probe.world);
        probe.discretizerSaved.restore(probe.world);
        probe.crafterSaved.restore(probe.world);
        probe.driveSaved.restore(probe.world);
        probe.energySaved.restore(probe.world);
        TransmutationOffline.clear(probe.owner);
        KnowledgePatternSync.clear();
        if (probe.playerFile.exists() && !probe.playerFile.delete()) {
            throw new IllegalStateException("Unable to remove Stage G probe player data " + probe.playerFile);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class ProbeContext {
        private final WorldServer world;
        private final UUID owner;
        private final File playerFile;
        private final BlockPos energyPos;
        private final BlockPos drivePos;
        private final BlockPos crafterPos;
        private final BlockPos discretizerPos;
        private final BlockPos cpuPos;
        private final SavedBlock energySaved;
        private final SavedBlock driveSaved;
        private final SavedBlock crafterSaved;
        private final SavedBlock discretizerSaved;
        private final SavedBlock cpuSaved;
        private final TileDrive drive;
        private final EmcCrafterAe2Integration crafter;
        private final EmcCraftingTarget target;
        private int phase;
        private int before;
        private int asyncWait;
        private EmcAePattern pattern;
        private ICraftingGrid crafting;
        private Future<ICraftingJob> calculation;
        private TileFluidDiscretizer discretizer;
        private ItemStack storageCell = ItemStack.EMPTY;

        private ProbeContext(WorldServer world, UUID owner, File playerFile,
                             BlockPos energyPos, BlockPos drivePos, BlockPos crafterPos,
                             BlockPos discretizerPos, BlockPos cpuPos,
                             SavedBlock energySaved, SavedBlock driveSaved, SavedBlock crafterSaved,
                             SavedBlock discretizerSaved, SavedBlock cpuSaved,
                             TileDrive drive, EmcCrafterAe2Integration crafter,
                             EmcCraftingTarget target) {
            this.world = world;
            this.owner = owner;
            this.playerFile = playerFile;
            this.energyPos = energyPos;
            this.drivePos = drivePos;
            this.crafterPos = crafterPos;
            this.discretizerPos = discretizerPos;
            this.cpuPos = cpuPos;
            this.energySaved = energySaved;
            this.driveSaved = driveSaved;
            this.crafterSaved = crafterSaved;
            this.discretizerSaved = discretizerSaved;
            this.cpuSaved = cpuSaved;
            this.drive = drive;
            this.crafter = crafter;
            this.target = target;
        }
    }

    private static final class SavedBlock {
        private final BlockPos pos;
        private final IBlockState state;
        @Nullable
        private final NBTTagCompound tileTag;

        private SavedBlock(BlockPos pos, IBlockState state, @Nullable NBTTagCompound tileTag) {
            this.pos = pos;
            this.state = state;
            this.tileTag = tileTag;
        }

        private static SavedBlock capture(WorldServer world, BlockPos pos) {
            TileEntity tile = world.getTileEntity(pos);
            return new SavedBlock(pos, world.getBlockState(pos),
                    tile == null ? null : tile.writeToNBT(new NBTTagCompound()));
        }

        private void restore(WorldServer world) {
            world.setBlockState(pos, state, 3);
            if (tileTag != null) {
                TileEntity tile = world.getTileEntity(pos);
                require(tile != null, "Unable to restore Stage G probe TileEntity");
                tile.readFromNBT(tileTag);
                tile.markDirty();
            }
        }
    }
}
