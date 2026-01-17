package ca.teamdman.sfm.common.compat;

import appeng.api.AEApi;
import ca.teamdman.sfm.common.registry.SFMWellKnownRegistries;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;

public class SFMModCompat {
    public static boolean isMekanismLoaded() {
        return isModLoaded("mekanism");
    }

    static final String AE2_MODID = "appliedenergistics2";

    public static boolean isAE2Loaded() {
        return isModLoaded(AE2_MODID);
    }

    public static boolean isModLoaded(String modid) {
        return Loader.instance().getIndexedModList().containsKey(modid);
    }

}
