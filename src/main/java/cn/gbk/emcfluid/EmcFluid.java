package cn.gbk.emcfluid;

import cn.gbk.emcfluid.client.ClientSetup;
import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.data.TierConfigWorldData;
import cn.gbk.emcfluid.integration.EmcCrafterIntegrations;
import cn.gbk.emcfluid.network.ModNetwork;
import cn.gbk.emcfluid.registry.ModContent;
import cn.gbk.emcfluid.util.KnowledgePatternSync;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(EmcFluid.MODID)
public class EmcFluid {
    public static final String MODID = "emcfluid";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EmcFluid(FMLJavaModLoadingContext context) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, EmcFluidConfig.SERVER_SPEC);
        var modBus = context.getModEventBus();
        ModContent.register(modBus);
        modBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(KnowledgePatternSync::onPlayerKnowledgeChanged);
        MinecraftForge.EVENT_BUS.addListener(KnowledgePatternSync::onServerStopped);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientSetup.register(modBus));
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetwork.register();
            EmcCrafterIntegrations.register();
        });
    }

    private void serverStarted(ServerStartedEvent event) {
        TierConfigWorldData.checkAndUpdate(event.getServer());
    }
}
