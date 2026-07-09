package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public record ChangeConvertLiquefierTierPacket(BlockPos pos, int delta) {
    public static void encode(ChangeConvertLiquefierTierPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeVarInt(packet.delta);
    }

    public static ChangeConvertLiquefierTierPacket decode(FriendlyByteBuf buffer) {
        return new ChangeConvertLiquefierTierPacket(buffer.readBlockPos(), buffer.readVarInt());
    }

    public static void handle(ChangeConvertLiquefierTierPacket packet, NetworkEvent.Context context) {
        var player = context.getSender();
        if (player == null || !(player.containerMenu instanceof EmcConvertLiquefierMenu menu) || !menu.getBlockPos().equals(packet.pos)) {
            return;
        }
        if (player.level().getBlockEntity(packet.pos) instanceof EmcConvertLiquefierBlockEntity liquefier) {
            liquefier.changeTier(packet.delta > 0 ? 1 : -1);
        }
    }
}
