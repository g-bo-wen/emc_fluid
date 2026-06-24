package cn.gbk.emcfluid.registry;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.block.EmcCrafterBlock;
import cn.gbk.emcfluid.content.block.EmcConverterBlock;
import cn.gbk.emcfluid.content.block.EmcLiquefierBlock;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.item.KnowledgePatternItem;
import cn.gbk.emcfluid.content.menu.EmcCrafterMenu;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import cn.gbk.emcfluid.content.recipe.KnowledgePatternUnbindRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;
import java.util.function.Consumer;

public final class ModContent {
    private static final ResourceLocation WATER_OVERLAY = new ResourceLocation("minecraft", "block/water_overlay");
    private static final int NO_TINT = 0xFFFFFFFF;
    private static final ResourceLocation[] EMC_FLUID_TEXTURES = {
            emcTexture("block/emc_fluid_t1"),
            emcTexture("block/emc_fluid_t2"),
            emcTexture("block/emc_fluid_t3"),
            emcTexture("block/emc_fluid_t4"),
            emcTexture("block/emc_fluid_t5")
    };

    private ModContent() {
    }

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, EmcFluid.MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, EmcFluid.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, EmcFluid.MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, EmcFluid.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, EmcFluid.MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EmcFluid.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, EmcFluid.MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, EmcFluid.MODID);

    public static final RegistryObject<FluidType> EMC_FLUID_T1_TYPE = FLUID_TYPES.register("emc_fluid_t1",
            () -> emcFluidType("emc_fluid_t1", 0));
    public static final RegistryObject<FluidType> EMC_FLUID_T2_TYPE = FLUID_TYPES.register("emc_fluid_t2",
            () -> emcFluidType("emc_fluid_t2", 1));
    public static final RegistryObject<FluidType> EMC_FLUID_T3_TYPE = FLUID_TYPES.register("emc_fluid_t3",
            () -> emcFluidType("emc_fluid_t3", 2));
    public static final RegistryObject<FluidType> EMC_FLUID_T4_TYPE = FLUID_TYPES.register("emc_fluid_t4",
            () -> emcFluidType("emc_fluid_t4", 3));
    public static final RegistryObject<FluidType> EMC_FLUID_T5_TYPE = FLUID_TYPES.register("emc_fluid_t5",
            () -> emcFluidType("emc_fluid_t5", 4));

    public static final RegistryObject<FlowingFluid> EMC_FLUID_T1_SOURCE = FLUIDS.register("emc_fluid_t1",
            () -> new ForgeFlowingFluid.Source(emcFluidProperties(0)));
    public static final RegistryObject<FlowingFluid> EMC_FLUID_T1_FLOWING = FLUIDS.register("flowing_emc_fluid_t1",
            () -> new ForgeFlowingFluid.Flowing(emcFluidProperties(0)));
    public static final RegistryObject<LiquidBlock> EMC_FLUID_T1_BLOCK = BLOCKS.register("emc_fluid_t1",
            () -> new LiquidBlock(() -> EMC_FLUID_T1_SOURCE.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .replaceable()
                    .noCollission()
                    .strength(100.0F)
                    .pushReaction(PushReaction.DESTROY)
                    .noLootTable()
                    .liquid()));
    public static final RegistryObject<Item> EMC_FLUID_T1_BUCKET = ITEMS.register("emc_fluid_t1_bucket",
            () -> new BucketItem(() -> EMC_FLUID_T1_SOURCE.get(), new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .stacksTo(1)));
    public static final RegistryObject<FlowingFluid> EMC_FLUID_T2_SOURCE = FLUIDS.register("emc_fluid_t2",
            () -> new ForgeFlowingFluid.Source(emcFluidProperties(1)));
    public static final RegistryObject<FlowingFluid> EMC_FLUID_T2_FLOWING = FLUIDS.register("flowing_emc_fluid_t2",
            () -> new ForgeFlowingFluid.Flowing(emcFluidProperties(1)));
    public static final RegistryObject<LiquidBlock> EMC_FLUID_T2_BLOCK = BLOCKS.register("emc_fluid_t2",
            () -> new LiquidBlock(() -> EMC_FLUID_T2_SOURCE.get(), fluidBlockProperties(MapColor.COLOR_BLUE)));
    public static final RegistryObject<Item> EMC_FLUID_T2_BUCKET = ITEMS.register("emc_fluid_t2_bucket",
            () -> new BucketItem(() -> EMC_FLUID_T2_SOURCE.get(), bucketProperties()));

    public static final RegistryObject<FlowingFluid> EMC_FLUID_T3_SOURCE = FLUIDS.register("emc_fluid_t3",
            () -> new ForgeFlowingFluid.Source(emcFluidProperties(2)));
    public static final RegistryObject<FlowingFluid> EMC_FLUID_T3_FLOWING = FLUIDS.register("flowing_emc_fluid_t3",
            () -> new ForgeFlowingFluid.Flowing(emcFluidProperties(2)));
    public static final RegistryObject<LiquidBlock> EMC_FLUID_T3_BLOCK = BLOCKS.register("emc_fluid_t3",
            () -> new LiquidBlock(() -> EMC_FLUID_T3_SOURCE.get(), fluidBlockProperties(MapColor.COLOR_LIGHT_BLUE)));
    public static final RegistryObject<Item> EMC_FLUID_T3_BUCKET = ITEMS.register("emc_fluid_t3_bucket",
            () -> new BucketItem(() -> EMC_FLUID_T3_SOURCE.get(), bucketProperties()));

    public static final RegistryObject<FlowingFluid> EMC_FLUID_T4_SOURCE = FLUIDS.register("emc_fluid_t4",
            () -> new ForgeFlowingFluid.Source(emcFluidProperties(3)));
    public static final RegistryObject<FlowingFluid> EMC_FLUID_T4_FLOWING = FLUIDS.register("flowing_emc_fluid_t4",
            () -> new ForgeFlowingFluid.Flowing(emcFluidProperties(3)));
    public static final RegistryObject<LiquidBlock> EMC_FLUID_T4_BLOCK = BLOCKS.register("emc_fluid_t4",
            () -> new LiquidBlock(() -> EMC_FLUID_T4_SOURCE.get(), fluidBlockProperties(MapColor.COLOR_MAGENTA)));
    public static final RegistryObject<Item> EMC_FLUID_T4_BUCKET = ITEMS.register("emc_fluid_t4_bucket",
            () -> new BucketItem(() -> EMC_FLUID_T4_SOURCE.get(), bucketProperties()));

    public static final RegistryObject<FlowingFluid> EMC_FLUID_T5_SOURCE = FLUIDS.register("emc_fluid_t5",
            () -> new ForgeFlowingFluid.Source(emcFluidProperties(4)));
    public static final RegistryObject<FlowingFluid> EMC_FLUID_T5_FLOWING = FLUIDS.register("flowing_emc_fluid_t5",
            () -> new ForgeFlowingFluid.Flowing(emcFluidProperties(4)));
    public static final RegistryObject<LiquidBlock> EMC_FLUID_T5_BLOCK = BLOCKS.register("emc_fluid_t5",
            () -> new LiquidBlock(() -> EMC_FLUID_T5_SOURCE.get(), fluidBlockProperties(MapColor.COLOR_RED)));
    public static final RegistryObject<Item> EMC_FLUID_T5_BUCKET = ITEMS.register("emc_fluid_t5_bucket",
            () -> new BucketItem(() -> EMC_FLUID_T5_SOURCE.get(), bucketProperties()));

    private static final List<RegistryObject<FluidType>> EMC_FLUID_TYPES = List.of(
            EMC_FLUID_T1_TYPE, EMC_FLUID_T2_TYPE, EMC_FLUID_T3_TYPE, EMC_FLUID_T4_TYPE, EMC_FLUID_T5_TYPE);
    private static final List<RegistryObject<FlowingFluid>> EMC_FLUID_SOURCES = List.of(
            EMC_FLUID_T1_SOURCE, EMC_FLUID_T2_SOURCE, EMC_FLUID_T3_SOURCE, EMC_FLUID_T4_SOURCE, EMC_FLUID_T5_SOURCE);
    private static final List<RegistryObject<FlowingFluid>> EMC_FLUID_FLOWING = List.of(
            EMC_FLUID_T1_FLOWING, EMC_FLUID_T2_FLOWING, EMC_FLUID_T3_FLOWING, EMC_FLUID_T4_FLOWING, EMC_FLUID_T5_FLOWING);
    private static final List<RegistryObject<LiquidBlock>> EMC_FLUID_BLOCKS = List.of(
            EMC_FLUID_T1_BLOCK, EMC_FLUID_T2_BLOCK, EMC_FLUID_T3_BLOCK, EMC_FLUID_T4_BLOCK, EMC_FLUID_T5_BLOCK);
    private static final List<RegistryObject<Item>> EMC_FLUID_BUCKETS = List.of(
            EMC_FLUID_T1_BUCKET, EMC_FLUID_T2_BUCKET, EMC_FLUID_T3_BUCKET, EMC_FLUID_T4_BUCKET, EMC_FLUID_T5_BUCKET);

    public static final RegistryObject<Block> EMC_LIQUEFIER = BLOCKS.register("emc_liquefier",
            () -> new EmcLiquefierBlock(machineProperties()));
    public static final RegistryObject<Block> EMC_CRAFTER = BLOCKS.register("emc_crafter",
            () -> new EmcCrafterBlock(machineProperties()));
    public static final RegistryObject<Block> EMC_CONVERTER = BLOCKS.register("emc_converter",
            () -> new EmcConverterBlock(machineProperties()));

    public static final RegistryObject<Item> EMC_LIQUEFIER_ITEM = ITEMS.register("emc_liquefier",
            () -> new BlockItem(EMC_LIQUEFIER.get(), new Item.Properties()));
    public static final RegistryObject<Item> EMC_CRAFTER_ITEM = ITEMS.register("emc_crafter",
            () -> new BlockItem(EMC_CRAFTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> EMC_CONVERTER_ITEM = ITEMS.register("emc_converter",
            () -> new BlockItem(EMC_CONVERTER.get(), new Item.Properties()));
    public static final RegistryObject<Item> KNOWLEDGE_PATTERN = ITEMS.register("knowledge_pattern",
            () -> new KnowledgePatternItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<BlockEntityType<EmcLiquefierBlockEntity>> EMC_LIQUEFIER_BE = BLOCK_ENTITY_TYPES.register("emc_liquefier",
            () -> BlockEntityType.Builder.of(EmcLiquefierBlockEntity::new, EMC_LIQUEFIER.get()).build(null));
    public static final RegistryObject<BlockEntityType<EmcCrafterBlockEntity>> EMC_CRAFTER_BE = BLOCK_ENTITY_TYPES.register("emc_crafter",
            () -> BlockEntityType.Builder.of(EmcCrafterBlockEntity::new, EMC_CRAFTER.get()).build(null));
    public static final RegistryObject<BlockEntityType<EmcConverterBlockEntity>> EMC_CONVERTER_BE = BLOCK_ENTITY_TYPES.register("emc_converter",
            () -> BlockEntityType.Builder.of(EmcConverterBlockEntity::new, EMC_CONVERTER.get()).build(null));

    public static final RegistryObject<MenuType<EmcLiquefierMenu>> EMC_LIQUEFIER_MENU = MENU_TYPES.register("emc_liquefier",
            () -> IForgeMenuType.create(EmcLiquefierMenu::fromNetwork));
    public static final RegistryObject<MenuType<EmcCrafterMenu>> EMC_CRAFTER_MENU = MENU_TYPES.register("emc_crafter",
            () -> IForgeMenuType.create(EmcCrafterMenu::fromNetwork));
    public static final RegistryObject<MenuType<EmcConverterMenu>> EMC_CONVERTER_MENU = MENU_TYPES.register("emc_converter",
            () -> IForgeMenuType.create(EmcConverterMenu::fromNetwork));

    public static final RegistryObject<RecipeSerializer<KnowledgePatternUnbindRecipe>> KNOWLEDGE_PATTERN_UNBIND_RECIPE =
            RECIPE_SERIALIZERS.register("knowledge_pattern_unbind", () -> new SimpleCraftingRecipeSerializer<>(KnowledgePatternUnbindRecipe::new));

    public static final RegistryObject<CreativeModeTab> MAIN_TAB = CREATIVE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.emcfluid.main"))
                    .icon(() -> KNOWLEDGE_PATTERN.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        output.accept(EMC_LIQUEFIER_ITEM.get());
                        output.accept(EMC_CRAFTER_ITEM.get());
                        output.accept(EMC_CONVERTER_ITEM.get());
                        output.accept(KNOWLEDGE_PATTERN.get());
                        EMC_FLUID_BUCKETS.forEach(bucket -> output.accept(bucket.get()));
                    })
                    .build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        BLOCK_ENTITY_TYPES.register(bus);
        MENU_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        CREATIVE_TABS.register(bus);
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .strength(3.5F, 6.0F)
                .requiresCorrectToolForDrops();
    }

    public static RegistryObject<FlowingFluid> getEmcFluidSource(int tierIndex) {
        return EMC_FLUID_SOURCES.get(tierIndex);
    }

    public static RegistryObject<FlowingFluid> getEmcFluidFlowing(int tierIndex) {
        return EMC_FLUID_FLOWING.get(tierIndex);
    }

    public static RegistryObject<Item> getEmcFluidBucket(int tierIndex) {
        return EMC_FLUID_BUCKETS.get(tierIndex);
    }

    private static ResourceLocation emcTexture(String path) {
        return new ResourceLocation(EmcFluid.MODID, path);
    }

    private static FluidType emcFluidType(String name, int tierIndex) {
        return new FluidType(FluidType.Properties.create()
                .descriptionId("fluid.emcfluid." + name)
                .canSwim(false)
                .canDrown(false)
                .density(1400)
                .viscosity(1200)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                ResourceLocation texture = EMC_FLUID_TEXTURES[tierIndex];
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return texture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return texture;
                    }

                    @Override
                    public ResourceLocation getOverlayTexture() {
                        return WATER_OVERLAY;
                    }

                    @Override
                    public ResourceLocation getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                        return texture;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                        return texture;
                    }

                    @Override
                    public ResourceLocation getOverlayTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                        return WATER_OVERLAY;
                    }

                    @Override
                    public int getTintColor() {
                        return NO_TINT;
                    }

                    @Override
                    public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
                        return NO_TINT;
                    }

                    @Override
                    public int getTintColor(FluidStack stack) {
                        return NO_TINT;
                    }
                });
            }
        };
    }

    private static Item.Properties bucketProperties() {
        return new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1);
    }

    private static BlockBehaviour.Properties fluidBlockProperties(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable()
                .liquid();
    }

    private static ForgeFlowingFluid.Properties emcFluidProperties(int tierIndex) {
        return new ForgeFlowingFluid.Properties(
                EMC_FLUID_TYPES.get(tierIndex),
                () -> EMC_FLUID_SOURCES.get(tierIndex).get(),
                () -> EMC_FLUID_FLOWING.get(tierIndex).get())
                .bucket(() -> EMC_FLUID_BUCKETS.get(tierIndex).get())
                .block(() -> EMC_FLUID_BLOCKS.get(tierIndex).get())
                .slopeFindDistance(2)
                .levelDecreasePerBlock(2);
    }
}
