package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.registry.ModContent;
import moze_intel.projecte.api.ItemInfo;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class KnowledgePatternData {
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_OWNER_NAME = "OwnerName";

    private KnowledgePatternData() {
    }

    public static boolean isPattern(ItemStack stack) {
        return stack.is(ModContent.KNOWLEDGE_PATTERN.get());
    }

    public static List<ItemInfo> readForCrafting(ItemStack stack) {
        Optional<UUID> owner = getOwner(stack);
        return owner.map(ProjectEAccess::getKnowledge).orElseGet(List::of);
    }

    public static boolean bind(ItemStack stack, UUID owner, String ownerName) {
        if (!isPattern(stack) || isBound(stack)) {
            return false;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putUUID(TAG_OWNER, owner);
        tag.putString(TAG_OWNER_NAME, ownerName);
        return true;
    }

    public static boolean isBound(ItemStack stack) {
        return getOwner(stack).isPresent();
    }

    public static boolean isBoundTo(ItemStack stack, UUID owner) {
        return getOwner(stack).filter(owner::equals).isPresent();
    }

    public static Optional<UUID> getOwner(ItemStack stack) {
        if (!isPattern(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_OWNER)) {
            return Optional.empty();
        }
        return Optional.of(tag.getUUID(TAG_OWNER));
    }

    public static Optional<String> getOwnerName(ItemStack stack) {
        if (!isPattern(stack)) {
            return Optional.empty();
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_OWNER_NAME, Tag.TAG_STRING)) {
            return Optional.empty();
        }
        String name = tag.getString(TAG_OWNER_NAME);
        return name.isBlank() ? Optional.empty() : Optional.of(name);
    }
}
