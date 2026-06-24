package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.content.blockentity.EmcLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcLiquefierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ToggleLiquefierModePacket(BlockPos pos) {
    public static void encode(ToggleLiquefierModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static ToggleLiquefierModePacket decode(FriendlyByteBuf buffer) {
        return new ToggleLiquefierModePacket(buffer.readBlockPos());
    }

    public static void handle(ToggleLiquefierModePacket packet, NetworkEvent.Context context) {
        var player = context.getSender();
        if (player == null || !(player.containerMenu instanceof EmcLiquefierMenu menu) || !menu.getBlockPos().equals(packet.pos)) {
            return;
        }
        if (player.level().getBlockEntity(packet.pos) instanceof EmcLiquefierBlockEntity liquefier) {
            liquefier.toggleMode();
        }
    }
}
