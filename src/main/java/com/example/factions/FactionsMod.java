package com.example.factions;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FactionsMod.MODID)
public class FactionsMod {

    public static final String MODID = "factionsmod"; // поменяй под свой modid

    public FactionsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        FactionManager.register(modBus);
        // FactionEventHandler подписывается на общий (game) event bus сам,
        // через @Mod.EventBusSubscriber — отдельно регистрировать не нужно.
    }
}
