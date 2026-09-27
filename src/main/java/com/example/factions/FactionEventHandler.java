package com.example.factions;

import com.example.factions.FactionConfig;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Аналог основной части старого factions.js: блокировка ПКМ по чужим блокам/предметам,
 * блокировка установки чужих блоков, приветствие при входе, команда /faction.
 *
 * Крафт-блокировка (самое важное — без потери ингредиентов при отказе) сделана
 * отдельно в FactionCraftMixin, потому что событий, которые срабатывают ДО списания
 * ингредиентов крафта, в обычном (не-mixin) API NeoForge нет.
 */
@EventBusSubscriber(modid = FactionsMod.MODID)
public class FactionEventHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getLevel().getBlockState(event.getPos()).getBlock());
        if (!FactionManager.canUse(player, id)) {
            event.setCanceled(true);
            blockedMessage(player, id);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem());
        if (!FactionManager.canUse(player, id)) {
            event.setCanceled(true);
            blockedMessage(player, id);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (!FactionManager.canUse(player, id)) {
            event.setCanceled(true);
            blockedMessage(player, id);
        }
    }
    
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getTarget().getType());
        if (!FactionManager.canUse(player, id)) {
            event.setCanceled(true);
            blockedMessage(player, id);
        }
    }

    @SubscribeEvent
    public static void onMount(net.neoforged.neoforge.event.entity.EntityMountEvent event) {
        if (!event.isMounting()) return;
        if (!(event.getEntityMounting() instanceof ServerPlayer player)) return;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(event.getEntityBeingMounted().getType());
        if (!FactionManager.canUse(player, id)) {
            event.setCanceled(true);
            blockedMessage(player, id);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
            net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
            if (held.isEmpty()) continue;

            ResourceLocation id = BuiltInRegistries.ITEM.getKey(held.getItem());
            if (FactionManager.canUse(player, id)) continue;

            if (!player.getInventory().add(held.copy())) {
                player.drop(held.copy(), false);
            }
            player.setItemInHand(hand, net.minecraft.world.item.ItemStack.EMPTY);
            blockedMessage(player, id);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Faction faction = FactionManager.get(player);
        if (faction == null) {
            player.sendSystemMessage(Component.literal("Добро пожаловать! Выбери фракцию: /faction choose <novizna|steampunk|magic>"));
        } else {
            player.sendSystemMessage(Component.literal("Твоя фракция: " + faction.displayName));
        }
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("faction")
                .then(Commands.literal("info").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    Faction f = FactionManager.get(player);
                    ctx.getSource().sendSuccess(() -> Component.literal(
                        f == null ? "Фракция не выбрана." : "Твоя фракция: " + f.displayName + " (" + f.description + ")"
                    ), false);
                    return 1;
                }))
                .then(Commands.literal("choose")
                    .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        if (FactionManager.hasChosen(player)) {
                            ctx.getSource().sendFailure(Component.literal("Фракция уже выбрана. Изменить может только админ."));
                            return 0;
                        }
                        Faction faction = Faction.byId(StringArgumentType.getString(ctx, "id"));
                        if (faction == null) {
                            ctx.getSource().sendFailure(Component.literal("Неверная фракция. novizna / steampunk / magic"));
                            return 0;
                        }
                        FactionManager.set(player, faction);
                        ctx.getSource().sendSuccess(() -> Component.literal("Выбрана фракция: " + faction.displayName), false);
                        return 1;
                    }))
                )
                .then(Commands.literal("admin")
                    .requires(src -> src.hasPermission(2)) // только оператор
                    .then(Commands.literal("set")
                        .then(Commands.argument("target", EntityArgument.player())
                            .then(Commands.argument("id", StringArgumentType.word()).executes(ctx -> {
                                ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                Faction faction = Faction.byId(StringArgumentType.getString(ctx, "id"));
                                if (faction == null) {
                                    ctx.getSource().sendFailure(Component.literal("Неверная фракция."));
                                    return 0;
                                }
                                FactionManager.set(target, faction);
                                target.sendSystemMessage(Component.literal("Администратор назначил фракцию: " + faction.displayName));
                                ctx.getSource().sendSuccess(() -> Component.literal(target.getName().getString() + " -> " + faction.displayName), true);
                                return 1;
                            }))
                        )
                    )
                    .then(Commands.literal("reset")
                        .then(Commands.argument("target", EntityArgument.player()).executes(ctx -> {
                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                            FactionManager.set(target, null);
                            target.sendSystemMessage(Component.literal("Фракция сброшена администратором."));
                            ctx.getSource().sendSuccess(() -> Component.literal("Сброшено: " + target.getName().getString()), true);
                            return 1;
                        }))
                    )
                )
        );
    }

    static void blockedMessage(ServerPlayer player, ResourceLocation id) {
        Faction owner = FactionConfig.ownerOf(id.toString());
        String ownerName = owner != null ? owner.displayName : "другой фракции";
        player.sendSystemMessage(Component.literal("Это относится к фракции «" + ownerName + "». Тебе недоступно."));
    }
}
