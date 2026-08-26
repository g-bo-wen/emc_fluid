package cn.gbk.emcfluid.dev;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.MachineTileEntity;
import cn.gbk.emcfluid.content.item.KnowledgePatternItem;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.CostResolver;
import cn.gbk.emcfluid.util.EmcCraftingTarget;
import cn.gbk.emcfluid.util.EmcFluidInput;
import cn.gbk.emcfluid.util.EmcFluidTierConfig;
import cn.gbk.emcfluid.util.EmcStackIdentity;
import cn.gbk.emcfluid.util.KnowledgePatternData;
import cn.gbk.emcfluid.util.KnowledgePatternSync;
import cn.gbk.emcfluid.util.ProjectEAccess;
import com.mojang.authlib.GameProfile;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.event.PlayerKnowledgeChangeEvent;
import moze_intel.projecte.api.item.IItemEmc;
import moze_intel.projecte.impl.TransmutationOffline;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.items.ItemStackHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EmcFluid.MODID)
public final class StageDProbe {
    private static MinecraftServer pendingServer;

    private StageDProbe() {
    }

    public static void schedule(MinecraftServer server) {
        pendingServer = server;
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || pendingServer == null || !pendingServer.isServerRunning()) {
            return;
        }
        MinecraftServer server = pendingServer;
        pendingServer = null;
        run(server);
    }

    public static void run(MinecraftServer server) {
        validateEmcAccess();
        validateIdentityAndCost();

        WorldServer world = server.getWorld(0);
        UUID owner = UUID.randomUUID();
        FakePlayer player = FakePlayerFactory.get(world, new GameProfile(owner, "EmcFluidKnowledgeProbe"));
        File playerFile = new File(new File(DimensionManager.getCurrentSaveRootDirectory(), "playerdata"), owner + ".dat");

        BlockPos top = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());
        BlockPos pos = new BlockPos(top.getX() + 1, Math.min(world.getHeight() - 2, top.getY() + 4), top.getZ());
        IBlockState originalState = world.getBlockState(pos);
        TileEntity originalTile = world.getTileEntity(pos);
        NBTTagCompound originalTileTag = originalTile == null ? null : originalTile.writeToNBT(new NBTTagCompound());

        try {
            validateBindingAndOfflineKnowledge(world, pos, player, playerFile);
        } finally {
            restore(world, pos, originalState, originalTileTag);
            TransmutationOffline.clear(owner);
            if (playerFile.exists() && !playerFile.delete()) {
                throw new IllegalStateException("Unable to remove Stage D probe player data " + playerFile);
            }
        }

        EmcFluid.logger.info("Validated Stage D ProjectE EMC access, bound/offline/full knowledge, stable targets, refresh, and unbind recipe");
    }

    private static void validateEmcAccess() {
        Item kleinItem = Item.REGISTRY.getObject(new ResourceLocation("projecte", "item.pe_klein_star"));
        require(kleinItem != null && kleinItem != Items.AIR, "ProjectE Klein Star is not registered");
        ItemStack star = new ItemStack(kleinItem, 1, 2);

        require(ProjectEAccess.insertEmc(star, 1_000L, false) == 1_000L,
                "Klein Star insert simulation mismatch");
        require(ProjectEAccess.getStoredEmc(star) == 0L, "Klein Star insert simulation mutated the item");
        require(ProjectEAccess.insertEmc(star, 1_000L, true) == 1_000L,
                "Klein Star insert execution mismatch");
        require(ProjectEAccess.extractEmc(star, 400L, false) == 400L,
                "Klein Star extract simulation mismatch");
        require(ProjectEAccess.getStoredEmc(star) == 1_000L, "Klein Star extract simulation mutated the item");
        require(ProjectEAccess.extractEmc(star, 400L, true) == 400L
                        && ProjectEAccess.getStoredEmc(star) == 600L,
                "Klein Star extract execution mismatch");

        ItemStack partial = new ItemStack(new PartialEmcItem());
        require(ProjectEAccess.insertEmc(partial, 100L, false) == 100L,
                "Third-party EMC holder simulation mismatch");
        require(ProjectEAccess.insertEmc(partial, 100L, true) == 50L
                        && ProjectEAccess.getStoredEmc(partial) == 50L,
                "Third-party EMC holder actual return was not honored");
        require(ProjectEAccess.extractEmc(partial, 40L, true) == 20L
                        && ProjectEAccess.getStoredEmc(partial) == 30L,
                "Third-party EMC holder partial extraction was not honored");
    }

    private static void validateIdentityAndCost() {
        ItemStack stack = new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 32, 5);
        NBTTagCompound data = new NBTTagCompound();
        data.setString("ProbeData", "preserved");
        stack.setTagCompound(data);
        ItemStack normalized = ProjectEAccess.normalizeKnowledgeStack(stack);
        require(normalized.getCount() == 1 && normalized.getMetadata() == 5
                        && "preserved".equals(normalized.getTagCompound().getString("ProbeData")),
                "Knowledge stack normalization lost count, metadata, or NBT");

        EmcStackIdentity identity = EmcStackIdentity.fromStack(normalized);
        EmcStackIdentity restored = EmcStackIdentity.readFromNBT(identity.writeToNBT(new NBTTagCompound()));
        require(identity.equals(restored) && identity.stableKey().equals(restored.stableKey())
                        && ItemStack.areItemStacksEqual(normalized, restored.createStack()),
                "Stable EMC target identity did not survive NBT round trip");

        Optional<List<EmcFluidInput>> cost = CostResolver.resolve(8_192L);
        require(cost.isPresent(), "Unable to resolve a positive ProjectE EMC cost");
        long total = 0L;
        for (EmcFluidInput input : cost.get()) {
            total += (long) input.amount() * EmcFluidTierConfig.value(input.tierIndex());
        }
        require(total == 8_192L, "Resolved EMC fluid inputs do not conserve value");
    }

    private static void validateBindingAndOfflineKnowledge(WorldServer world, BlockPos pos, FakePlayer player,
                                                            File playerFile) {
        ItemStack pattern = new ItemStack(ModContent.knowledgePattern);
        player.setHeldItem(EnumHand.MAIN_HAND, pattern);
        ((KnowledgePatternItem) ModContent.knowledgePattern).onItemRightClick(world, player, EnumHand.MAIN_HAND);
        require(KnowledgePatternData.isBoundTo(pattern, player.getUniqueID())
                        && "EmcFluidKnowledgeProbe".equals(KnowledgePatternData.getOwnerName(pattern).orElse("")),
                "Knowledge Pattern did not bind to the server player UUID/name");

        IKnowledgeProvider first = ProjectEAPI.KNOWLEDGE_CAPABILITY.getDefaultInstance();
        ItemStack diamond = new ItemStack(Items.DIAMOND, 64);
        ItemStack wool = new ItemStack(Item.getItemFromBlock(Blocks.WOOL), 8, 5);
        NBTTagCompound woolData = new NBTTagCompound();
        woolData.setString("ProbeData", "offline");
        wool.setTagCompound(woolData);
        require(first.addKnowledge(diamond) && first.addKnowledge(wool), "Unable to prepare offline ProjectE knowledge");
        writeOfflineKnowledge(playerFile, first);
        TransmutationOffline.clear(player.getUniqueID());

        List<ItemStack> knowledge = ProjectEAccess.getKnowledge(player.getUniqueID());
        require(knowledge.size() == 2, "Offline ProjectE knowledge size mismatch");
        require(isSortedAndNormalized(knowledge), "Offline knowledge was not normalized and stably sorted");
        require(findStack(knowledge, Item.getItemFromBlock(Blocks.WOOL), 5, "ProbeData", "offline"),
                "Offline knowledge lost metadata or meaningful NBT");
        require(KnowledgePatternData.readForCrafting(pattern).size() == 2,
                "Bound Knowledge Pattern did not resolve offline knowledge");
        require(ProjectEAccess.getEmcValue(new ItemStack(Items.DIAMOND)) > 0L,
                "ProjectE EMC proxy returned no value for diamond");

        EmcCrafterBlockEntity crafter = placeCrafter(world, pos);
        require(crafter.getPattern().insertItem(0, pattern.copy(), false).isEmpty(),
                "Crafter rejected a bound Knowledge Pattern");
        require(containsTarget(crafter.getTargets(), Items.DIAMOND),
                "Crafter targets did not include initial offline knowledge");

        IKnowledgeProvider changed = ProjectEAPI.KNOWLEDGE_CAPABILITY.getDefaultInstance();
        require(changed.addKnowledge(new ItemStack(Items.EMERALD)), "Unable to prepare changed ProjectE knowledge");
        writeOfflineKnowledge(playerFile, changed);
        TransmutationOffline.clear(player.getUniqueID());
        int previousVersion = KnowledgePatternSync.getKnowledgeVersion(player.getUniqueID());
        KnowledgePatternSync.onPlayerKnowledgeChanged(new PlayerKnowledgeChangeEvent(player));
        require(KnowledgePatternSync.getKnowledgeVersion(player.getUniqueID()) == previousVersion + 1,
                "Knowledge change event did not advance the owner version");
        require(containsTarget(crafter.getTargets(), Items.EMERALD)
                        && !containsTarget(crafter.getTargets(), Items.DIAMOND),
                "Crafter target cache did not refresh after ProjectE knowledge changed");

        IKnowledgeProvider full = ProjectEAPI.KNOWLEDGE_CAPABILITY.getDefaultInstance();
        full.setFullKnowledge(true);
        writeOfflineKnowledge(playerFile, full);
        TransmutationOffline.clear(player.getUniqueID());
        require(ProjectEAccess.hasFullKnowledge(player.getUniqueID()),
                "Offline full-knowledge/Tome flag was not restored");
        require(ProjectEAccess.getKnowledge(player.getUniqueID()).size() > 100,
                "Full-knowledge provider did not expose cached Tome knowledge");

        validateUnbindRecipe(world, pattern);
    }

    private static void validateUnbindRecipe(WorldServer world, ItemStack boundPattern) {
        IRecipe recipe = CraftingManager.REGISTRY.getObject(
                new ResourceLocation(EmcFluid.MODID, "knowledge_pattern_unbind"));
        require(recipe != null, "Knowledge Pattern unbind recipe is not registered");
        InventoryCrafting inventory = new InventoryCrafting(new Container() {
            @Override
            public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer player) {
                return true;
            }
        }, 2, 2);
        inventory.setInventorySlotContents(0, boundPattern.copy());
        require(recipe.matches(inventory, world), "Unbind recipe rejected one bound Knowledge Pattern");
        ItemStack result = recipe.getCraftingResult(inventory);
        require(KnowledgePatternData.isPattern(result) && !KnowledgePatternData.isBound(result),
                "Unbind recipe did not return a fresh unbound Knowledge Pattern");
        inventory.setInventorySlotContents(1, new ItemStack(Items.STICK));
        require(!recipe.matches(inventory, world), "Unbind recipe accepted an extra ingredient");
    }

    private static boolean isSortedAndNormalized(List<ItemStack> knowledge) {
        String previous = "";
        for (ItemStack stack : knowledge) {
            if (stack.getCount() != 1) {
                return false;
            }
            String key = EmcStackIdentity.fromStack(stack).stableKey();
            if (key.compareTo(previous) < 0) {
                return false;
            }
            previous = key;
        }
        return true;
    }

    private static boolean findStack(List<ItemStack> stacks, Item item, int metadata, String tagKey, String tagValue) {
        for (ItemStack stack : stacks) {
            if (stack.getItem() == item && stack.getMetadata() == metadata && stack.hasTagCompound()
                    && tagValue.equals(stack.getTagCompound().getString(tagKey))) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsTarget(List<EmcCraftingTarget> targets, Item item) {
        for (EmcCraftingTarget target : targets) {
            if (target.output().getItem() == item) {
                return true;
            }
        }
        return false;
    }

    private static void writeOfflineKnowledge(File playerFile, IKnowledgeProvider provider) {
        File parent = playerFile.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create playerdata directory for Stage D probe");
        }
        NBTTagCompound root = new NBTTagCompound();
        NBTTagCompound forgeCaps = new NBTTagCompound();
        forgeCaps.setTag("projecte:knowledge", provider.serializeNBT());
        root.setTag("ForgeCaps", forgeCaps);
        try (FileOutputStream output = new FileOutputStream(playerFile)) {
            CompressedStreamTools.writeCompressed(root, output);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to write Stage D offline knowledge probe", exception);
        }
    }

    private static EmcCrafterBlockEntity placeCrafter(WorldServer world, BlockPos pos) {
        world.setBlockToAir(pos);
        require(world.setBlockState(pos, ModContent.emcCrafter.getDefaultState(), 3),
                "Unable to place Crafter for Stage D probe");
        TileEntity tile = world.getTileEntity(pos);
        require(tile instanceof EmcCrafterBlockEntity, "Stage D probe got the wrong Crafter TileEntity");
        return (EmcCrafterBlockEntity) tile;
    }

    private static void restore(WorldServer world, BlockPos pos, IBlockState state, NBTTagCompound tileTag) {
        TileEntity probe = world.getTileEntity(pos);
        if (probe instanceof MachineTileEntity) {
            for (ItemStackHandler handler : ((MachineTileEntity) probe).getDroppableItemHandlers()) {
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    handler.extractItem(slot, Integer.MAX_VALUE, false);
                }
            }
        }
        world.setBlockToAir(pos);
        world.setBlockState(pos, state, 3);
        if (tileTag != null) {
            TileEntity restored = world.getTileEntity(pos);
            require(restored != null, "Unable to restore original TileEntity after Stage D probe");
            restored.readFromNBT(tileTag);
            restored.markDirty();
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static final class PartialEmcItem extends Item implements IItemEmc {
        @Override
        public long addEmc(ItemStack stack, long toAdd) {
            long added = toAdd / 2L;
            setStored(stack, getStoredEmc(stack) + added);
            return added;
        }

        @Override
        public long extractEmc(ItemStack stack, long toRemove) {
            long removed = Math.min(getStoredEmc(stack), toRemove / 2L);
            setStored(stack, getStoredEmc(stack) - removed);
            return removed;
        }

        @Override
        public long getStoredEmc(ItemStack stack) {
            return stack.hasTagCompound() ? stack.getTagCompound().getLong("StoredEMC") : 0L;
        }

        @Override
        public long getMaximumEmc(ItemStack stack) {
            return 1_000L;
        }

        private static void setStored(ItemStack stack, long value) {
            NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
            tag.setLong("StoredEMC", value);
            stack.setTagCompound(tag);
        }
    }
}
