package cn.gbk.emcfluid.network;

import cn.gbk.emcfluid.content.blockentity.EmcConverterBlockEntity;
import cn.gbk.emcfluid.content.menu.EmcConverterMenu;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class ToggleConverterModePacket implements IMessage {
    private BlockPos pos;

    public ToggleConverterModePacket() {
    }

    public ToggleConverterModePacket(BlockPos pos) {
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        pos = BlockPos.fromLong(buffer.readLong());
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(pos.toLong());
    }

    public void handle(EntityPlayerMP player) {
        if (!(player.openContainer instanceof EmcConverterMenu)
                || !((EmcConverterMenu) player.openContainer).getBlockPos().equals(pos)) {
            return;
        }
        TileEntity tile = player.world.getTileEntity(pos);
        if (tile instanceof EmcConverterBlockEntity) {
            ((EmcConverterBlockEntity) tile).toggleMode();
        }
    }

    public static final class Handler implements IMessageHandler<ToggleConverterModePacket, IMessage> {
        @Override
        public IMessage onMessage(final ToggleConverterModePacket packet, MessageContext context) {
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
