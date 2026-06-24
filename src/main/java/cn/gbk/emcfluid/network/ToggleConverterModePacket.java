package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ToggleConverterModePacket(BlockPos pos) {
    public static void encode(ToggleConverterModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static ToggleConverterModePacket decode(FriendlyByteBuf buffer) {
        return new ToggleConverterModePacket(buffer.readBlockPos());
    }

    public static void handle(ToggleConverterModePacket packet, NetworkEvent.Context context) {
        var player = context.getSender();
        if (player == null || !(player.containerMenu instanceof EmcConverterMenu menu) || !menu.getBlockPos().equals(packet.pos)) {
            return;
        }
        if (player.level().getBlockEntity(packet.pos) instanceof EmcConverterBlockEntity converter) {
            converter.toggleMode();
        }
    }
}
