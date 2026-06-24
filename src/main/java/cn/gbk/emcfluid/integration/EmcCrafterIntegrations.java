package cn.gbk.emcfluid.integration;

import cn.gbk.emcfluid.EmcFluid;
import cn.gbk.emcfluid.content.blockentity.EmcCrafterBlockEntity;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class EmcCrafterIntegrations {
    private EmcCrafterIntegrations() {
    }

    public static List<EmcCrafterIntegration> create(EmcCrafterBlockEntity owner) {
        List<EmcCrafterIntegration> integrations = new ArrayList<>();
        if (ModList.get().isLoaded("ae2")) {
            createIntegration("cn.gbk.emcfluid.integration.ae2.EmcCrafterAe2Integration", owner, integrations);
        }
        if (ModList.get().isLoaded("refinedstorage")) {
            createIntegration("cn.gbk.emcfluid.integration.rs.EmcCrafterRsIntegration", owner, integrations);
        }
        return List.copyOf(integrations);
    }

    public static void register() {
        if (ModList.get().isLoaded("refinedstorage")) {
            invokeStatic("cn.gbk.emcfluid.integration.rs.RsIntegration", "register");
        }
    }

    private static void createIntegration(String className, EmcCrafterBlockEntity owner, List<EmcCrafterIntegration> integrations) {
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor(EmcCrafterBlockEntity.class);
            integrations.add((EmcCrafterIntegration) constructor.newInstance(owner));
        } catch (ReflectiveOperationException | LinkageError e) {
            EmcFluid.LOGGER.warn("Could not load optional integration {}", className, e);
        }
    }

    private static void invokeStatic(String className, String methodName) {
        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getMethod(methodName);
            method.invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            EmcFluid.LOGGER.warn("Could not invoke optional integration {}#{}", className, methodName, e);
        }
    }
}
