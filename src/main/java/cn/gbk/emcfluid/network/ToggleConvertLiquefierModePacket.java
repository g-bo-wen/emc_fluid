package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ToggleConvertLiquefierModePacket(BlockPos pos) {
    public static void encode(ToggleConvertLiquefierModePacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static ToggleConvertLiquefierModePacket decode(FriendlyByteBuf buffer) {
        return new ToggleConvertLiquefierModePacket(buffer.readBlockPos());
    }

    public static void handle(ToggleConvertLiquefierModePacket packet, NetworkEvent.Context context) {
        var player = context.getSender();
        if (player == null || !(player.containerMenu instanceof EmcConvertLiquefierMenu menu) || !menu.getBlockPos().equals(packet.pos)) {
            return;
        }
        if (player.level().getBlockEntity(packet.pos) instanceof EmcConvertLiquefierBlockEntity liquefier) {
            liquefier.toggleMode();
        }
    }
}
