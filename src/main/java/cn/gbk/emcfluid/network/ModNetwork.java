package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.EmcFluid;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(EmcFluid.MODID);
    private static boolean registered;

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        int id = 0;
        CHANNEL.registerMessage(ToggleLiquefierModePacket.Handler.class, ToggleLiquefierModePacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(ToggleConverterModePacket.Handler.class, ToggleConverterModePacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(ToggleConvertLiquefierModePacket.Handler.class, ToggleConvertLiquefierModePacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(ChangeConvertLiquefierTierPacket.Handler.class, ChangeConvertLiquefierTierPacket.class, id, Side.SERVER);
    }
}
