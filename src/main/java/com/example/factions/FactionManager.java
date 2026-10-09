package com.example.factions;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.UUID;

public class CityManager {

    public static boolean exists(ServerLevel level, String name) {
        return CitySavedData.get(level).exists(name);
    }

    public static City create(ServerLevel level, String name, UUID mayor, ChunkPos startingChunk) {
        City city = CitySavedData.get(level).create(name, mayor);
        city.claim(startingChunk);
        CitySavedData.get(level).setDirty();
        return city;
    }

    public static City get(ServerLevel level, String name) {
        return CitySavedData.get(level).get(name);
    }

    public static City cityAt(ServerLevel level, ChunkPos pos) {
        return CitySavedData.get(level).cityAt(pos);
    }

    public static City cityOf(ServerLevel level, UUID player) {
        return CitySavedData.get(level).cityOf(player);
    }

    public static void delete(ServerLevel level, String name) {
        CitySavedData.get(level).remove(name);
    }

    public static void markDirty(ServerLevel level) {
        CitySavedData.get(level).setDirty();
    }
}
