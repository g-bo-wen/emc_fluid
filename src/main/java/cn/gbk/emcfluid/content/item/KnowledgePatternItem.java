package cn.gbk.emcfluid.content.item;

import cn.gbk.emcfluid.util.KnowledgePatternData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KnowledgePatternItem extends Item {
    public KnowledgePatternItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        bindIfNeeded(level, player, stack);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        bindIfNeeded(context.getLevel(), player, context.getItemInHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    private void bindIfNeeded(Level level, Player player, ItemStack stack) {
        if (KnowledgePatternData.isBound(stack)) {
            if (!level.isClientSide) {
                String ownerName = KnowledgePatternData.getOwnerName(stack).orElse("Unknown");
                player.displayClientMessage(Component.translatable("message.emcfluid.knowledge_pattern.already_bound", ownerName), true);
            }
            return;
        }
        if (!level.isClientSide) {
            String ownerName = player.getGameProfile().getName();
            KnowledgePatternData.bind(stack, player.getUUID(), ownerName);
            player.displayClientMessage(Component.translatable("message.emcfluid.knowledge_pattern.bound", ownerName), true);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return KnowledgePatternData.isBound(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (KnowledgePatternData.isBound(stack)) {
            String ownerName = KnowledgePatternData.getOwnerName(stack).orElse("Unknown");
            tooltip.add(Component.translatable("tooltip.emcfluid.knowledge_pattern.bound", ownerName).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.emcfluid.knowledge_pattern.live").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.emcfluid.knowledge_pattern.unbound").withStyle(ChatFormatting.GRAY));
    }
}
