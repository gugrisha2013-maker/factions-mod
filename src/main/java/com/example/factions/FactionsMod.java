package com.example.factions;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(FactionsMod.MODID)
public class FactionsMod {

    public static final String MODID = "factionsmod"; // поменяй под свой modid

    public FactionsMod(IEventBus modBus) {
        FactionManager.register(modBus);
    }
}
