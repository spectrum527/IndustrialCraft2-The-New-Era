package com.industrialage.ia2;

import net.neoforged.fml.common.Mod;
import org.slf4j.LoggerFactory;

/** Minimal loader entry point for Industrial Age 2. */
@Mod("ia2")
public final class IndustrialAge2 {
    /** Called by NeoForge when the mod is constructed. */
    public IndustrialAge2() {
        LoggerFactory.getLogger(IndustrialAge2.class).info("Industrial Age 2 loaded");
    }
}
