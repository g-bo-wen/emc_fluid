package cn.gbk.emcfluid.proxy;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.client.screen.EmcConvertLiquefierScreen;
import cn.gbk.emcfluid.client.screen.EmcConverterScreen;
import cn.gbk.emcfluid.client.screen.EmcCrafterScreen;
import cn.gbk.emcfluid.client.screen.EmcLiquefierScreen;
import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import cn.gbk.emcfluid.content.menu.EmcCrafterMenu;
import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.item.Item;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = EmcFluid.MODID, value = Side.CLIENT)
public final class ClientProxy extends CommonProxy {
    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        switch (id) {
            case 0:
                if (tile instanceof EmcLiquefierBlockEntity && !(tile instanceof EmcConvertLiquefierBlockEntity)) {
                    EmcLiquefierMenu menu = new EmcLiquefierMenu(player.inventory, (EmcLiquefierBlockEntity) tile);
                    return new EmcLiquefierScreen(menu, player.inventory);
                }
                break;
            case 1:
                if (tile instanceof EmcConvertLiquefierBlockEntity) {
                    EmcConvertLiquefierMenu menu = new EmcConvertLiquefierMenu(player.inventory, (EmcConvertLiquefierBlockEntity) tile);
                    return new EmcConvertLiquefierScreen(menu, player.inventory);
                }
                break;
            case 2:
                if (tile instanceof EmcCrafterBlockEntity) {
                    EmcCrafterMenu menu = new EmcCrafterMenu(player.inventory, (EmcCrafterBlockEntity) tile);
                    return new EmcCrafterScreen(menu, player.inventory);
                }
                break;
            case 3:
                if (tile instanceof EmcConverterBlockEntity) {
                    EmcConverterMenu menu = new EmcConverterMenu(player.inventory, (EmcConverterBlockEntity) tile);
                    return new EmcConverterScreen(menu, player.inventory);
                }
                break;
            default:
                break;
        }
        return null;
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        registerBlockItemModel(ModContent.emcLiquefier);
        registerBlockItemModel(ModContent.emcConvertLiquefier);
        registerBlockItemModel(ModContent.emcCrafter);
        registerBlockItemModel(ModContent.emcConverter);
        ModelLoader.setCustomModelResourceLocation(
                ModContent.knowledgePattern,
                0,
                new ModelResourceLocation(ModContent.knowledgePattern.getRegistryName(), "inventory")
        );

        for (BlockFluidBase fluidBlock : ModContent.EMC_FLUID_BLOCKS) {
            ModelLoader.setCustomStateMapper(
                    fluidBlock,
                    new StateMap.Builder().ignore(BlockFluidBase.LEVEL).build()
            );
        }
    }

    private static void registerBlockItemModel(Block block) {
        Item item = Item.getItemFromBlock(block);
        ModelLoader.setCustomModelResourceLocation(
                item,
                0,
                new ModelResourceLocation(block.getRegistryName(), "inventory")
        );
    }
}
