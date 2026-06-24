package cn.gbk.emcfluid.client;

import cn.gbk.emcfluid.client.screen.EmcCrafterScreen;
import cn.gbk.emcfluid.client.screen.EmcConverterScreen;
import cn.gbk.emcfluid.client.screen.EmcLiquefierScreen;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public final class ClientSetup {
    private ClientSetup() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ClientSetup::clientSetup);
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModContent.EMC_LIQUEFIER_MENU.get(), EmcLiquefierScreen::new);
            MenuScreens.register(ModContent.EMC_CRAFTER_MENU.get(), EmcCrafterScreen::new);
            MenuScreens.register(ModContent.EMC_CONVERTER_MENU.get(), EmcConverterScreen::new);
            for (int i = 0; i < 5; i++) {
                ItemBlockRenderTypes.setRenderLayer(ModContent.getEmcFluidSource(i).get(), RenderType.translucent());
                ItemBlockRenderTypes.setRenderLayer(ModContent.getEmcFluidFlowing(i).get(), RenderType.translucent());
            }
        });
    }
}
