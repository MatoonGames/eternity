package com.matoon.eternity.dev;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Development-only fixes for bugs in the ForgeGradle dev workspace itself. Loaded only
 * through {@code -Dfml.coreMods.load}, which the run tasks in build.gradle pass. The
 * release jar's manifest does not name it as a coremod, so players never run it.
 */
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.Name("EternityDevFixes")
@IFMLLoadingPlugin.TransformerExclusions("com.matoon.eternity.dev")
public class DevFixesPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[]{RespawnFieldFix.class.getName()};
    }

    @Nullable
    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Nullable
    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
