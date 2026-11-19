package com.industrialage.ia2;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.LoggerFactory;

/** Minimal loader entry point for IndustrialCraft 2. */
@Mod("ic2")
public final class IndustrialAge2 {
    /** Called by NeoForge when the mod is constructed. */
    public IndustrialAge2(ModContainer container) {
        var metadata = container.getModInfo();
        LoggerFactory.getLogger(container.getModId())
                .info("Loaded {} {}", metadata.getDisplayName(), metadata.getVersion());
    }
}
