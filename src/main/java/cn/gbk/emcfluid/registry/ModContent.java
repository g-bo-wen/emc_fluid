package cn.gbk.emcfluid.registry;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.block.EmcConvertLiquefierBlock;
import cn.gbk.emcfluid.content.block.EmcConverterBlock;
import cn.gbk.emcfluid.content.block.EmcCrafterBlock;
import cn.gbk.emcfluid.content.block.EmcLiquefierBlock;
import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.item.KnowledgePatternItem;
import cn.gbk.emcfluid.content.recipe.KnowledgePatternUnbindRecipe;
import cn.gbk.emcfluid.integration.EmcCrafterIntegrations;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.init.Items;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = EmcFluid.MODID)
public final class ModContent {
    public static final int TIER_COUNT = 5;
    public static final Fluid[] EMC_FLUIDS = new Fluid[TIER_COUNT];
    public static final BlockFluidClassic[] EMC_FLUID_BLOCKS = new BlockFluidClassic[TIER_COUNT];

    public static Block emcLiquefier;
    public static Block emcConvertLiquefier;
    public static Block emcCrafter;
    public static Block emcConverter;
    public static Item knowledgePattern;

    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(EmcFluid.MODID) {
        @Override
        public ItemStack createIcon() {
            return knowledgePattern == null ? ItemStack.EMPTY : new ItemStack(knowledgePattern);
        }
    };

    private ModContent() {
    }

    public static void registerFluids() {
        for (int tier = 0; tier < TIER_COUNT; tier++) {
            String name = fluidName(tier);
            Fluid fluid = FluidRegistry.getFluid(name);
            if (fluid == null) {
                ResourceLocation texture = new ResourceLocation(EmcFluid.MODID, "block/" + name);
                fluid = new Fluid(name, texture, texture)
                        .setUnlocalizedName(EmcFluid.MODID + "." + name)
                        .setDensity(1400)
                        .setViscosity(1200);
                if (!FluidRegistry.registerFluid(fluid)) {
                    throw new IllegalStateException("Unable to register EMC fluid " + name);
                }
            }
            EMC_FLUIDS[tier] = fluid;
            FluidRegistry.addBucketForFluid(fluid);
        }
    }

    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(EmcLiquefierBlockEntity.class, id("emc_liquefier"));
        GameRegistry.registerTileEntity(EmcConvertLiquefierBlockEntity.class, id("emc_convert_liquefier"));
        Class<? extends EmcCrafterBlockEntity> crafterClass =
                EmcCrafterIntegrations.crafterTileEntityClass();
        if (crafterClass != EmcCrafterBlockEntity.class) {
            // The normal implementation is still instantiated by persistence probes and
            // can also be supplied by third-party code. Give it a private fallback mapping
            // while keeping the public ID bound to the AE-aware subclass so existing worlds
            // automatically gain or shed the optional integration as the mods change.
            GameRegistry.registerTileEntity(EmcCrafterBlockEntity.class, id("emc_crafter_base"));
        }
        GameRegistry.registerTileEntity(crafterClass, id("emc_crafter"));
        GameRegistry.registerTileEntity(EmcConverterBlockEntity.class, id("emc_converter"));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (int tier = 0; tier < TIER_COUNT; tier++) {
            String name = fluidName(tier);
            EMC_FLUID_BLOCKS[tier] = (BlockFluidClassic) new BlockFluidClassic(EMC_FLUIDS[tier], Material.WATER)
                    .setRegistryName(id(name))
                    .setTranslationKey(EmcFluid.MODID + "." + name);
            event.getRegistry().register(EMC_FLUID_BLOCKS[tier]);
        }

