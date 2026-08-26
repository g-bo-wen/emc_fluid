package cn.gbk.emcfluid.dev;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;
import net.minecraftforge.fml.relauncher.Side;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;

/**
 * Development-only workaround for Forge 2860's userdev3 artifact.
 *
 * <p>The mapped development Side enum contains a synthetic BUKKIT constant,
 * while NetworkRegistry initializes maps only for CLIENT and SERVER. The
 * production universal jar has exactly those two constants. Restore the
 * production shape before FML constructs its network channels.</p>
 */
@IFMLLoadingPlugin.Name("EMC Fluid Forge 2860 development side fix")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public final class DevSideFixCoremod implements IFMLLoadingPlugin {
    public DevSideFixCoremod() {
        try {
            Field valuesField = Side.class.getDeclaredField("$VALUES");
            valuesField.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(valuesField, valuesField.getModifiers() & ~Modifier.FINAL);

            valuesField.set(null, new Side[]{Side.CLIENT, Side.SERVER});
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to normalize Forge 2860 development Side enum", exception);
        }
    }

    @Override
    public String[] getASMTransformerClass() {
        return new String[0];
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return "cn.gbk.emcfluid.dev.DevAccessTransformer";
    }
}
