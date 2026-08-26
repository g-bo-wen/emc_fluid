package cn.gbk.emcfluid.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Objects;

public final class EmcStackIdentity implements Comparable<EmcStackIdentity> {
    private static final String TAG_ITEM = "Item";
    private static final String TAG_META = "Meta";
    private static final String TAG_DATA = "Data";

    private final ResourceLocation itemId;
    private final int metadata;
    @Nullable
    private final NBTTagCompound itemTag;
    private final String stableKey;

    private EmcStackIdentity(ResourceLocation itemId, int metadata, @Nullable NBTTagCompound itemTag) {
        this.itemId = itemId;
        this.metadata = metadata;
        this.itemTag = itemTag == null || itemTag.isEmpty() ? null : itemTag.copy();
        this.stableKey = itemId + "@" + metadata + "#" + (this.itemTag == null ? "" : this.itemTag.toString());
    }

    public static EmcStackIdentity fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty() || stack.getItem().getRegistryName() == null) {
            throw new IllegalArgumentException("Cannot identify an empty or unregistered ItemStack");
        }
        return new EmcStackIdentity(stack.getItem().getRegistryName(), stack.getMetadata(), stack.getTagCompound());
    }

    public static EmcStackIdentity readFromNBT(NBTTagCompound compound) {
        ResourceLocation itemId = new ResourceLocation(compound.getString(TAG_ITEM));
        NBTTagCompound data = compound.hasKey(TAG_DATA, 10) ? compound.getCompoundTag(TAG_DATA) : null;
        return new EmcStackIdentity(itemId, compound.getInteger(TAG_META), data);
    }

    public ItemStack createStack() {
        Item item = Item.REGISTRY.getObject(itemId);
        if (item == null || item == net.minecraft.init.Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item, 1, metadata);
        if (itemTag != null) {
            stack.setTagCompound(itemTag.copy());
        }
        return stack;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound.setString(TAG_ITEM, itemId.toString());
        compound.setInteger(TAG_META, metadata);
        if (itemTag != null) {
            compound.setTag(TAG_DATA, itemTag.copy());
        }
        return compound;
    }

    public String stableKey() {
        return stableKey;
    }

    @Override
    public int compareTo(EmcStackIdentity other) {
        return stableKey.compareTo(other.stableKey);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof EmcStackIdentity)) {
            return false;
        }
        EmcStackIdentity other = (EmcStackIdentity) object;
        return metadata == other.metadata && itemId.equals(other.itemId) && Objects.equals(itemTag, other.itemTag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId, metadata, itemTag);
    }

    @Override
    public String toString() {
        return stableKey;
    }
}
