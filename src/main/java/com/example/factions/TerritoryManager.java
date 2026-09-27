package com.example.factions;

import net.minecraft.server.level.ServerPlayer;

public class TerritoryManager {

    /** Возвращает фракцию территории в этой точке, или null если это нейтральный спаун. */
    public static Faction territoryAt(double x, double z) {
        double spawnRadius = FactionConfig.SPAWN_RADIUS.get();
        double dist = Math.sqrt(x * x + z * z);
        if (dist <= spawnRadius) return null;

        double angle = Math.toDegrees(Math.atan2(z, x));
        if (angle < 0) angle += 360;

        double offset = FactionConfig.ROTATION_OFFSET.get();
        double adjusted = (angle - offset + 360) % 360;

        if (adjusted < 120) return Faction.MAGIC;
        if (adjusted < 240) return Faction.STEAMPUNK;
        return Faction.NOVIZNA;
    }

    public static boolean isInOwnTerritory(ServerPlayer player) {
        Faction own = FactionManager.get(player);
        if (own == null) return false;
        return territoryAt(player.getX(), player.getZ()) == own;
    }
}
