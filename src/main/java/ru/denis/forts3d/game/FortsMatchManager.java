package ru.denis.forts3d.game;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import ru.denis.forts3d.block.FortsStructureBlock;
import ru.denis.forts3d.config.FortsConfig;
import ru.denis.forts3d.registry.ModBlocks;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Authoritative server-side state for one Forts match.
 *
 * <p>All economy, structural integrity, projectiles, shields and victory decisions
 * are made here. Clients only render normal Minecraft state and boss bars, which
 * keeps dedicated-server play deterministic and prevents client-side cheating.</p>
 */
public final class FortsMatchManager {
    public record ProjectileData(
        UUID id,
        FortsTeam team,
        int structureDamage,
        int reactorDamage,
        double radius,
        double gravity,
        ResourceKey<Level> dimension,
        long expiresAt
    ) {}

    private static final Map<MinecraftServer, FortsMatchManager> INSTANCES =
        new ConcurrentHashMap<>();

    public static FortsMatchManager get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, ignored -> new FortsMatchManager());
    }

    public static void remove(MinecraftServer server) {
        FortsMatchManager manager = INSTANCES.remove(server);
        if (manager != null) {
            manager.reactorBars.values().forEach(ServerBossEvent::removeAllPlayers);
        }
    }

    private MatchState state = MatchState.IDLE;
    private int remainingTicks;
    private long ticks;
    private boolean fortsBuilt;

    private final EnumMap<FortsTeam, TeamResourceBank> banks =
        new EnumMap<>(FortsTeam.class);
    private final Map<UUID, FortsTeam> players = new ConcurrentHashMap<>();
    private final EnumMap<FortsTeam, BlockPos> reactors =
        new EnumMap<>(FortsTeam.class);
    private final EnumMap<FortsTeam, Integer> reactorHealth =
        new EnumMap<>(FortsTeam.class);
    private final StructureRegistry structures = new StructureRegistry();
    private final EnumMap<FortsTeam, ServerBossEvent> reactorBars =
        new EnumMap<>(FortsTeam.class);
    private final Map<UUID, ProjectileData> projectiles = new ConcurrentHashMap<>();

    private FortsMatchManager() {}

    public MatchState state() {
        return state;
    }

    public int remainingTicks() {
        return remainingTicks;
    }

    public long ticks() {
        return ticks;
    }

    public Map<FortsTeam, TeamResourceBank> banks() {
        return Collections.unmodifiableMap(banks);
    }

    public StructureRegistry structures() {
        return structures;
    }

    public FortsTeam teamOf(UUID id) {
        return players.getOrDefault(id, FortsTeam.SPECTATOR);
    }

    public TeamResourceBank bank(FortsTeam team) {
        return banks.get(team);
    }

    public BlockPos reactorPosition(FortsTeam team) {
        return reactors.get(team);
    }

    public int activeProjectileCount() {
        return projectiles.size();
    }

    public boolean canRegisterProjectiles(int count) {
        removeExpiredProjectileRecords();
        return count > 0
            && projectiles.size() + count <= FortsConfig.MAX_PROJECTILES.get();
    }

    public void create() {
        state = MatchState.PREPARING;
        ticks = 0;
        remainingTicks = FortsConfig.MATCH_TIME_SECONDS.get() * 20;
        fortsBuilt = false;
        players.clear();
        reactors.clear();
        reactorHealth.clear();
        banks.clear();
        structures.clear();
        projectiles.clear();
        reactorBars.values().forEach(ServerBossEvent::removeAllPlayers);

        banks.put(
            FortsTeam.RED,
            new TeamResourceBank(
                FortsConfig.START_METAL.get(),
                FortsConfig.START_ENERGY.get(),
                10_000
            )
        );
        banks.put(
            FortsTeam.BLUE,
            new TeamResourceBank(
                FortsConfig.START_METAL.get(),
                FortsConfig.START_ENERGY.get(),
                10_000
            )
        );
    }

    public boolean join(ServerPlayer player, FortsTeam team) {
        if (team == FortsTeam.SPECTATOR) {
            players.put(player.getUUID(), team);
            removeFromMatchScoreboards(player);
            player.sendSystemMessage(Component.literal("§7Вы стали наблюдателем"));
            return true;
        }

        long count = players.values().stream().filter(value -> value == team).count();
        FortsTeam previous = teamOf(player.getUUID());
        if (previous != team && count >= FortsConfig.MAX_TEAM_SIZE.get()) {
            player.sendSystemMessage(Component.literal("§cВ выбранной команде нет мест"));
            return false;
        }

        players.put(player.getUUID(), team);
        configureScoreboard(player.server, player, team);
        player.sendSystemMessage(Component.literal(
            team.color() + "Вы вступили в команду " + team.id()));

        if (state == MatchState.RUNNING) {
            BlockPos reactor = reactors.get(team);
            if (reactor != null) {
                teleportToFort(player, player.server.overworld(), reactor, team);
            }
            updateGui(player.server);
        }
        return true;
    }

    public void registerReactor(FortsTeam team, BlockPos pos) {
        if (team == FortsTeam.SPECTATOR) {
            return;
        }
        reactors.put(team, pos.immutable());
        reactorHealth.put(team, FortsConfig.REACTOR_HEALTH.get());
    }

    public int reactorHealth(FortsTeam team) {
        return reactorHealth.getOrDefault(team, 0);
    }

    public void damageReactor(
        MinecraftServer server,
        FortsTeam target,
        FortsTeam attacker,
        int damage
    ) {
        if (target == FortsTeam.SPECTATOR
            || damage <= 0
            || state != MatchState.RUNNING
            || (target == attacker && !FortsConfig.FRIENDLY_FIRE.get())) {
            return;
        }
        int current = reactorHealth.getOrDefault(
            target,
            FortsConfig.REACTOR_HEALTH.get()
        );
        int scaledDamage = Math.max(
            1,
            (int)Math.round(damage * FortsConfig.DAMAGE_MULTIPLIER.get())
        );
        int health = Math.max(0, current - scaledDamage);
        reactorHealth.put(target, health);
        updateGui(server);
        if (health == 0) {
            win(server, target.opponent());
        }
    }

    public void reactorRemoved(MinecraftServer server, BlockPos pos) {
        if (state != MatchState.RUNNING) {
            return;
        }
        for (FortsTeam team : FortsTeam.playingTeams()) {
            if (pos.equals(reactors.get(team))) {
                damageReactor(
                    server,
                    team,
                    team.opponent(),
                    FortsConfig.REACTOR_HEALTH.get()
                );
                return;
            }
        }
    }

    public void start(MinecraftServer server) {
        if (state == MatchState.RUNNING) {
            broadcast(server, "§e[Forts 3D] Матч уже запущен");
            return;
        }
        if (state == MatchState.PAUSED) {
            state = MatchState.RUNNING;
            broadcast(server, "§a[Forts 3D] Матч продолжен");
            return;
        }
        if (state == MatchState.IDLE || state == MatchState.FINISHED) {
            create();
        }

        preparePlayersAndForts(server);
        state = MatchState.RUNNING;
        remainingTicks = FortsConfig.MATCH_TIME_SECONDS.get() * 20;
        updateGui(server);
        broadcast(
            server,
            "§6[Forts 3D] Матч начался: уничтожьте реактор противника"
        );
    }

    public void stop(MinecraftServer server) {
        if (state == MatchState.IDLE || state == MatchState.FINISHED) {
            return;
        }
        state = MatchState.FINISHED;
        discardProjectiles(server);
        reactorBars.values().forEach(ServerBossEvent::removeAllPlayers);
        broadcast(server, "§6[Forts 3D] Матч завершён");
    }

    public boolean pause(MinecraftServer server) {
        if (state == MatchState.RUNNING) {
            state = MatchState.PAUSED;
            broadcast(server, "§e[Forts 3D] Матч приостановлен");
            return true;
        }
        if (state == MatchState.PAUSED) {
            state = MatchState.RUNNING;
            broadcast(server, "§a[Forts 3D] Матч продолжен");
            return true;
        }
        return false;
    }

    public void win(MinecraftServer server, FortsTeam team) {
        if (state == MatchState.FINISHED) {
            return;
        }
        state = MatchState.FINISHED;
        discardProjectiles(server);
        reactorBars.values().forEach(ServerBossEvent::removeAllPlayers);
        broadcast(server, team.color() + "Команда " + team.id() + " победила!");
    }

    public void tick(MinecraftServer server) {
        if (state != MatchState.RUNNING) {
            return;
        }
        ticks++;
        remainingTicks--;
        if (remainingTicks <= 0) {
            stop(server);
            return;
        }

        if (ticks % 20 == 0) {
            updateGui(server);
            produceReactorEnergy();
            tickRadar(server.overworld());
        }
        tickProjectiles(server);
        tickShields(server.overworld());

        if (FortsConfig.STRUCTURAL_COLLAPSE.get()
            && ticks % FortsConfig.STRUCTURE_TICK_RATE.get() == 0) {
            collapseUnsupported(server.overworld());
        }
    }

    /**
     * Validates a player placement and charges the team atomically.
     *
     * @return null when placement is accepted, otherwise a user-facing reason.
     */
    public Component validateAndPayForPlacement(
        ServerPlayer player,
        BlockPos pos,
        FortsStructureBlock block
    ) {
        if (state != MatchState.RUNNING) {
            return null;
        }
        FortsTeam team = teamOf(player.getUUID());
        TeamResourceBank teamBank = bank(team);
        if (teamBank == null) {
            return Component.literal("§cСначала вступите в команду");
        }

        if (FortsConfig.LIMITED_BUILD_ZONE.get()) {
            BlockPos reactor = reactors.get(team);
            long radius = FortsConfig.BUILD_RADIUS.get();
            if (reactor == null || reactor.distSqr(pos) > radius * radius) {
                return Component.literal("§cСтроительство вне зоны вашей базы запрещено");
            }
        }

        Technology technology = block.requiredTechnology();
        if (FortsConfig.REQUIRE_TECH.get()
            && technology != null
            && !teamBank.has(technology)) {
            return Component.literal("§cНе изучена технология: " + technology.title());
        }

        if (!player.getAbilities().instabuild
            && !teamBank.spend(block.metalCost(), 0)) {
            return Component.literal(
                "§cНедостаточно металла: требуется " + block.metalCost());
        }
        return null;
    }

    public boolean canBreak(ServerPlayer player, BlockPos pos, boolean refund) {
        if (state != MatchState.RUNNING || player.hasPermissions(2)) {
            return true;
        }
        StructureRegistry.Node node = structures.nodeAt(pos);
        if (node == null) {
            return true;
        }
        FortsTeam playerTeam = teamOf(player.getUUID());
        if (node.team() != playerTeam) {
            player.sendSystemMessage(Component.literal(
                "§cНельзя вручную ломать конструкцию противника"));
            return false;
        }
        if (pos.equals(reactors.get(playerTeam))) {
            player.sendSystemMessage(Component.literal(
                "§cРеактор можно уничтожить только оружием"));
            return false;
        }
        if (refund) {
            TeamResourceBank teamBank = bank(playerTeam);
            if (teamBank != null) {
                teamBank.addMetal(node.metalCost() / 2L);
            }
        }
        return true;
    }

    public int repair(ServerPlayer player, BlockPos pos, int requestedRepair) {
        if (state != MatchState.RUNNING) {
            return -1;
        }
        StructureRegistry.Node node = structures.nodeAt(pos);
        FortsTeam team = teamOf(player.getUUID());
        if (node == null || node.team() != team) {
            return -1;
        }
        int actual = Math.min(
            Math.max(0, requestedRepair),
            node.maxIntegrity() - node.integrity()
        );
        if (actual == 0) {
            return node.integrity();
        }
        long metalCost = Math.max(1, (actual + 24L) / 25L);
        TeamResourceBank teamBank = bank(team);
        if (teamBank == null || !teamBank.spend(metalCost, 0)) {
            return -2;
        }
        return structures.repair(pos, actual);
    }

    public boolean registerProjectile(
        Projectile projectile,
        FortsTeam team,
        int structureDamage,
        int reactorDamage,
        double radius,
        double gravity
    ) {
        removeExpiredProjectileRecords();
        if (projectiles.size() >= FortsConfig.MAX_PROJECTILES.get()) {
            return false;
        }
        projectiles.put(projectile.getUUID(), new ProjectileData(
            projectile.getUUID(),
            team,
            Math.max(1, structureDamage),
            Math.max(1, reactorDamage),
            Math.max(1, radius),
            Math.max(0, gravity),
            projectile.level().dimension(),
            ticks + 20L * 30L
        ));
        return true;
    }

    public boolean isManagedProjectile(UUID id) {
        return projectiles.containsKey(id);
    }

    public void projectileImpact(
        ServerLevel level,
        Projectile projectile,
        HitResult hitResult
    ) {
        ProjectileData data = projectiles.get(projectile.getUUID());
        if (data == null || state != MatchState.RUNNING) {
            return;
        }
        Vec3 hit = hitResult.getLocation();
        applyAreaDamage(level, hit, data);
        data = new ProjectileData(
            data.id(),
            data.team(),
            data.structureDamage(),
            data.reactorDamage(),
            data.radius(),
            data.gravity(),
            data.dimension(),
            ticks + 10
        );
        projectiles.put(data.id(), data);
    }

    private void applyAreaDamage(
        ServerLevel level,
        Vec3 impact,
        ProjectileData projectile
    ) {
        for (FortsTeam target : FortsTeam.playingTeams()) {
            BlockPos reactor = reactors.get(target);
            if (reactor == null
                || (target == projectile.team() && !FortsConfig.FRIENDLY_FIRE.get())) {
                continue;
            }
            double distance = Vec3.atCenterOf(reactor).distanceTo(impact);
            if (distance <= projectile.radius() + 2.0) {
                double falloff = Math.max(
                    0.2,
                    1.0 - distance / (projectile.radius() + 2.0)
                );
                damageReactor(
                    level.getServer(),
                    target,
                    projectile.team(),
                    (int)Math.round(projectile.reactorDamage() * falloff)
                );
            }
        }

        for (StructureRegistry.Node node : structures.nodes()) {
            if (node.team() == projectile.team() && !FortsConfig.FRIENDLY_FIRE.get()) {
                continue;
            }
            if (node.pos().equals(reactors.get(node.team()))) {
                continue;
            }
            double distance = Vec3.atCenterOf(node.pos()).distanceTo(impact);
            if (distance > projectile.radius()) {
                continue;
            }
            double falloff = Math.max(0.15, 1.0 - distance / projectile.radius());
            int damage = Math.max(
                1,
                (int)Math.round(
                    projectile.structureDamage()
                        * falloff
                        * FortsConfig.DAMAGE_MULTIPLIER.get()
                )
            );
            if (structures.damage(node.pos(), damage) == 0) {
                level.destroyBlock(node.pos(), true);
            }
        }
    }

    private void preparePlayersAndForts(MinecraftServer server) {
        int red = 0;
        int blue = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FortsTeam current = teamOf(player.getUUID());
            if (current == FortsTeam.SPECTATOR) {
                current = red <= blue ? FortsTeam.RED : FortsTeam.BLUE;
                players.put(player.getUUID(), current);
            }
            if (current == FortsTeam.RED) {
                red++;
            } else if (current == FortsTeam.BLUE) {
                blue++;
            }
            configureScoreboard(server, player, current);
        }

        ServerLevel level = server.overworld();
        if (!fortsBuilt) {
            BlockPos spawn = level.getSharedSpawnPos();
            buildTeamFort(level, spawn.offset(-48, 0, 0), FortsTeam.RED);
            buildTeamFort(level, spawn.offset(48, 0, 0), FortsTeam.BLUE);
            fortsBuilt = true;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FortsTeam team = teamOf(player.getUUID());
            BlockPos reactor = reactors.get(team);
            if (reactor != null) {
                teleportToFort(player, level, reactor, team);
            }
        }
    }

    private void buildTeamFort(ServerLevel level, BlockPos horizontal, FortsTeam team) {
        int y = level.getHeight(
            Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            horizontal.getX(),
            horizontal.getZ()
        );
        BlockPos origin = new BlockPos(horizontal.getX(), y, horizontal.getZ());
        BlockPos reactor = StarterFortBuilder.build(
            level,
            origin,
            team,
            structures
        );
        registerReactor(team, reactor);
    }

    private static void teleportToFort(
        ServerPlayer player,
        ServerLevel level,
        BlockPos reactor,
        FortsTeam team
    ) {
        // NeoForge 1.21.1 has the seven-argument overload; the removed boolean
        // parameter was the compilation failure in the original prototype.
        player.teleportTo(
            level,
            reactor.getX() + 0.5,
            reactor.getY() + 1.0,
            reactor.getZ() + 0.5,
            team == FortsTeam.RED ? -90.0F : 90.0F,
            0.0F
        );
    }

    private void produceReactorEnergy() {
        double multiplier = FortsConfig.RESOURCE_MULTIPLIER.get();
        for (TeamResourceBank teamBank : banks.values()) {
            teamBank.addEnergy(Math.round(5 * multiplier));
        }
    }

    private void collapseUnsupported(ServerLevel level) {
        for (BlockPos pos : structures.unsupported()) {
            if (level.isLoaded(pos)) {
                level.destroyBlock(pos, true);
                structures.remove(pos);
            }
        }
    }

    private void tickProjectiles(MinecraftServer server) {
        double wind = FortsConfig.WEATHER_BALLISTICS.get()
            ? Math.sin(ticks / 160.0) * 0.006 * FortsConfig.WIND_MULTIPLIER.get()
            : 0.0;

        Iterator<Map.Entry<UUID, ProjectileData>> iterator =
            projectiles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, ProjectileData> entry = iterator.next();
            ProjectileData data = entry.getValue();
            ServerLevel level = server.getLevel(data.dimension());
            Entity entity = level == null ? null : level.getEntity(data.id());
            if (ticks > data.expiresAt()) {
                if (entity != null) {
                    entity.discard();
                }
                iterator.remove();
                continue;
            }
            if (!(entity instanceof Projectile projectile) || !entity.isAlive()) {
                if (ticks + 5 > data.expiresAt()) {
                    iterator.remove();
                }
                continue;
            }
            Vec3 movement = projectile.getDeltaMovement();
            projectile.setDeltaMovement(
                movement.x + wind,
                movement.y - data.gravity(),
                movement.z - wind * 0.35
            );
        }
    }

    private void tickShields(ServerLevel level) {
        if (ticks % 2 != 0 || projectiles.isEmpty()) {
            return;
        }
        for (StructureRegistry.Node node : structures.nodes()) {
            if (!level.getBlockState(node.pos()).is(ModBlocks.SHIELD_EMITTER.get())) {
                continue;
            }
            TeamResourceBank teamBank = bank(node.team());
            if (teamBank == null
                || (FortsConfig.REQUIRE_TECH.get()
                    && !teamBank.has(Technology.SHIELDS))) {
                continue;
            }
            for (ProjectileData data : projectiles.values()) {
                if (data.team() == node.team()) {
                    continue;
                }
                ServerLevel projectileLevel = level.getServer().getLevel(data.dimension());
                Entity entity = projectileLevel == null
                    ? null
                    : projectileLevel.getEntity(data.id());
                if (entity == null
                    || entity.position().distanceToSqr(Vec3.atCenterOf(node.pos())) > 100.0
                    || !teamBank.spend(0, 15)) {
                    continue;
                }
                entity.discard();
                projectiles.remove(data.id());
                level.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    24,
                    0.4,
                    0.4,
                    0.4,
                    0.08
                );
                level.playSound(
                    null,
                    node.pos(),
                    SoundEvents.BEACON_DEACTIVATE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.6F
                );
                break;
            }
        }
    }

    private void tickRadar(ServerLevel level) {
        for (StructureRegistry.Node node : structures.nodes()) {
            if (!level.getBlockState(node.pos()).is(ModBlocks.RADAR.get())) {
                continue;
            }
            TeamResourceBank teamBank = bank(node.team());
            if (teamBank == null
                || (FortsConfig.REQUIRE_TECH.get()
                    && !teamBank.has(Technology.RADAR))
                || !teamBank.spend(0, 2)) {
                continue;
            }
            Vec3 center = Vec3.atCenterOf(node.pos());
            for (ServerPlayer player : level.players()) {
                FortsTeam targetTeam = teamOf(player.getUUID());
                if (targetTeam != node.team()
                    && targetTeam != FortsTeam.SPECTATOR
                    && player.position().distanceToSqr(center) <= 64.0 * 64.0) {
                    player.addEffect(new MobEffectInstance(
                        MobEffects.GLOWING,
                        30,
                        0,
                        false,
                        false,
                        true
                    ));
                }
            }
        }
    }

    private void updateGui(MinecraftServer server) {
        for (FortsTeam team : FortsTeam.playingTeams()) {
            int health = reactorHealth(team);
            int maximum = FortsConfig.REACTOR_HEALTH.get();
            ServerBossEvent bar = reactorBars.computeIfAbsent(
                team,
                ignored -> new ServerBossEvent(
                    Component.literal(team.id().toUpperCase() + " reactor"),
                    team == FortsTeam.RED
                        ? BossEvent.BossBarColor.RED
                        : BossEvent.BossBarColor.BLUE,
                    BossEvent.BossBarOverlay.PROGRESS
                )
            );
            bar.setProgress(Math.max(0.0F, Math.min(1.0F, health / (float)maximum)));
            TeamResourceBank teamBank = bank(team);
            bar.setName(Component.literal(
                team.id().toUpperCase()
                    + " | Reactor " + health + "/" + maximum
                    + (teamBank == null
                        ? ""
                        : " | M " + teamBank.metal()
                            + " E " + teamBank.energy()
                            + " A " + teamBank.ammo())
                    + " | " + Math.max(0, remainingTicks / 20) + "s"
            ));
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                bar.addPlayer(player);
            }
        }
    }

    private void discardProjectiles(MinecraftServer server) {
        for (ProjectileData data : projectiles.values()) {
            ServerLevel level = server.getLevel(data.dimension());
            Entity entity = level == null ? null : level.getEntity(data.id());
            if (entity != null) {
                entity.discard();
            }
        }
        projectiles.clear();
    }

    private void removeExpiredProjectileRecords() {
        projectiles.entrySet().removeIf(entry -> ticks > entry.getValue().expiresAt());
    }

    private static void configureScoreboard(
        MinecraftServer server,
        ServerPlayer player,
        FortsTeam team
    ) {
        if (team == FortsTeam.SPECTATOR) {
            removeFromMatchScoreboards(player);
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        String name = "forts_" + team.id();
        PlayerTeam scoreboardTeam = scoreboard.getPlayerTeam(name);
        if (scoreboardTeam == null) {
            scoreboardTeam = scoreboard.addPlayerTeam(name);
            scoreboardTeam.setPlayerPrefix(Component.literal(
                team == FortsTeam.RED ? "§c[RED] " : "§9[BLUE] "));
            scoreboardTeam.setColor(
                team == FortsTeam.RED ? ChatFormatting.RED : ChatFormatting.BLUE
            );
        }
        scoreboardTeam.setAllowFriendlyFire(FortsConfig.FRIENDLY_FIRE.get());
        scoreboard.addPlayerToTeam(player.getScoreboardName(), scoreboardTeam);
    }

    private static void removeFromMatchScoreboards(ServerPlayer player) {
        Scoreboard scoreboard = player.getScoreboard();
        PlayerTeam current = scoreboard.getPlayersTeam(player.getScoreboardName());
        if (current != null && current.getName().startsWith("forts_")) {
            scoreboard.removePlayerFromTeam(player.getScoreboardName(), current);
        }
    }

    private static void broadcast(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(Component.literal(message), false);
    }
}
