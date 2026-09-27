package com.example.factions;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

@Mod(FactionsMod.MODID)
public class FactionsMod {

    public static final String MODID = "factionsmod";

    public FactionsMod(IEventBus modBus, ModContainer container) {
        FactionManager.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, FactionConfig.SPEC);
    }
}
