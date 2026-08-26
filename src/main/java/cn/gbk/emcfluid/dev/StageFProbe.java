package cn.gbk.emcfluid.dev;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import cn.gbk.emcfluid.util.KnowledgePatternSync;
import com.raoulvdberge.refinedstorage.RSBlocks;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingManager;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPattern;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternContainer;
import com.raoulvdberge.refinedstorage.api.autocrafting.registry.ICraftingTaskFactory;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeProxy;
import com.raoulvdberge.refinedstorage.api.storage.AccessType;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDiskContainerContext;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.apiimpl.storage.disk.StorageDiskFluid;
import com.raoulvdberge.refinedstorage.apiimpl.storage.disk.StorageDiskItem;
import com.raoulvdberge.refinedstorage.capability.CapabilityNetworkNodeProxy;
import com.raoulvdberge.refinedstorage.tile.TileController;
import cn.gbk.emcfluid.integration.rs.EmcCrafterNetworkNode;
import cn.gbk.emcfluid.integration.rs.RsIntegration;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.impl.TransmutationOffline;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class StageFProbe {
    private static final IStorageDiskContainerContext READ_WRITE = new IStorageDiskContainerContext() {
        @Override
        public AccessType getAccessType() {
            return AccessType.INSERT_EXTRACT;
        }
    };

    private static MinecraftServer pendingServer;
    private static boolean registered;

    private StageFProbe() {
    }

    public static void schedule(MinecraftServer server) {
        if (!registered) {
            MinecraftForge.EVENT_BUS.register(StageFProbe.class);
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
        MinecraftServer server = pendingServer;
        pendingServer = null;
        run(server);
    }

    public static void run(MinecraftServer server) {
        require(RsIntegration.API != null, "Refined Storage API was not injected");
        WorldServer world = server.getWorld(0);
        UUID owner = UUID.randomUUID();
        File playerFile = new File(new File(
                DimensionManager.getCurrentSaveRootDirectory(), "playerdata"), owner + ".dat");

        BlockPos top = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());
        BlockPos controllerPos = new BlockPos(top.getX() + 4,
                Math.min(world.getHeight() - 2, top.getY() + 4), top.getZ());
        BlockPos crafterPos = controllerPos.east();
        SavedBlock controllerSaved = SavedBlock.capture(world, controllerPos);
        SavedBlock crafterSaved = SavedBlock.capture(world, crafterPos);

        try {
            writeKnowledge(playerFile, Items.DIAMOND);
            TransmutationOffline.clear(owner);

            ProbeNetwork probe = createNetwork(world, controllerPos, crafterPos, owner);
            validateMissingPreview(probe);
            validateBatchAndPersistence(probe);
            validateRecursiveCrafting(probe);
            validatePowerAndOutputBackpressure(probe);
            validateCancellationAndDisconnect(probe);
            validateInvalidRestoredTarget(probe, playerFile, owner);
        } finally {
            cleanup(world, controllerPos, crafterPos);
            crafterSaved.restore(world);
            controllerSaved.restore(world);
            TransmutationOffline.clear(owner);
            KnowledgePatternSync.clear();
            if (playerFile.exists() && !playerFile.delete()) {
                throw new IllegalStateException("Unable to remove Stage F probe player data " + playerFile);
            }
        }

        EmcFluid.logger.info("Validated Stage F RS patterns, missing previews, batch and recursive crafting, cancellation, task restore, power loss, disconnect, and output backpressure");
    }

    private static ProbeNetwork createNetwork(WorldServer world, BlockPos controllerPos,
                                              BlockPos crafterPos, UUID owner) {
        world.setBlockToAir(crafterPos);
        world.setBlockToAir(controllerPos);
        require(world.setBlockState(controllerPos, RSBlocks.CONTROLLER.getDefaultState(), 3),
                "Unable to place RS controller");
        require(world.setBlockState(crafterPos, ModContent.emcCrafter.getDefaultState(), 3),
                "Unable to place EMC Crafter");

        TileEntity controllerTile = world.getTileEntity(controllerPos);
        TileEntity crafterTile = world.getTileEntity(crafterPos);
        require(controllerTile instanceof TileController, "RS controller TileEntity is missing");
        require(crafterTile instanceof EmcCrafterBlockEntity, "EMC Crafter TileEntity is missing");
        TileController controller = (TileController) controllerTile;
        EmcCrafterBlockEntity crafter = (EmcCrafterBlockEntity) crafterTile;

        ItemStack knowledgePattern = new ItemStack(ModContent.knowledgePattern);
        require(KnowledgePatternData.bind(knowledgePattern, owner, "StageFProbe"),
                "Unable to bind Stage F Knowledge Pattern");
        require(crafter.getPattern().insertItem(0, knowledgePattern, false).isEmpty(),
                "EMC Crafter rejected Stage F Knowledge Pattern");
        EmcCraftingTarget target = findTarget(crafter, Items.DIAMOND);

        controller.getEnergy().setStored(controller.getEnergy().getCapacity());
        controller.getNodeGraph().invalidate(Action.PERFORM, world, controllerPos);
        INetworkNodeProxy proxy = crafter.getCapability(
                CapabilityNetworkNodeProxy.NETWORK_NODE_PROXY_CAPABILITY, EnumFacing.WEST);
        require(proxy != null && proxy.getNode() instanceof EmcCrafterNetworkNode,
                "EMC Crafter did not expose its RS node proxy");
        EmcCrafterNetworkNode node = (EmcCrafterNetworkNode) proxy.getNode();
        node.update();
        controller.getCraftingManager().rebuild();
        require(controller.getCraftingManager().getPattern(target.output()) != null,
                "RS crafting manager did not publish the EMC target");

        StorageDiskItem items = new StorageDiskItem(world, 4096);
        StorageDiskFluid fluids = new StorageDiskFluid(world, 1_000_000);
        items.setSettings(null, READ_WRITE);
        fluids.setSettings(null, READ_WRITE);
        controller.getItemStorageCache().getStorages().add(items);
        controller.getFluidStorageCache().getStorages().add(fluids);
        return new ProbeNetwork(controller, crafter, node, target, items, fluids);
    }

    private static void validateMissingPreview(ProbeNetwork probe) {
        removeAllTargetFluids(probe);
        ICraftingTask task = requireTask(probe.network, probe.target.output(), 3);
        require(task.calculate() == null, "RS missing-fluid calculation returned a structural error");
        require(task.hasMissing(), "RS missing-fluid preview reported no missing inputs");
        for (FluidStack required : probe.target.fluidStacksForQuantity(3)) {
            FluidStack missing = task.getMissingFluids().get(required);
            require(missing != null && missing.amount == required.amount,
                    "RS missing-fluid preview amount mismatch for " + required.getFluid().getName());
        }
    }

    private static void validateBatchAndPersistence(ProbeNetwork probe) {
        removeAllTargetFluids(probe);
        insertTargetFluids(probe, 3);
        int before = itemCount(probe.network, probe.target.output());
        ICraftingTask task = requireTask(probe.network, probe.target.output(), 3);
        require(task.calculate() == null && !task.hasMissing(),
                "RS batch task calculation failed with available fluids");

        NBTTagCompound saved = task.writeToNbt(new NBTTagCompound());
        ICraftingTaskFactory factory = RsIntegration.API.getCraftingTaskRegistry().get(RsIntegration.TASK_ID);
        require(factory != null, "EMC Fluid RS task factory is not registered");
        ICraftingTask restored;
        try {
            restored = factory.createFromNbt(probe.network, saved);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to restore calculated RS task", exception);
        }
        require(restored.getId().equals(task.getId()) && restored.getQuantity() == 3,
                "RS task identity or quantity changed after NBT restore");
        runTask(probe, restored, 80);
        require(itemCount(probe.network, probe.target.output()) == before + 3,
                "Restored RS batch task produced the wrong output quantity");
        requireTargetFluidCount(probe, 0);
    }

    private static void validateRecursiveCrafting(ProbeNetwork probe) {
        removeAllTargetFluids(probe);
        ICraftingManager manager = probe.network.getCraftingManager();
        List<ICraftingPattern> added = new ArrayList<ICraftingPattern>();
        int index = 0;
        for (FluidStack input : probe.target.fluidStacksForQuantity(1)) {
            FluidSourceContainer container = new FluidSourceContainer(
                    probe.network, probe.crafter.getPos().up(2 + index++), input);
            ICraftingPattern pattern = new FluidSourcePattern(container, input);
            container.pattern = pattern;
            manager.getPatterns().add(pattern);
            added.add(pattern);
        }

        int before = itemCount(probe.network, probe.target.output());
        ICraftingTask task = requireTask(probe.network, probe.target.output(), 2);
        require(task.calculate() == null && !task.hasMissing(),
                "RS recursive fluid calculation did not resolve child patterns");
        runTask(probe, task, 120);
        require(itemCount(probe.network, probe.target.output()) == before + 2,
                "RS recursive crafting produced the wrong quantity");
        manager.getPatterns().removeAll(added);
    }

    private static void validatePowerAndOutputBackpressure(ProbeNetwork probe) {
        removeAllTargetFluids(probe);
        insertTargetFluids(probe, 1);
        ICraftingTask paused = requireTask(probe.network, probe.target.output(), 1);
        require(paused.calculate() == null && !paused.hasMissing(), "Power-loss task calculation failed");
        int fluidBefore = totalTargetFluidCount(probe);
        probe.controller.getEnergy().setStored(0);
        probe.network.getCraftingManager().add(paused);
        probe.network.getCraftingManager().update();
        require(totalTargetFluidCount(probe) == fluidBefore,
                "Controller power loss consumed EMC fluid");
        probe.controller.getEnergy().setStored(probe.controller.getEnergy().getCapacity());
        runExistingTask(probe, paused, 80);

        removeAllTargetFluids(probe);
        insertTargetFluids(probe, 1);
        fillItemStorage(probe);
        int before = itemCount(probe.network, probe.target.output());
        ICraftingTask blocked = requireTask(probe.network, probe.target.output(), 1);
        require(blocked.calculate() == null && !blocked.hasMissing(), "Backpressure task calculation failed");
        probe.network.getCraftingManager().add(blocked);
        for (int i = 0; i < 8; i++) {
            probe.network.getCraftingManager().update();
            probe.node.update();
        }
        require(hasCachedOutputs(probe.node), "Full RS item storage did not retain the output in node cache");
        require(itemCount(probe.network, probe.target.output()) == before,
                "Full RS item storage accepted an output unexpectedly");

        ItemStack removed = probe.network.extractItem(new ItemStack(Blocks.COBBLESTONE),
                1, Action.PERFORM);
        require(removed != null && removed.getCount() == 1, "Unable to free RS item storage space");
        runExistingTask(probe, blocked, 80);
        require(!hasCachedOutputs(probe.node)
                        && itemCount(probe.network, probe.target.output()) == before + 1,
                "Cached RS output did not flush exactly once after backpressure cleared");
        freeItemStorage(probe, 128);
    }

    private static void validateCancellationAndDisconnect(ProbeNetwork probe) {
        removeAllTargetFluids(probe);
        insertTargetFluids(probe, 1);
        fillNodeCache(probe.node);
        int before = totalTargetFluidCount(probe);
        ICraftingTask cancelled = requireTask(probe.network, probe.target.output(), 1);
        require(cancelled.calculate() == null && !cancelled.hasMissing(), "Cancellation task calculation failed");
        probe.network.getCraftingManager().add(cancelled);
        probe.network.getCraftingManager().update();
        require(totalTargetFluidCount(probe) < before,
                "Blocked task did not move its fluids into protected internal storage");
        probe.network.getCraftingManager().cancel(cancelled.getId());
        probe.network.getCraftingManager().update();
        require(totalTargetFluidCount(probe) == before,
                "Cancelling a blocked task did not refund all EMC fluid");
        clearNodeCache(probe.node);

        removeAllTargetFluids(probe);
        insertTargetFluids(probe, 1);
        fillNodeCache(probe.node);
        before = totalTargetFluidCount(probe);
        ICraftingTask disconnected = requireTask(probe.network, probe.target.output(), 1);
        require(disconnected.calculate() == null && !disconnected.hasMissing(), "Disconnect task calculation failed");
        probe.network.getCraftingManager().add(disconnected);
        probe.network.getCraftingManager().update();
        probe.node.onDisconnected(probe.network);
        probe.network.getCraftingManager().update();
        require(totalTargetFluidCount(probe) == before,
                "Disconnecting the EMC Crafter did not refund the active root task");
        clearNodeCache(probe.node);
        probe.node.onConnected(probe.network);
    }

    private static void validateInvalidRestoredTarget(ProbeNetwork probe, File playerFile, UUID owner) {
        removeAllTargetFluids(probe);
        insertTargetFluids(probe, 1);
        ICraftingTask task = requireTask(probe.network, probe.target.output(), 1);
        require(task.calculate() == null && !task.hasMissing(), "Invalidation task calculation failed");
        NBTTagCompound saved = task.writeToNbt(new NBTTagCompound());
        int fluidBefore = totalTargetFluidCount(probe);
        int outputBefore = itemCount(probe.network, probe.target.output());

        writeKnowledge(playerFile, Items.EMERALD);
        TransmutationOffline.clear(owner);
        probe.crafter.refreshForKnowledgeOwner(owner);

        ICraftingTaskFactory factory = RsIntegration.API.getCraftingTaskRegistry().get(RsIntegration.TASK_ID);
        try {
            ICraftingTask restored = factory.createFromNbt(probe.network, saved);
            probe.network.getCraftingManager().add(restored);
            probe.network.getCraftingManager().update();
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid target task could not be safely restored", exception);
        }
        require(totalTargetFluidCount(probe) == fluidBefore
                        && itemCount(probe.network, probe.target.output()) == outputBefore,
                "Restored task used a ProjectE knowledge target that is no longer authorized");

        writeKnowledge(playerFile, Items.DIAMOND);
        TransmutationOffline.clear(owner);
        probe.crafter.refreshForKnowledgeOwner(owner);
    }

    private static void runTask(ProbeNetwork probe, ICraftingTask task, int maxTicks) {
        probe.network.getCraftingManager().add(task);
        runExistingTask(probe, task, maxTicks);
    }

    private static void runExistingTask(ProbeNetwork probe, ICraftingTask task, int maxTicks) {
        for (int tick = 0; tick < maxTicks; tick++) {
            probe.network.getCraftingManager().update();
            probe.node.update();
            if (probe.network.getCraftingManager().getTask(task.getId()) == null
                    && !hasCachedOutputs(probe.node)) {
                return;
            }
        }
        ICraftingTask active = probe.network.getCraftingManager().getTask(task.getId());
        throw new IllegalStateException("RS task did not finish within " + maxTicks
                + " probe ticks; active=" + (active != null)
                + ", completion=" + (active == null ? -1 : active.getCompletionPercentage())
                + ", missing=" + (active != null && active.hasMissing())
                + ", cached=" + hasCachedOutputs(probe.node)
                + ", outputCount=" + itemCount(probe.network, probe.target.output())
                + ", fluidCount=" + totalTargetFluidCount(probe)
                + ", processing=" + describeProcessing(active));
    }

    private static ICraftingTask requireTask(INetwork network, ItemStack output, int quantity) {
        ICraftingTask task = network.getCraftingManager().create(output, quantity);
        require(task != null, "RS crafting manager could not create the EMC task");
        return task;
    }

    private static EmcCraftingTarget findTarget(EmcCrafterBlockEntity crafter, net.minecraft.item.Item item) {
        for (EmcCraftingTarget target : crafter.getTargets()) {
            if (target.output().getItem() == item) {
                return target;
            }
        }
        throw new IllegalStateException("EMC Crafter did not resolve the Stage F knowledge target");
    }

    private static void insertTargetFluids(ProbeNetwork probe, int quantity) {
        for (FluidStack stack : probe.target.fluidStacksForQuantity(quantity)) {
            FluidStack remainder = probe.network.insertFluid(stack, stack.amount, Action.PERFORM);
            require(remainder == null, "RS fluid storage rejected EMC fluid " + stack.getFluid().getName());
        }
    }

    private static void removeAllTargetFluids(ProbeNetwork probe) {
        for (FluidStack stack : probe.target.fluidStacksForQuantity(1)) {
            FluidStack current = probe.network.extractFluid(stack, Integer.MAX_VALUE, Action.SIMULATE);
            if (current != null) {
                probe.network.extractFluid(stack, current.amount, Action.PERFORM);
            }
        }
    }

    private static int totalTargetFluidCount(ProbeNetwork probe) {
        int total = 0;
        for (FluidStack stack : probe.target.fluidStacksForQuantity(1)) {
            FluidStack current = probe.network.extractFluid(stack, Integer.MAX_VALUE, Action.SIMULATE);
            total += current == null ? 0 : current.amount;
        }
        return total;
    }

    private static void requireTargetFluidCount(ProbeNetwork probe, int expected) {
        require(totalTargetFluidCount(probe) == expected,
                "RS EMC fluid remainder mismatch: expected " + expected
                        + ", got " + totalTargetFluidCount(probe));
    }

    private static int itemCount(INetwork network, ItemStack prototype) {
        ItemStack stack = network.extractItem(prototype, Integer.MAX_VALUE, Action.SIMULATE);
        return stack == null ? 0 : stack.getCount();
    }

    private static void fillItemStorage(ProbeNetwork probe) {
        int remaining = probe.items.getCapacity() - probe.items.getStored();
        if (remaining > 0) {
            ItemStack cobble = new ItemStack(Blocks.COBBLESTONE);
            ItemStack remainder = probe.network.insertItem(cobble, remaining, Action.PERFORM);
            require(remainder == null, "Unable to fill RS item storage for backpressure test");
        }
        require(probe.items.getStored() == probe.items.getCapacity(), "RS item disk is not full");
    }

    private static void freeItemStorage(ProbeNetwork probe, int amount) {
        probe.network.extractItem(new ItemStack(Blocks.COBBLESTONE), amount, Action.PERFORM);
    }

    private static ItemStackHandler nodeCache(EmcCrafterNetworkNode node) {
        try {
            Field field = EmcCrafterNetworkNode.class.getDeclaredField("outputCache");
            field.setAccessible(true);
            return (ItemStackHandler) field.get(node);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access development RS output cache", exception);
        }
    }

    private static String describeProcessing(@Nullable ICraftingTask task) {
        if (task == null) {
            return "none";
        }
        try {
            Object craftingTask = task;
            Field delegate = task.getClass().getDeclaredField("delegate");
            delegate.setAccessible(true);
            craftingTask = delegate.get(task);
            Field processing = craftingTask.getClass().getDeclaredField("processing");
            processing.setAccessible(true);
            List<?> entries = (List<?>) processing.get(craftingTask);
            List<String> descriptions = new ArrayList<String>();
            for (Object entry : entries) {
                java.lang.reflect.Method getState = entry.getClass().getDeclaredMethod("getState");
                java.lang.reflect.Method getPattern = entry.getClass().getDeclaredMethod("getPattern");
                getState.setAccessible(true);
                getPattern.setAccessible(true);
                Object pattern = getPattern.invoke(entry);
                descriptions.add(pattern.getClass().getSimpleName() + ":" + getState.invoke(entry));
            }
            return descriptions.toString();
        } catch (ReflectiveOperationException exception) {
            return "unavailable:" + exception.getClass().getSimpleName();
        }
    }

    private static void fillNodeCache(EmcCrafterNetworkNode node) {
        ItemStackHandler cache = nodeCache(node);
        for (int slot = 0; slot < cache.getSlots(); slot++) {
            cache.setStackInSlot(slot, new ItemStack(Blocks.COBBLESTONE, 64));
        }
    }

    private static void clearNodeCache(EmcCrafterNetworkNode node) {
        ItemStackHandler cache = nodeCache(node);
        for (int slot = 0; slot < cache.getSlots(); slot++) {
            cache.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    private static boolean hasCachedOutputs(EmcCrafterNetworkNode node) {
        ItemStackHandler cache = nodeCache(node);
        for (int slot = 0; slot < cache.getSlots(); slot++) {
            if (!cache.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static void writeKnowledge(File playerFile, net.minecraft.item.Item item) {
        IKnowledgeProvider provider = ProjectEAPI.KNOWLEDGE_CAPABILITY.getDefaultInstance();
        require(provider.addKnowledge(new ItemStack(item)), "Unable to prepare Stage F knowledge");
        File parent = playerFile.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create Stage F playerdata directory");
        }
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound forgeCaps = new NBTTagCompound();
        forgeCaps.setTag("projecte:knowledge", provider.serializeNBT());
        root.setTag("ForgeCaps", forgeCaps);
        try (FileOutputStream output = new FileOutputStream(playerFile)) {
            CompressedStreamTools.writeCompressed(root, output);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write Stage F offline knowledge", exception);
        }
    }

    private static void cleanup(WorldServer world, BlockPos controllerPos, BlockPos crafterPos) {
        TileEntity tile = world.getTileEntity(crafterPos);
        if (tile instanceof EmcCrafterBlockEntity) {
            ((EmcCrafterBlockEntity) tile).getPattern().extractItem(0, Integer.MAX_VALUE, false);
            INetworkNodeProxy proxy = tile.getCapability(
                    CapabilityNetworkNodeProxy.NETWORK_NODE_PROXY_CAPABILITY, null);
            if (proxy != null && proxy.getNode() instanceof EmcCrafterNetworkNode) {
                clearNodeCache((EmcCrafterNetworkNode) proxy.getNode());
            }
        }
        world.setBlockToAir(crafterPos);
        world.setBlockToAir(controllerPos);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class ProbeNetwork {
        private final TileController controller;
        private final EmcCrafterBlockEntity crafter;
        private final EmcCrafterNetworkNode node;
        private final EmcCraftingTarget target;
        private final StorageDiskItem items;
        @SuppressWarnings("unused")
        private final StorageDiskFluid fluids;
        private final INetwork network;

        private ProbeNetwork(TileController controller, EmcCrafterBlockEntity crafter,
                             EmcCrafterNetworkNode node, EmcCraftingTarget target,
                             StorageDiskItem items, StorageDiskFluid fluids) {
            this.controller = controller;
            this.crafter = crafter;
            this.node = node;
            this.target = target;
            this.items = items;
            this.fluids = fluids;
            this.network = controller;
        }
    }

    private static final class FluidSourcePattern implements ICraftingPattern {
        private final FluidSourceContainer container;
        private final FluidStack output;

        private FluidSourcePattern(FluidSourceContainer container, FluidStack output) {
            this.container = container;
            this.output = output.copy();
        }

        @Override public ICraftingPatternContainer getContainer() { return container; }
        @Override public ItemStack getStack() { return ItemStack.EMPTY; }
        @Override public boolean isValid() { return true; }
        @Override public boolean isProcessing() { return true; }
        @Override public boolean isOredict() { return false; }
        @Override public List<NonNullList<ItemStack>> getInputs() { return Collections.emptyList(); }
        @Override public NonNullList<ItemStack> getOutputs() { return NonNullList.create(); }
        @Override public ItemStack getOutput(NonNullList<ItemStack> took) { return ItemStack.EMPTY; }
        @Override public NonNullList<ItemStack> getByproducts() { return NonNullList.create(); }
        @Override public NonNullList<ItemStack> getByproducts(NonNullList<ItemStack> took) { return NonNullList.create(); }
        @Override public NonNullList<FluidStack> getFluidInputs() { return NonNullList.create(); }
        @Override public NonNullList<FluidStack> getFluidOutputs() {
            NonNullList<FluidStack> outputs = NonNullList.create();
            outputs.add(output.copy());
            return outputs;
        }
        @Override public String getId() { return "normal"; }
        @Override public boolean canBeInChainWith(ICraftingPattern other) { return other == this; }
        @Override public int getChainHashCode() { return System.identityHashCode(this); }
    }

    private static final class FluidSourceContainer implements ICraftingPatternContainer {
        private final ItemStackHandler emptyItemInput = new ItemStackHandler(0);
        private final INetwork network;
        private final BlockPos pos;
        private final FluidStack output;
        private final UUID uuid = UUID.randomUUID();
        private ICraftingPattern pattern;

        private FluidSourceContainer(INetwork network, BlockPos pos, FluidStack output) {
            this.network = network;
            this.pos = pos;
            this.output = output.copy();
        }

        @Override public int getUpdateInterval() { return 0; }
        @Override public IItemHandler getConnectedInventory() { return emptyItemInput; }
        @Nullable @Override public IFluidHandler getConnectedFluidInventory() { return null; }
        @Nullable @Override public TileEntity getConnectedTile() { return null; }
        @Override public TileEntity getFacingTile() { return null; }
        @Override public EnumFacing getDirection() { return EnumFacing.NORTH; }
        @Override public List<ICraftingPattern> getPatterns() { return Collections.singletonList(pattern); }
        @Nullable @Override public IItemHandlerModifiable getPatternInventory() { return null; }
        @Override public String getName() { return "Stage F recursive fluid source"; }
        @Override public BlockPos getPosition() { return pos; }
        @Override public ICraftingPatternContainer getRootContainer() { return this; }
        @Override public UUID getUuid() { return uuid; }
        @Override public void onUsedForProcessing() {
            int remainder = network.getCraftingManager().track(output, output.amount);
            require(remainder == 0, "Recursive source fluid was not fully tracked");
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
                require(tile != null, "Unable to restore Stage F probe TileEntity");
                tile.readFromNBT(tileTag);
                tile.markDirty();
            }
        }
    }
}
