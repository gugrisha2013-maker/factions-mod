package com.example.factions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CitySavedData extends SavedData {
    private static final String ID = "factionsmod_cities";

    private final Map<String, City> cities = new HashMap<>();

    public static CitySavedData get(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(CitySavedData::new, CitySavedData::load),
                ID
        );
    }

    public boolean exists(String name) {
        return cities.containsKey(name.toLowerCase());
    }

    public City get(String name) {
        return cities.get(name.toLowerCase());
    }

    public City create(String name, UUID mayor) {
        City city = new City(name, mayor);
        cities.put(name.toLowerCase(), city);
        setDirty();
        return city;
    }

    public void remove(String name) {
        cities.remove(name.toLowerCase());
        setDirty();
    }

    public City cityAt(net.minecraft.world.level.ChunkPos pos) {
        for (City city : cities.values()) {
            if (city.owns(pos)) return city;
        }
        return null;
    }

    public City cityOf(UUID player) {
        for (City city : cities.values()) {
            if (city.isMember(player)) return city;
        }
        return null;
    }

    private static CitySavedData load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        CitySavedData data = new CitySavedData();
        ListTag list = tag.getList("cities", 10);
        for (int i = 0; i < list.size(); i++) {
            City city = City.load(list.getCompound(i));
            data.cities.put(city.name.toLowerCase(), city);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (City city : cities.values()) {
            list.add(city.save());
        }
        tag.put("cities", list);
        return tag;
    }
}
