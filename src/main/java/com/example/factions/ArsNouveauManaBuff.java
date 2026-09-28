package com.example.factions;

import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import net.minecraft.server.level.ServerPlayer;

class ArsNouveauManaBuff {
    private static final int BONUS_MANA_PER_SECOND = 20;

    static void apply(ServerPlayer player) {
        var mana = CapabilityRegistry.getMana(player);
        if (mana == null) return;
        double newMana = Math.min(mana.getMaxMana(), mana.getCurrentMana() + BONUS_MANA_PER_SECOND);
        mana.setMana(newMana);
    }
}
