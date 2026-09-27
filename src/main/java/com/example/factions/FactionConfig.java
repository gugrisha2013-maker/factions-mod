package com.example.factions;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Locale;

/**
 * Список modid для каждой фракции, редактируемый БЕЗ пересборки мода.
 * После первого запуска сервера появится файл config/factionsmod-server.toml —
 * его можно открыть текстовым редактором и добавить/убрать modid, затем
 * перезапустить сервер (или /neoforge reload, если поддерживается).
 */
public class FactionConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> NOVIZNA_MODS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> STEAMPUNK_MODS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> MAGIC_MODS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("factions");

        NOVIZNA_MODS = builder
                .comment("Modid'ы фракции Новизна")
                .defineList("novizna", List.of(
                        "mekanism", "mekanismgenerators", "ae2", "appmek", "superbwarfare"
                ), o -> o instanceof String);

        STEAMPUNK_MODS = builder
                .comment("Modid'ы фракции Стимпанк (Create и его аддоны)")
                .defineList("steampunk", List.of(
                        "create", "createaddition", "createdieselgenerators", "createbigcannons",
                        "create_connected", "create_aeronautics",
                        "aeronautics", "aeronautics_bundled", "offroad", "simulated"
                ), o -> o instanceof String);

        MAGIC_MODS = builder
                .comment("Modid'ы фракции Магия")
                .defineList("magic", List.of(
                        "ars_nouveau", "irons_spellbooks", "wizards", "runes", "spell_engine", "spell_power"
                ), o -> o instanceof String);

        builder.pop();
        SPEC = builder.build();
    }

    public static Faction ownerOf(String registryId) {
        if (registryId == null || registryId.isBlank()) return null;
        String namespace = registryId.split(":", 2)[0].toLowerCase(Locale.ROOT);

        // Подстраховка на будущее: любой ещё не внесённый в конфиг create-аддон
        // (id начинается с "create") всё равно считается стимпанком.
        if (namespace.startsWith("create")) return Faction.STEAMPUNK;

        if (NOVIZNA_MODS.get().contains(namespace)) return Faction.NOVIZNA;
        if (STEAMPUNK_MODS.get().contains(namespace)) return Faction.STEAMPUNK;
        if (MAGIC_MODS.get().contains(namespace)) return Faction.MAGIC;
        return null;
    }
}
