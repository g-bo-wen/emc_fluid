package cn.gbk.emcfluid.content.item;

import cn.gbk.emcfluid.util.KnowledgePatternData;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

public class KnowledgePatternItem extends Item {
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        bindIfNeeded(world, player, stack);
        return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand,
                                      EnumFacing facing, float hitX, float hitY, float hitZ) {
        bindIfNeeded(world, player, player.getHeldItem(hand));
        return EnumActionResult.SUCCESS;
    }

    private void bindIfNeeded(World world, EntityPlayer player, ItemStack stack) {
        if (world.isRemote) {
            return;
        }
        if (KnowledgePatternData.isBound(stack)) {
            String ownerName = KnowledgePatternData.getOwnerName(stack).orElse("Unknown");
            player.sendStatusMessage(new TextComponentTranslation(
                    "message.emcfluid.knowledge_pattern.already_bound", ownerName), true);
            return;
        }
        String ownerName = player.getGameProfile().getName();
        if (KnowledgePatternData.bind(stack, player.getUniqueID(), ownerName)) {
            player.inventory.markDirty();
            player.sendStatusMessage(new TextComponentTranslation(
                    "message.emcfluid.knowledge_pattern.bound", ownerName), true);
        }
    }

    @Override
    public boolean hasEffect(ItemStack stack) {
        return KnowledgePatternData.isBound(stack) || super.hasEffect(stack);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        if (KnowledgePatternData.isBound(stack)) {
            String ownerName = KnowledgePatternData.getOwnerName(stack).orElse("Unknown");
            tooltip.add(TextFormatting.GOLD + new TextComponentTranslation(
                    "tooltip.emcfluid.knowledge_pattern.bound", ownerName).getFormattedText());
            tooltip.add(TextFormatting.GRAY + new TextComponentTranslation(
                    "tooltip.emcfluid.knowledge_pattern.live").getFormattedText());
        } else {
            tooltip.add(TextFormatting.GRAY + new TextComponentTranslation(
                    "tooltip.emcfluid.knowledge_pattern.unbound").getFormattedText());
        }
    }
}
