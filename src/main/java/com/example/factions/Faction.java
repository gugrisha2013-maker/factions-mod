package com.example.factions;

import java.util.List;
import java.util.Locale;

/**
 * Три фракции сервера. Логика 1-в-1 повторяет старый KubeJS-скрипт:
 * у каждой фракции список префиксов namespace (modid) её предметов/блоков.
 * Create и все его аддоны (namespace начинается с "create") автоматически
 * относятся к STEAMPUNK, даже если аддон появится в будущем.
 */
public enum Faction {

    NOVIZNA("Новизна", "Mekanism, AE2 и Superb Warfare", List.of(
            "mekanism", "mekanismgenerators", "ae2", "appmek", "superbwarfare"
    )),

    STEAMPUNK("Стимпанк", "Create и его аддоны", List.of(
            "create", "createaddition", "createdieselgenerators",
            "createbigcannons", "create_connected", "create_aeronautics"
    )),

    MAGIC("Магия", "Ars Nouveau, Iron's Spells, Wizards и руны", List.of(
            "ars_nouveau", "irons_spellbooks", "wizards", "runes",
            "spell_engine", "spell_power"
    ));

    public final String displayName;
    public final String description;
    public final List<String> namespaces;

    Faction(String displayName, String description, List<String> namespaces) {
        this.displayName = displayName;
        this.description = description;
        this.namespaces = namespaces;
    }

    /**
     * Определяет фракцию-владельца по registry id предмета/блока
     * (например "mekanism:digital_miner" -> NOVIZNA).
     * Возвращает null, если предмет общий (JEI, Quark, ваниль и т.д.).
     */
    public static Faction ownerOf(String registryId) {
        if (registryId == null || registryId.isBlank()) return null;
        String namespace = registryId.split(":", 2)[0].toLowerCase(Locale.ROOT);

        // Любой namespace, начинающийся с "create", считается стимпанком —
        // покрывает будущие аддоны create_* / create-*.
        if (namespace.startsWith("create")) return STEAMPUNK;

        for (Faction f : values()) {
            for (String prefix : f.namespaces) {
                if (namespace.equals(prefix)) return f;
            }
        }
        return null;
    }

    public static Faction byId(String id) {
        if (id == null) return null;
        try {
            return Faction.valueOf(id.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
