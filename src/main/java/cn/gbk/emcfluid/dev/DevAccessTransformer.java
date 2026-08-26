package cn.gbk.emcfluid.dev;

import net.minecraftforge.fml.common.asm.transformers.AccessTransformer;

import java.io.IOException;

/** Development-only MCP-name equivalent of optional dependency access transformers. */
public final class DevAccessTransformer extends AccessTransformer {
    public DevAccessTransformer() throws IOException {
        super("META-INF/emcfluid_dev_at.cfg");
    }
}
