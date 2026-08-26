package cn.gbk.emcfluid.util;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import moze_intel.projecte.api.event.PlayerKnowledgeChangeEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = EmcFluid.MODID)
public final class KnowledgePatternSync {
    private static final Map<CrafterKey, UUID> REGISTERED_OWNERS = new HashMap<CrafterKey, UUID>();
    private static final Map<UUID, Set<CrafterKey>> CRAFTERS_BY_OWNER = new HashMap<UUID, Set<CrafterKey>>();
    private static final Map<UUID, Integer> KNOWLEDGE_VERSIONS = new HashMap<UUID, Integer>();

    private KnowledgePatternSync() {
    }

    public static void update(EmcCrafterBlockEntity crafter) {
        CrafterKey key = key(crafter);
        if (key == null) {
            return;
        }
        java.util.Optional<UUID> owner = KnowledgePatternData.getOwner(crafter.getPattern().getStackInSlot(0));
        if (owner.isPresent()) {
            register(key, owner.get());
        } else {
            unregister(key);
        }
    }

    public static void unregister(EmcCrafterBlockEntity crafter) {
        CrafterKey key = key(crafter);
        if (key != null) {
            unregister(key);
        }
    }

    public static int getKnowledgeVersion(UUID owner) {
        Integer version = KNOWLEDGE_VERSIONS.get(owner);
        return version == null ? 0 : version;
    }

    @SubscribeEvent
    public static void onPlayerKnowledgeChanged(PlayerKnowledgeChangeEvent event) {
        UUID owner = event.getPlayerUUID();
        Integer oldVersion = KNOWLEDGE_VERSIONS.get(owner);
        KNOWLEDGE_VERSIONS.put(owner, oldVersion == null ? 1 : oldVersion + 1);
        Set<CrafterKey> keys = CRAFTERS_BY_OWNER.get(owner);
        if (keys == null || keys.isEmpty()) {
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null) {
            return;
        }
        for (CrafterKey key : new HashSet<CrafterKey>(keys)) {
            World world = server.getWorld(key.dimension);
            if (world == null) {
                unregister(key);
                continue;
            }
            TileEntity tile = world.getTileEntity(key.pos);
            if (tile instanceof EmcCrafterBlockEntity) {
                ((EmcCrafterBlockEntity) tile).refreshForKnowledgeOwner(owner);
            } else {
                unregister(key);
            }
        }
    }

    public static void clear() {
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
        Set<CrafterKey> keys = CRAFTERS_BY_OWNER.get(owner);
        if (keys == null) {
            keys = new HashSet<CrafterKey>();
            CRAFTERS_BY_OWNER.put(owner, keys);
        }
        keys.add(key);
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
        World world = crafter.getWorld();
        if (world == null || world.isRemote) {
            return null;
        }
        return new CrafterKey(world.provider.getDimension(), crafter.getPos().toImmutable());
    }

    private static final class CrafterKey {
        private final int dimension;
        private final BlockPos pos;

        private CrafterKey(int dimension, BlockPos pos) {
            this.dimension = dimension;
            this.pos = pos;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof CrafterKey)) {
                return false;
            }
            CrafterKey other = (CrafterKey) object;
            return dimension == other.dimension && pos.equals(other.pos);
        }

        @Override
        public int hashCode() {
            return 31 * dimension + pos.hashCode();
        }
    }
}
