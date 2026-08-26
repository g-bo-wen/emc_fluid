package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.content.blockentity.EmcConvertLiquefierBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConvertLiquefierMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class ChangeConvertLiquefierTierPacket implements IMessage {
    private BlockPos pos;
    private int delta;

    public ChangeConvertLiquefierTierPacket() {
    }

    public ChangeConvertLiquefierTierPacket(BlockPos pos, int delta) {
        this.pos = pos;
        this.delta = delta;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        pos = BlockPos.fromLong(buffer.readLong());
        delta = buffer.readInt();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(pos.toLong());
        buffer.writeInt(delta);
    }

    public void handle(EntityPlayerMP player) {
        if (!(player.openContainer instanceof EmcConvertLiquefierMenu)
                || !((EmcConvertLiquefierMenu) player.openContainer).getBlockPos().equals(pos)) {
            return;
        }
        TileEntity tile = player.world.getTileEntity(pos);
        if (tile instanceof EmcConvertLiquefierBlockEntity && delta != 0) {
            ((EmcConvertLiquefierBlockEntity) tile).changeTier(delta > 0 ? 1 : -1);
        }
    }

    public static final class Handler implements IMessageHandler<ChangeConvertLiquefierTierPacket, IMessage> {
        @Override
        public IMessage onMessage(final ChangeConvertLiquefierTierPacket packet, MessageContext context) {
            final EntityPlayerMP player = context.getServerHandler().player;
            player.getServerWorld().addScheduledTask(new Runnable() {
                @Override
                public void run() {
                    packet.handle(player);
                }
            });
            return null;
        }
    }
}
