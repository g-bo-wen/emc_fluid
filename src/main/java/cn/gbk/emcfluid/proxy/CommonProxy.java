package cn.gbk.emcfluid.proxy;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.gui.ModGuiHandler;
import cn.gbk.emcfluid.network.ModNetwork;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;

public class CommonProxy {
    public void preInit() {
        ModNetwork.register();
        NetworkRegistry.INSTANCE.registerGuiHandler(EmcFluid.instance, new ModGuiHandler());
    }

    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    public void init() {
    }

    public void postInit() {
    }
}
