package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.EmcFluid;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EmcFluid.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private static int nextId;
    private static boolean registered;

    private ModNetwork() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        CHANNEL.messageBuilder(ToggleLiquefierModePacket.class, nextId++)
                .encoder(ToggleLiquefierModePacket::encode)
                .decoder(ToggleLiquefierModePacket::decode)
                .consumerMainThread((packet, context) -> ToggleLiquefierModePacket.handle(packet, context.get()))
                .add();
        CHANNEL.messageBuilder(ToggleConverterModePacket.class, nextId++)
                .encoder(ToggleConverterModePacket::encode)
                .decoder(ToggleConverterModePacket::decode)
                .consumerMainThread((packet, context) -> ToggleConverterModePacket.handle(packet, context.get()))
                .add();
    }
}
