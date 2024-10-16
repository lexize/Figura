package org.figuramc.figura.neoforge;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod("figura")
public class FiguraModForge {
    // dummy empty mod class, we are client only
    // Not anymore lol - Kate
    public FiguraModForge() {
        if (FMLEnvironment.dist == Dist.CLIENT)
            NeoForge.EVENT_BUS.addListener(FiguraModClientNeoForge::cancelVanillaOverlays);
    }
}
