package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import moze_intel.projecte.api.event.PlayerKnowledgeChangeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class KnowledgePatternSync {
    private static final Map<CrafterKey, UUID> REGISTERED_OWNERS = new HashMap<>();
    private static final Map<UUID, Set<CrafterKey>> CRAFTERS_BY_OWNER = new HashMap<>();
    private static final Map<UUID, Integer> KNOWLEDGE_VERSIONS = new HashMap<>();

    private KnowledgePatternSync() {
    }

    public static void update(EmcCrafterBlockEntity crafter) {
        CrafterKey key = key(crafter);
        if (key == null) {
            return;
        }
        KnowledgePatternData.getOwner(crafter.getItems().getStackInSlot(0))
                .ifPresentOrElse(owner -> register(key, owner), () -> unregister(key));
    }

    public static void unregister(EmcCrafterBlockEntity crafter) {
        CrafterKey key = key(crafter);
        if (key != null) {
            unregister(key);
        }
    }

    public static int getKnowledgeVersion(UUID owner) {
        return KNOWLEDGE_VERSIONS.getOrDefault(owner, 0);
    }

    public static void onPlayerKnowledgeChanged(PlayerKnowledgeChangeEvent event) {
        UUID owner = event.getPlayerUUID();
        KNOWLEDGE_VERSIONS.merge(owner, 1, Integer::sum);
        Set<CrafterKey> keys = CRAFTERS_BY_OWNER.get(owner);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (CrafterKey key : Set.copyOf(keys)) {
            ServerLevel level = server.getLevel(key.level());
            if (level == null) {
                unregister(key);
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(key.pos());
            if (blockEntity instanceof EmcCrafterBlockEntity crafter) {
                crafter.refreshForKnowledgeOwner(owner);
            } else {
                unregister(key);
            }
        }
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        REGISTERED_OWNERS.clear();
        CRAFTERS_BY_OWNER.clear();
        KNOWLEDGE_VERSIONS.clear();
    }

    private static void register(CrafterKey key, UUID owner) {
        UUID previous = REGISTERED_OWNERS.put(key, owner);
        if (owner.equals(previous)) {
            return;
        }
        if (previous != null) {
            removeFromOwner(previous, key);
        }
        CRAFTERS_BY_OWNER.computeIfAbsent(owner, ignored -> new HashSet<>()).add(key);
    }

    private static void unregister(CrafterKey key) {
        UUID previous = REGISTERED_OWNERS.remove(key);
        if (previous != null) {
            removeFromOwner(previous, key);
        }
    }

    private static void removeFromOwner(UUID owner, CrafterKey key) {
        Set<CrafterKey> keys = CRAFTERS_BY_OWNER.get(owner);
        if (keys == null) {
            return;
        }
        keys.remove(key);
        if (keys.isEmpty()) {
            CRAFTERS_BY_OWNER.remove(owner);
        }
    }

    private static CrafterKey key(EmcCrafterBlockEntity crafter) {
        Level level = crafter.getLevel();
        if (level == null || level.isClientSide) {
            return null;
        }
        return new CrafterKey(level.dimension(), crafter.getBlockPos().immutable());
    }

    private record CrafterKey(ResourceKey<Level> level, BlockPos pos) {
    }
}
