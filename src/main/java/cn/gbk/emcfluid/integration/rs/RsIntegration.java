package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.EmcFluid;
import com.raoulvdberge.refinedstorage.api.IRSAPI;
import com.raoulvdberge.refinedstorage.api.RSAPIInject;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

public final class RsIntegration {
    public static final String NODE_ID = EmcFluid.MODID + ":emc_crafter";
    public static final String TASK_ID = EmcFluid.MODID + ":emc_crafting";

    @RSAPIInject
    public static IRSAPI API;

    static Item virtualPatternItem;
    private static boolean registered;

    private RsIntegration() {
    }

    public static void registerItems(IForgeRegistry<Item> registry) {
        if (virtualPatternItem != null) {
            return;
        }
        virtualPatternItem = new EmcRsVirtualPatternItem()
                .setRegistryName(new ResourceLocation(EmcFluid.MODID, "rs_virtual_pattern"))
                .setTranslationKey(EmcFluid.MODID + ".rs_virtual_pattern")
                .setMaxStackSize(1);
        registry.register(virtualPatternItem);
    }

    public static void register() {
        if (registered) {
            return;
        }
        if (API == null) {
            EmcFluid.logger.warn("Refined Storage API was not injected; EMC Crafter RS integration is disabled");
            return;
        }
        if (virtualPatternItem == null) {
            EmcFluid.logger.warn("EMC Crafter RS virtual pattern item was not registered; integration is disabled");
            return;
        }

        API.getNetworkNodeRegistry().add(NODE_ID, (tag, world, pos) -> {
            EmcCrafterNetworkNode node = new EmcCrafterNetworkNode(world, pos);
            node.read(tag);
            return node;
        });
        API.getCraftingTaskRegistry().add(TASK_ID, new EmcRsCraftingTaskFactory());
        registered = true;
        EmcFluid.logger.info("Registered Refined Storage 1.6.16 EMC Crafter node and crafting task");
    }

    static boolean isOurNode(INetworkNode node) {
        return node instanceof EmcCrafterNetworkNode && NODE_ID.equals(node.getId());
    }
}
