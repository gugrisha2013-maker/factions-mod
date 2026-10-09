package com.example.factions;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

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
        checkCityAccess(event, player, event.getPos());
        if (event.isCanceled()) return;

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(event.getPlacedBlock().getBlock());
        if (!FactionManager.canUse(player, id)) {
            event.setCanceled(true);
            blockedMessage(player, id);
        }
    }
    
        @SubscribeEvent
    public static void onCityBlockBreak(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        checkCityAccess(event, player, event.getPos());
    }

      private static <T extends net.neoforged.bus.api.Event & net.neoforged.bus.api.ICancellableEvent> void checkCityAccess(T event, ServerPlayer player, net.minecraft.core.BlockPos pos) {
        net.minecraft.server.level.ServerLevel level = (net.minecraft.server.level.ServerLevel) player.level();
        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(pos);
        City city = CityManager.cityAt(level, chunk);
        if (city == null) return;
        if (city.isMember(player.getUUID())) return;

        event.setCanceled(true);
        player.sendSystemMessage(Component.literal("Этот участок принадлежит городу «" + city.name + "»."));
    }

        @SubscribeEvent
    public static void onExplosion(net.neoforged.neoforge.event.level.ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;

        event.getAffectedBlocks().removeIf(pos -> {
            net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(pos);
            return CityManager.cityAt(level, chunk) != null;
        });
    }

    private static final java.util.Map<java.util.UUID, String> LAST_ZONE = new java.util.HashMap<>();

    @SubscribeEvent
    public static void onZoneNotify(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;

        net.minecraft.world.level.ChunkPos chunk = new net.minecraft.world.level.ChunkPos(player.blockPosition());
        City city = CityManager.cityAt(level, chunk);

        String zone;
        if (city != null) {
            zone = "Город «" + city.name + "»";
        } else {
            Faction territory = TerritoryManager.territoryAt(player.getX(), player.getZ());
            zone = territory != null ? "Территория «" + territory.displayName + "»" : "Нейтральный спаун";
        }

        String previous = LAST_ZONE.get(player.getUUID());
        if (!zone.equals(previous)) {
            LAST_ZONE.put(player.getUUID(), zone);
            player.sendSystemMessage(Component.literal("Вы вошли: " + zone));
        }
    }

        @SubscribeEvent
    public static void onFluidSpread(net.neoforged.neoforge.event.level.BlockEvent.FluidPlaceBlockEvent event) {
        if (!(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)) return;

        net.minecraft.world.level.ChunkPos targetChunk = new net.minecraft.world.level.ChunkPos(event.getPos());
        net.minecraft.world.level.ChunkPos sourceChunk = new net.minecraft.world.level.ChunkPos(event.getFluidPos());

        City targetCity = CityManager.cityAt(level, targetChunk);
        if (targetCity == null) return;

        City sourceCity = CityManager.cityAt(level, sourceChunk);
        if (targetCity.equals(sourceCity)) return;

        event.setCanceled(true);
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

        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD,
                net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.FEET}) {
            net.minecraft.world.item.ItemStack worn = player.getItemBySlot(slot);
            if (worn.isEmpty()) continue;

            ResourceLocation id = BuiltInRegistries.ITEM.getKey(worn.getItem());
            if (FactionManager.canUse(player, id)) continue;

            player.setItemSlot(slot, net.minecraft.world.item.ItemStack.EMPTY);
            if (!player.getInventory().add(worn.copy())) {
                player.drop(worn.copy(), false);
            }
            blockedMessage(player, id);
        }

        if (net.neoforged.fml.ModList.get().isLoaded("curios")) {
            CuriosIntegration.enforce(player);
        }
    }

    private static final double BORDER_VIEW_DISTANCE = 40.0;
    private static final double PARTICLE_STEP = 2.0;
    private static final double LINE_LENGTH = 30.0;

    @SubscribeEvent
    public static void onFactionBuffs(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 20 != 0) return;

        Faction faction = FactionManager.get(player);

        if (faction == Faction.STEAMPUNK && TerritoryManager.isInOwnTerritory(player)) {
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.HEALTH_BOOST, 60, 1, false, false, true));
        }

        if (faction == Faction.NOVIZNA && TerritoryManager.isInOwnTerritory(player)) {
            NoviznaEnergyBuff.apply(player);
        }

        if (faction == Faction.MAGIC && TerritoryManager.isInOwnTerritory(player)) {
            ArsNouveauManaBuff.apply(player);
        }
    }

    @SubscribeEvent
    public static void onBorderParticles(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.tickCount % 40 != 0) return;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;

        double x = player.getX();
        double z = player.getZ();
        double y = player.getY() + 1.0;

        double spawnRadius = FactionConfig.SPAWN_RADIUS.get();
        double dist = Math.sqrt(x * x + z * z);

        if (Math.abs(dist - spawnRadius) < BORDER_VIEW_DISTANCE) {
            double playerAngle = Math.atan2(z, x);
            double angularStep = PARTICLE_STEP / spawnRadius;
            double angularRange = LINE_LENGTH / spawnRadius;
            for (double a = -angularRange; a <= angularRange; a += angularStep) {
                double angle = playerAngle + a;
                double px = Math.cos(angle) * spawnRadius;
                double pz = Math.sin(angle) * spawnRadius;
                level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.END_ROD, false,
                        px, y, pz, 1, 0, 0, 0, 0);
            }
        }

        double offset = Math.toRadians(FactionConfig.ROTATION_OFFSET.get());
        for (int i = 0; i < 3; i++) {
            double lineAngle = offset + Math.toRadians(120.0 * i);
            double dx = Math.cos(lineAngle);
            double dz = Math.sin(lineAngle);

            double t = x * dx + z * dz;
            if (t < spawnRadius) continue;

            double projX = dx * t;
            double projZ = dz * t;
            double perpDist = Math.sqrt((x - projX) * (x - projX) + (z - projZ) * (z - projZ));
            if (perpDist > BORDER_VIEW_DISTANCE) continue;

            for (double s = t - LINE_LENGTH; s <= t + LINE_LENGTH; s += PARTICLE_STEP) {
                if (s < spawnRadius) continue;
                double px = dx * s;
                double pz = dz * s;
                level.sendParticles(player, net.minecraft.core.particles.ParticleTypes.END_ROD, false,
                        px, y, pz, 1, 0, 0, 0, 0);
            }
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
                    .requires(src -> src.hasPermission(2))
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

        CityCommands.register(event.getDispatcher());
    }

    static void blockedMessage(ServerPlayer player, ResourceLocation id) {
        Faction owner = FactionConfig.ownerOf(id.toString());
        String ownerName = owner != null ? owner.displayName : "другой фракции";
        player.sendSystemMessage(Component.literal("Это относится к фракции «" + ownerName + "». Тебе недоступно."));
    }
}
