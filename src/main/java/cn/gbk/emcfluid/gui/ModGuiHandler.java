package cn.gbk.emcfluid.gui;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import cn.gbk.emcfluid.content.menu.EmcCrafterMenu;
import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public final class ModGuiHandler implements IGuiHandler {
    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(new BlockPos(x, y, z));
        switch (id) {
            case 0:
                if (tile instanceof EmcLiquefierBlockEntity && !(tile instanceof EmcConvertLiquefierBlockEntity)) {
                    return new EmcLiquefierMenu(player.inventory, (EmcLiquefierBlockEntity) tile);
                }
                break;
            case 1:
                if (tile instanceof EmcConvertLiquefierBlockEntity) {
                    return new EmcConvertLiquefierMenu(player.inventory, (EmcConvertLiquefierBlockEntity) tile);
                }
                break;
            case 2:
                if (tile instanceof EmcCrafterBlockEntity) {
                    return new EmcCrafterMenu(player.inventory, (EmcCrafterBlockEntity) tile);
                }
                break;
            case 3:
                if (tile instanceof EmcConverterBlockEntity) {
                    return new EmcConverterMenu(player.inventory, (EmcConverterBlockEntity) tile);
                }
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return EmcFluid.proxy.getClientGuiElement(id, player, world, x, y, z);
    }
}
