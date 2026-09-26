package com.example.factions;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Хранит выбранную фракцию игрока через NeoForge Data Attachments API
 * (аналог persistentData.putString из KubeJS, но с нормальным NBT-сохранением
 * "из коробки" и без ручной сериализации).
 *
 * ВАЖНО: точное имя реестра/метода регистрации Attachment Types может отличаться
 * между минорными версиями NeoForge 1.21.x — сверься с примером в MDK
 * (Neo Forge Documentation -> "Attachments"), если при сборке будет ошибка типов.
 */
public class FactionManager {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, FactionsMod.MODID);

    // Храним как строку ("novizna"/"steampunk"/"magic") либо пустую строку = не выбрано.
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<String>> FACTION =
            ATTACHMENT_TYPES.register("faction",
                    () -> AttachmentType.builder(() -> "")
                            .serialize(Codec.STRING)
                            .build());

    public static void register(IEventBus modBus) {
        ATTACHMENT_TYPES.register(modBus);
    }

    public static Faction get(ServerPlayer player) {
        String raw = player.getData(FACTION);
        return Faction.byId(raw);
    }

    public static void set(ServerPlayer player, Faction faction) {
        player.setData(FACTION, faction == null ? "" : faction.name().toLowerCase());
    }

    public static boolean hasChosen(ServerPlayer player) {
        return get(player) != null;
    }

    /**
     * Может ли игрок пользоваться предметом/блоком с данным registry id.
     */
    public static boolean canUse(ServerPlayer player, ResourceLocation id) {
        Faction owner = Faction.ownerOf(id.toString());
        if (owner == null) return true; // общий предмет — доступен всем
        return owner == get(player);
    }
}
