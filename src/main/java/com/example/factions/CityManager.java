package com.example.factions;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CityManager {
    private static final Map<String, City> CITIES = new HashMap<>();

    public static boolean exists(String name) {
        return CITIES.containsKey(name.toLowerCase());
    }

    public static void create(String name, UUID mayor) {
        CITIES.put(name.toLowerCase(), new City(name, mayor));
    }

    public static City get(String name) {
        return CITIES.get(name.toLowerCase());
    }
}