        emcLiquefier = machine(new EmcLiquefierBlock(), "emc_liquefier");
        emcConvertLiquefier = machine(new EmcConvertLiquefierBlock(), "emc_convert_liquefier");
        emcCrafter = machine(new EmcCrafterBlock(), "emc_crafter");
        emcConverter = machine(new EmcConverterBlock(), "emc_converter");
        event.getRegistry().registerAll(emcLiquefier, emcConvertLiquefier, emcCrafter, emcConverter);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(
                blockItem(emcLiquefier),
                blockItem(emcConvertLiquefier),
                blockItem(emcCrafter),
                blockItem(emcConverter)
        );
        knowledgePattern = new KnowledgePatternItem()
                .setRegistryName(id("knowledge_pattern"))
                .setTranslationKey(EmcFluid.MODID + ".knowledge_pattern")
                .setMaxStackSize(1)
                .setCreativeTab(CREATIVE_TAB);
        event.getRegistry().register(knowledgePattern);
        EmcCrafterIntegrations.registerItems(event.getRegistry());
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new KnowledgePatternUnbindRecipe().setRegistryName(id("knowledge_pattern_unbind")));
    }

    public static Fluid getEmcFluid(int tierIndex) {
        if (tierIndex < 0 || tierIndex >= TIER_COUNT) {
            throw new IndexOutOfBoundsException("EMC fluid tier " + tierIndex);
        }
        return EMC_FLUIDS[tierIndex];
    }

    public static void validateRegistration() {
        for (int tier = 0; tier < TIER_COUNT; tier++) {
            String name = fluidName(tier);
            Fluid fluid = EMC_FLUIDS[tier];
            BlockFluidClassic block = EMC_FLUID_BLOCKS[tier];
            require(FluidRegistry.getFluid(name) == fluid, "Fluid registry mismatch for " + name);
            require(Block.REGISTRY.getObject(id(name)) == block, "Block registry mismatch for " + name);
            require(fluid.getBlock() == block, "Fluid block link missing for " + name);

            ItemStack bucket = FluidUtil.getFilledBucket(new FluidStack(fluid, Fluid.BUCKET_VOLUME));
            FluidStack contained = FluidUtil.getFluidContained(bucket);
            require(!bucket.isEmpty() && contained != null && contained.getFluid() == fluid
                            && contained.amount == Fluid.BUCKET_VOLUME,
                    "Universal bucket unavailable for " + name);
        }

        Block[] machines = {emcLiquefier, emcConvertLiquefier, emcCrafter, emcConverter};
        for (Block machine : machines) {
            ResourceLocation registryName = machine.getRegistryName();
            Item blockItem = Item.getItemFromBlock(machine);
            require(registryName != null && Block.REGISTRY.getObject(registryName) == machine,
                    "Machine block registry mismatch for " + registryName);
            require(Item.REGISTRY.getObject(registryName) == blockItem,
                    "Machine item registry mismatch for " + registryName);
            require(blockItem.getCreativeTab() == CREATIVE_TAB,
                    "Machine item missing from EMC Fluid creative tab: " + registryName);
        }
        require(knowledgePattern.getRegistryName() != null
                        && Item.REGISTRY.getObject(knowledgePattern.getRegistryName()) == knowledgePattern,
                "Knowledge Pattern item registry mismatch");
        require(knowledgePattern.getCreativeTab() == CREATIVE_TAB,
                "Knowledge Pattern missing from EMC Fluid creative tab");
        EmcFluid.logger.info("Validated 5 EMC fluids, universal buckets, 4 machines, and creative-tab items");
    }

    public static void validateWorldFluidInteractions(MinecraftServer server) {
        WorldServer world = server.getWorld(0);
        BlockPos pos = world.getTopSolidOrLiquidBlock(world.getSpawnPoint());
        if (pos.getY() >= world.getHeight() - 1) {
            pos = new BlockPos(pos.getX(), world.getHeight() - 1, pos.getZ());
        }
        net.minecraft.block.state.IBlockState original = world.getBlockState(pos);
        try {
            for (int tier = 0; tier < TIER_COUNT; tier++) {
                Fluid fluid = EMC_FLUIDS[tier];
                BlockFluidClassic fluidBlock = EMC_FLUID_BLOCKS[tier];
                world.setBlockToAir(pos);

                ItemStack filledBucket = FluidUtil.getFilledBucket(new FluidStack(fluid, Fluid.BUCKET_VOLUME));
                FluidActionResult placed = FluidUtil.tryPlaceFluid(
                        null, world, pos, filledBucket, new FluidStack(fluid, Fluid.BUCKET_VOLUME));
                require(placed.isSuccess() && world.getBlockState(pos).getBlock() == fluidBlock,
                        "Unable to place " + fluid.getName() + " from its bucket");

                IFluidHandler worldHandler = FluidUtil.getFluidHandler(world, pos, EnumFacing.UP);
                require(worldHandler != null, "No pipe-compatible fluid handler for " + fluid.getName());
                FluidStack simulatedDrain = worldHandler.drain(Fluid.BUCKET_VOLUME, false);
                require(simulatedDrain != null && simulatedDrain.getFluid() == fluid
                                && simulatedDrain.amount == Fluid.BUCKET_VOLUME,
                        "Unable to simulate pipe drain for " + fluid.getName());

                world.setBlockState(pos, fluidBlock.getDefaultState(), 3);
                FluidActionResult pickedUp = FluidUtil.tryPickUpFluid(
                        new ItemStack(Items.BUCKET), null, world, pos, EnumFacing.UP);
                FluidStack recovered = pickedUp.isSuccess()
                        ? FluidUtil.getFluidContained(pickedUp.getResult()) : null;
                require(recovered != null && recovered.getFluid() == fluid
                                && recovered.amount == Fluid.BUCKET_VOLUME,
                        "Unable to recover " + fluid.getName() + " with a bucket");
            }
        } finally {
            world.setBlockState(pos, original, 3);
        }
        EmcFluid.logger.info("Validated world placement, bucket recovery, and pipe drain for 5 EMC fluids");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    private static Block machine(Block block, String name) {
        return block.setRegistryName(id(name))
                .setTranslationKey(EmcFluid.MODID + "." + name)
                .setCreativeTab(CREATIVE_TAB)
                .setHardness(3.5F)
                .setResistance(6.0F);
    }

    private static Item blockItem(Block block) {
        return new ItemBlock(block)
                .setRegistryName(block.getRegistryName())
                .setCreativeTab(CREATIVE_TAB);
    }

    private static String fluidName(int tierIndex) {
        return "emc_fluid_t" + (tierIndex + 1);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation(EmcFluid.MODID, path);
    }
}
