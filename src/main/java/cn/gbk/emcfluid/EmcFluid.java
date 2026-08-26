package cn.gbk.emcfluid;

import cn.gbk.emcfluid.config.EmcFluidConfig;
import cn.gbk.emcfluid.data.TierConfigWorldData;
import cn.gbk.emcfluid.integration.EmcCrafterIntegrations;
import cn.gbk.emcfluid.proxy.CommonProxy;
import cn.gbk.emcfluid.registry.ModContent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartedEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppedEvent;
import cn.gbk.emcfluid.util.KnowledgePatternSync;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;

@Mod(
        modid = EmcFluid.MODID,
        name = EmcFluid.NAME,
        version = EmcFluid.VERSION,
        acceptedMinecraftVersions = "[1.12.2]",
        dependencies = "required-after:projecte@[1.12.2-PE1.4.1]"
                + ";after:refinedstorage@[1.6.16]"
                + ";after:appliedenergistics2@[rv6-stable-7]"
                + ";after:ae2fc@[1.0.11]"
                + ";after:jei@[4.16.5.1029]"
)
public final class EmcFluid {
    public static final String MODID = "emcfluid";
    public static final String NAME = "EMC Fluid";
    public static final String VERSION = "1.0.0-mc1.12.2";

    static {
        FluidRegistry.enableUniversalBucket();
    }

    @Mod.Instance(MODID)
    public static EmcFluid instance;

    @SidedProxy(
            clientSide = "cn.gbk.emcfluid.proxy.ClientProxy",
            serverSide = "cn.gbk.emcfluid.proxy.CommonProxy"
    )
    public static CommonProxy proxy;

    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        EmcFluidConfig.load(event.getSuggestedConfigurationFile());
        ModContent.registerFluids();
        ModContent.registerTileEntities();
        proxy.preInit();
        logger.info("Starting {} {} for Minecraft 1.12.2", NAME, VERSION);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
        EmcCrafterIntegrations.register();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit();
        ModContent.validateRegistration();
    }

    @Mod.EventHandler
    public void serverStarted(FMLServerStartedEvent event) {
        net.minecraft.server.MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        TierConfigWorldData.checkAndUpdate(server);
        if (Boolean.getBoolean("emcfluid.dev.stageBProbe")) {
            ModContent.validateWorldFluidInteractions(server);
        }
        if (Boolean.getBoolean("emcfluid.dev.stageCProbe")) {
            runDevelopmentProbe("cn.gbk.emcfluid.dev.StageCProbe", server);
        }
        if (Boolean.getBoolean("emcfluid.dev.stageDProbe")) {
            scheduleDevelopmentProbe("cn.gbk.emcfluid.dev.StageDProbe", server);
        }
        if (Boolean.getBoolean("emcfluid.dev.stageFProbe")) {
            scheduleDevelopmentProbe("cn.gbk.emcfluid.dev.StageFProbe", server);
        }
        if (Boolean.getBoolean("emcfluid.dev.stageGProbe")) {
            scheduleDevelopmentProbe("cn.gbk.emcfluid.dev.StageGProbe", server);
        }
        if (Boolean.getBoolean("emcfluid.dev.stageHProbe")) {
            runDevelopmentProbe("cn.gbk.emcfluid.dev.StageHProbe", server);
        }
    }

    @Mod.EventHandler
    public void serverStopped(FMLServerStoppedEvent event) {
        KnowledgePatternSync.clear();
    }

    private static void runDevelopmentProbe(String className, net.minecraft.server.MinecraftServer server) {
        try {
            Class.forName(className).getMethod("run", net.minecraft.server.MinecraftServer.class).invoke(null, server);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Development probe failed: " + className, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to load development probe: " + className, exception);
        }
    }

    private static void scheduleDevelopmentProbe(String className, net.minecraft.server.MinecraftServer server) {
        try {
            Class.forName(className).getMethod("schedule", net.minecraft.server.MinecraftServer.class)
                    .invoke(null, server);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new IllegalStateException("Development probe scheduling failed: " + className, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to schedule development probe: " + className, exception);
        }
    }
}
