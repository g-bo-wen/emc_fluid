package cn.gbk.emcfluid.integration;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import net.minecraft.item.Item;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.registries.IForgeRegistry;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EmcCrafterIntegrations {
    private static final String RS_INTEGRATION =
            "cn.gbk.emcfluid.integration.rs.EmcCrafterRsIntegration";
    private static final String RS_REGISTRATION =
            "cn.gbk.emcfluid.integration.rs.RsIntegration";
    private static final String AE2_CRAFTER_TILE =
            "cn.gbk.emcfluid.integration.ae2.EmcCrafterAe2Integration";

    private EmcCrafterIntegrations() {
    }

    public static List<EmcCrafterIntegration> create(EmcCrafterBlockEntity owner) {
        List<EmcCrafterIntegration> integrations = new ArrayList<EmcCrafterIntegration>();
        if (Loader.isModLoaded("refinedstorage")) {
            createIntegration(RS_INTEGRATION, owner, integrations);
        }
        return Collections.unmodifiableList(integrations);
    }

    public static void registerItems(IForgeRegistry<Item> registry) {
        if (Loader.isModLoaded("refinedstorage")) {
            invokeStatic(RS_REGISTRATION, "registerItems", IForgeRegistry.class, registry);
        }
    }

    public static void register() {
        if (Loader.isModLoaded("refinedstorage")) {
            invokeStatic(RS_REGISTRATION, "register", null, null);
        }
        if (hasAe2FluidCrafting()) {
            EmcFluid.logger.info("Enabled AE2 rv6-stable-7 + AE2 Fluid Crafting 1.0.11 EMC Crafter integration");
        } else if (Loader.isModLoaded("appliedenergistics2")) {
            EmcFluid.logger.info("AE2 detected without AE2 Fluid Crafting; EMC fluid patterns are disabled");
        }
    }

    public static Class<? extends EmcCrafterBlockEntity> crafterTileEntityClass() {
        if (!hasAe2FluidCrafting()) {
            return EmcCrafterBlockEntity.class;
        }
        try {
            return Class.forName(AE2_CRAFTER_TILE).asSubclass(EmcCrafterBlockEntity.class);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to load AE2 EMC Crafter TileEntity", exception);
        } catch (LinkageError error) {
            throw new IllegalStateException("Unable to link AE2 EMC Crafter TileEntity", error);
        }
    }

    public static EmcCrafterBlockEntity createCrafterTileEntity() {
        Class<? extends EmcCrafterBlockEntity> tileClass = crafterTileEntityClass();
        try {
            return tileClass.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create EMC Crafter TileEntity "
                    + tileClass.getName(), exception);
        }
    }

    private static boolean hasAe2FluidCrafting() {
        return Loader.isModLoaded("appliedenergistics2") && Loader.isModLoaded("ae2fc");
    }

    private static void createIntegration(String className, EmcCrafterBlockEntity owner,
                                          List<EmcCrafterIntegration> integrations) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(EmcCrafterBlockEntity.class);
            integrations.add((EmcCrafterIntegration) constructor.newInstance(owner));
        } catch (ReflectiveOperationException exception) {
            EmcFluid.logger.warn("Could not load optional integration {}", className, exception);
        } catch (LinkageError error) {
            EmcFluid.logger.warn("Could not link optional integration {}", className, error);
        }
    }

    private static void invokeStatic(String className, String methodName,
                                     Class<?> parameterType, Object argument) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = parameterType == null
                    ? clazz.getMethod(methodName)
                    : clazz.getMethod(methodName, parameterType);
            if (parameterType == null) {
                method.invoke(null);
            } else {
                method.invoke(null, argument);
            }
        } catch (ReflectiveOperationException exception) {
            EmcFluid.logger.warn("Could not invoke optional integration {}#{}",
                    className, methodName, exception);
        } catch (LinkageError error) {
            EmcFluid.logger.warn("Could not link optional integration {}#{}",
                    className, methodName, error);
        }
    }
}
