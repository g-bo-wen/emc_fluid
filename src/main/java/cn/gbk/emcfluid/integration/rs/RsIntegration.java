package cn.gbk.emcfluid.integration.rs;

import cn.gbk.emcfluid.EmcFluid;
import com.refinedmods.refinedstorage.api.IRSAPI;
import com.refinedmods.refinedstorage.api.RSAPIInject;

public final class RsIntegration {
    @RSAPIInject
    public static IRSAPI API;

    private RsIntegration() {
    }

    public static void register() {
        if (API == null) {
            EmcFluid.LOGGER.warn("Refined Storage API was not injected; EMC Crafter RS integration is disabled");
            return;
        }
        API.getNetworkNodeRegistry().add(EmcCrafterNetworkNode.ID, (tag, level, pos) -> {
            EmcCrafterNetworkNode node = new EmcCrafterNetworkNode(level, pos);
            node.read(tag);
            return node;
        });
        API.getCraftingTaskRegistry().add(EmcRsCraftingTaskFactory.ID, new EmcRsCraftingTaskFactory());
    }
}
