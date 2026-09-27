package com.example.factions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

class NoviznaEnergyBuff {
    private static final int SCAN_RADIUS = 8;
    private static final int BONUS_ENERGY_PER_SECOND = 40; // подбери значение под свой баланс

    static void apply(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) return;

        BlockPos center = player.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-SCAN_RADIUS, -SCAN_RADIUS, -SCAN_RADIUS),
                center.offset(SCAN_RADIUS, SCAN_RADIUS, SCAN_RADIUS))) {

            String blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getKey(level.getBlockState(pos).getBlock()).toString();
            if (!blockId.startsWith("mekanism:") && !blockId.startsWith("mekanismgenerators:")) continue;

            for (Direction dir : Direction.values()) {
                IEnergyStorage storage = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, dir);
                if (storage != null && storage.canReceive()) {
                    storage.receiveEnergy(BONUS_ENERGY_PER_SECOND, false);
                    break;
                }
            }
        }
    }
}
