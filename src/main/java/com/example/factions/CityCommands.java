package com.example.factions;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CityCommands {

    // Приглашения: кого пригласили -> название города
    private static final Map<UUID, String> INVITES = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("city")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> create(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("claim").executes(ctx -> claim(ctx.getSource())))
                .then(Commands.literal("unclaim").executes(ctx -> unclaim(ctx.getSource())))
                .then(Commands.literal("invite")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> invite(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("join").executes(ctx -> join(ctx.getSource())))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(Commands.literal("kick")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> kick(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("transfer")
                    .then(Commands.argument("target", EntityArgument.player())
                        .executes(ctx -> transfer(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                .then(Commands.literal("delete").executes(ctx -> delete(ctx.getSource())))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
        );
    }

    // ---------- помощники ----------

    // Данные городов всегда берём из обычного мира, чтобы они были одни на весь сервер
    private static ServerLevel data(CommandSourceStack src) {
        return src.getServer().overworld();
    }

    private static int fail(CommandSourceStack src, String text) {
        src.sendFailure(Component.literal(text));
        return 0;
    }

    private static int ok(CommandSourceStack src, String text) {
        src.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    // Возвращает город игрока, если он мэр. Иначе пишет ошибку и возвращает null
    private static City mayorCity(CommandSourceStack src, ServerPlayer player) {
        City city = CityManager.cityOf(data(src), player.getUUID());
        if (city == null) {
            src.sendFailure(Component.literal("Ты не состоишь в городе."));
            return null;
        }
        if (!city.mayor.equals(player.getUUID())) {
            src.sendFailure(Component.literal("Это может делать только мэр."));
            return null;
        }
        return city;
    }

    // ---------- команды ----------

    private static int create(CommandSourceStack src, String name) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = data(src);

        if (player.level().dimension() != Level.OVERWORLD) {
            return fail(src, "Города можно основывать только в обычном мире.");
        }
        if (CityManager.cityOf(level, player.getUUID()) != null) {
            return fail(src, "Ты уже состоишь в городе. Сначала выйди: /city leave");
        }
        if (CityManager.exists(level, name)) {
            return fail(src, "Город с таким названием уже есть.");
        }
        if (TerritoryManager.territoryAt(player.getX(), player.getZ()) == null) {
            return fail(src, "В круге спауна нельзя основать город.");
        }
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        if (CityManager.cityAt(level, chunk) != null) {
            return fail(src, "Этот чанк уже принадлежит городу.");
        }

        CityManager.create(level, name, player.getUUID(), chunk);
        return ok(src, "Город «" + name + "» создан. Ты мэр. Чанк застолблён.");
    }

    private static int claim(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        City city = mayorCity(src, player);
        if (city == null) return 0;

        ServerLevel level = data(src);
        if (player.level().dimension() != Level.OVERWORLD) {
            return fail(src, "Клеймить можно только в обычном мире.");
        }
        if (TerritoryManager.territoryAt(player.getX(), player.getZ()) == null) {
            return fail(src, "В круге спауна нельзя клеймить чанки.");
        }
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        if (city.owns(chunk)) {
            return fail(src, "Этот чанк уже твой.");
        }
        if (CityManager.cityAt(level, chunk) != null) {
            return fail(src, "Этот чанк принадлежит другому городу.");
        }
        if (city.claimedChunks.size() >= city.chunkLimit()) {
            return fail(src, "Лимит чанков исчерпан (" + city.chunkLimit() + ").");
        }

        city.claim(chunk);
        CityManager.markDirty(level);
        return ok(src, "Чанк застолблён (" + city.claimedChunks.size() + "/" + city.chunkLimit() + ").");
    }

    private static int unclaim(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        City city = mayorCity(src, player);
        if (city == null) return 0;

        ChunkPos chunk = new ChunkPos(player.blockPosition());
        if (!city.owns(chunk)) {
            return fail(src, "Этот чанк не принадлежит твоему городу.");
        }
        if (city.claimedChunks.size() <= 1) {
            return fail(src, "Нельзя убрать последний чанк. Если город не нужен: /city delete");
        }

        city.claimedChunks.remove(chunk.toLong());
        CityManager.markDirty(data(src));
        return ok(src, "Чанк освобождён (" + city.claimedChunks.size() + "/" + city.chunkLimit() + ").");
    }

    private static int invite(CommandSourceStack src, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        City city = mayorCity(src, player);
        if (city == null) return 0;

        if (target.getUUID().equals(player.getUUID())) {
            return fail(src, "Себя приглашать не нужно.");
        }
        if (CityManager.cityOf(data(src), target.getUUID()) != null) {
            return fail(src, "Этот игрок уже состоит в городе.");
        }

        INVITES.put(target.getUUID(), city.name);
        target.sendSystemMessage(Component.literal(
                "Тебя пригласили в город «" + city.name + "». Принять: /city join"));
        return ok(src, "Приглашение отправлено: " + target.getName().getString());
    }

    private static int join(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = data(src);

        String cityName = INVITES.get(player.getUUID());
        if (cityName == null) {
            return fail(src, "У тебя нет приглашений.");
        }
        if (CityManager.cityOf(level, player.getUUID()) != null) {
            INVITES.remove(player.getUUID());
            return fail(src, "Ты уже состоишь в городе.");
        }
        City city = CityManager.get(level, cityName);
        if (city == null) {
            INVITES.remove(player.getUUID());
            return fail(src, "Этот город больше не существует.");
        }

        city.members.add(player.getUUID());
        INVITES.remove(player.getUUID());
        CityManager.markDirty(level);

        ServerPlayer mayor = src.getServer().getPlayerList().getPlayer(city.mayor);
        if (mayor != null) {
            mayor.sendSystemMessage(Component.literal(
                    player.getName().getString() + " вступил в город. Лимит чанков: " + city.chunkLimit()));
        }
        return ok(src, "Ты вступил в город «" + city.name + "».");
    }

    private static int leave(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        ServerLevel level = data(src);

        City city = CityManager.cityOf(level, player.getUUID());
        if (city == null) {
            return fail(src, "Ты не состоишь в городе.");
        }
        if (city.mayor.equals(player.getUUID())) {
            return fail(src, "Ты мэр. Передай мэрию (/city transfer ник) или удали город (/city delete).");
        }

        city.members.remove(player.getUUID());
        CityManager.markDirty(level);
        return ok(src, "Ты вышел из города «" + city.name + "».");
    }

    private static int kick(CommandSourceStack src, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        City city = mayorCity(src, player);
        if (city == null) return 0;

        if (target.getUUID().equals(player.getUUID())) {
            return fail(src, "Себя выгнать нельзя.");
        }
        if (!city.isMember(target.getUUID())) {
            return fail(src, "Этот игрок не живёт в твоём городе.");
        }

        city.members.remove(target.getUUID());
        CityManager.markDirty(data(src));
        target.sendSystemMessage(Component.literal("Тебя выгнали из города «" + city.name + "»."));
        return ok(src, "Выгнан: " + target.getName().getString());
    }

    private static int transfer(CommandSourceStack src, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        City city = mayorCity(src, player);
        if (city == null) return 0;

        if (target.getUUID().equals(player.getUUID())) {
            return fail(src, "Ты уже мэр.");
        }
        if (!city.isMember(target.getUUID())) {
            return fail(src, "Этот игрок не живёт в твоём городе.");
        }

        city.mayor = target.getUUID();
        CityManager.markDirty(data(src));
        target.sendSystemMessage(Component.literal("Теперь ты мэр города «" + city.name + "»."));
        return ok(src, "Мэрия передана: " + target.getName().getString());
    }

    private static int delete(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        City city = mayorCity(src, player);
        if (city == null) return 0;

        String name = city.name;
        CityManager.delete(data(src), name);
        return ok(src, "Город «" + name + "» удалён.");
    }

    private static int info(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        City city = CityManager.cityOf(data(src), player.getUUID());
        if (city == null) {
            return fail(src, "Ты не состоишь в городе.");
        }

        ServerPlayer mayor = src.getServer().getPlayerList().getPlayer(city.mayor);
        String mayorName = mayor != null ? mayor.getName().getString() : "не в сети";
        return ok(src, "Город «" + city.name + "»\n"
                + "Мэр: " + mayorName + "\n"
                + "Жителей: " + city.members.size() + "\n"
                + "Чанков: " + city.claimedChunks.size() + "/" + city.chunkLimit());
    }
}
