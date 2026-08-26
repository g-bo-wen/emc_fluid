package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class KnowledgePatternData {
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_OWNER_NAME = "OwnerName";

    private KnowledgePatternData() {
    }

    public static boolean isPattern(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.getItem() == ModContent.knowledgePattern;
    }

    public static List<ItemStack> readForCrafting(ItemStack stack) {
        Optional<UUID> owner = getOwner(stack);
        return owner.isPresent() ? ProjectEAccess.getKnowledge(owner.get()) : Collections.<ItemStack>emptyList();
    }

    public static boolean bind(ItemStack stack, UUID owner, String ownerName) {
        if (!isPattern(stack) || owner == null || isBound(stack)) {
            return false;
        }
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setUniqueId(TAG_OWNER, owner);
        tag.setString(TAG_OWNER_NAME, ownerName == null ? "" : ownerName);
        stack.setTagCompound(tag);
        return true;
    }

    public static boolean isBound(ItemStack stack) {
        return getOwner(stack).isPresent();
    }

    public static boolean isBoundTo(ItemStack stack, UUID owner) {
        Optional<UUID> boundOwner = getOwner(stack);
        return boundOwner.isPresent() && boundOwner.get().equals(owner);
    }

    public static Optional<UUID> getOwner(ItemStack stack) {
        if (!isPattern(stack) || !stack.hasTagCompound() || !stack.getTagCompound().hasUniqueId(TAG_OWNER)) {
            return Optional.empty();
        }
        return Optional.of(stack.getTagCompound().getUniqueId(TAG_OWNER));
    }

    public static Optional<String> getOwnerName(ItemStack stack) {
        if (!isPattern(stack) || !stack.hasTagCompound() || !stack.getTagCompound().hasKey(TAG_OWNER_NAME, 8)) {
            return Optional.empty();
        }
        String name = stack.getTagCompound().getString(TAG_OWNER_NAME).trim();
        return name.isEmpty() ? Optional.<String>empty() : Optional.of(name);
    }
}
