package ru.denis.forts3d.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import ru.denis.forts3d.config.FortsConfig;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class FortsMatchManager {

    private static final Map<MinecraftServer, FortsMatchManager> INSTANCES =
        new ConcurrentHashMap<>();

    public static FortsMatchManager get(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(
            server,
            ignored -> new FortsMatchManager()
        );
    }

    public static void remove(MinecraftServer server) {
        INSTANCES.remove(server);
    }

    private MatchState state = MatchState.IDLE;
    private int remainingTicks;
    private long ticks;

    private final EnumMap<FortsTeam, TeamResourceBank> banks =
        new EnumMap<>(FortsTeam.class);

    private final Map<UUID, FortsTeam> players =
        new ConcurrentHashMap<>();

    private final EnumMap<FortsTeam, BlockPos> reactors =
        new EnumMap<>(FortsTeam.class);

    private final EnumMap<FortsTeam, Integer> reactorHealth =
        new EnumMap<>(FortsTeam.class);

    private final StructureRegistry structures =
        new StructureRegistry();

    private final EnumMap<FortsTeam, ServerBossEvent> reactorBars =
        new EnumMap<>(FortsTeam.class);

    private FortsMatchManager() {
    }

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

    public FortsTeam teamOf(UUID playerId) {
        return players.getOrDefault(playerId, FortsTeam.SPECTATOR);
    }

    public TeamResourceBank bank(FortsTeam team) {
        return banks.get(team);
    }

    public void create() {
        state = MatchState.PREPARING;
        ticks = 0;
        remainingTicks = 0;

        players.clear();
        reactors.clear();
        reactorHealth.clear();
        banks.clear();
        structures.clear();

        reactorBars.values().forEach(ServerBossEvent::removeAllPlayers);
        reactorBars.clear();

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
            return true;
        }

        long teamPlayerCount = players.values()
            .stream()
            .filter(currentTeam -> currentTeam == team)
            .count();

        if (teamPlayerCount >= FortsConfig.MAX_TEAM_SIZE.get()) {
            return false;
        }

        players.put(player.getUUID(), team);
        return true;
    }

    public void registerReactor(FortsTeam team, BlockPos position) {
        reactors.put(team, position.immutable());
        reactorHealth.put(team, FortsConfig.REACTOR_HEALTH.get());
    }

    public void damageReactor(
        MinecraftServer server,
        FortsTeam team,
        int damage
    ) {
        if (state != MatchState.RUNNING) {
            return;
        }

        int currentHealth = reactorHealth.getOrDefault(
            team,
            FortsConfig.REACTOR_HEALTH.get()
        );

        int newHealth = Math.max(0, currentHealth - Math.max(0, damage));
        reactorHealth.put(team, newHealth);

        updateGui(server);

        if (newHealth == 0) {
            FortsTeam winner = team == FortsTeam.RED
                ? FortsTeam.BLUE
                : FortsTeam.RED;

            win(server, winner);
        }
    }

    public int reactorHealth(FortsTeam team) {
        return reactorHealth.getOrDefault(team, 0);
    }

    public void start(MinecraftServer server) {
        if (state == MatchState.RUNNING) {
            broadcast(
                server,
                "§e[Forts 3D] Матч уже запущен"
            );
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
            "§6[Forts 3D] Матч начался: команды и стартовые крепости "
                + "созданы автоматически"
        );
    }

    public void stop(MinecraftServer server) {
        state = MatchState.FINISHED;

        reactorBars.values().forEach(ServerBossEvent::removeAllPlayers);

        broadcast(
            server,
            "§6[Forts 3D] Матч завершён"
        );
    }

    public void pause() {
        if (state == MatchState.RUNNING) {
            state = MatchState.PAUSED;
        } else if (state == MatchState.PAUSED) {
            state = MatchState.RUNNING;
        }
    }

    public void win(MinecraftServer server, FortsTeam team) {
        state = MatchState.FINISHED;

        reactorBars.values().forEach(ServerBossEvent::removeAllPlayers);

        broadcast(
            server,
            team.color() + "Команда " + team.id() + " победила!"
        );
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
        }

        int resourceTickRate = Math.max(
            1,
            FortsConfig.RESOURCE_TICK_RATE.get()
        );

        if (ticks % resourceTickRate == 0) {
            double multiplier = FortsConfig.RESOURCE_MULTIPLIER.get();
            long generatedEnergy = Math.round(5.0D * multiplier);

            for (TeamResourceBank bank : banks.values()) {
                bank.addEnergy(generatedEnergy);
            }
        }

        int structureTickRate = Math.max(
            1,
            FortsConfig.STRUCTURE_TICK_RATE.get()
        );

        if (
            FortsConfig.STRUCTURAL_COLLAPSE.get()
                && ticks % structureTickRate == 0
        ) {
            for (BlockPos position : structures.unsupported()) {
                var level = server.overworld();

                if (level.isLoaded(position)) {
                    level.destroyBlock(position, true);
                    structures.remove(position);
                }
            }
        }
    }

    private void broadcast(MinecraftServer server, String message) {
        server.getPlayerList().broadcastSystemMessage(
            Component.literal(message),
            false
        );
    }

    private void preparePlayersAndForts(MinecraftServer server) {
        var onlinePlayers = server.getPlayerList().getPlayers();

        int redPlayers = 0;
        int bluePlayers = 0;

        /*
         * Сначала учитываем игроков, которые уже находятся
         * в одной из команд.
         */
        for (ServerPlayer player : onlinePlayers) {
            FortsTeam currentTeam = teamOf(player.getUUID());

            if (currentTeam == FortsTeam.RED) {
                redPlayers++;
            } else if (currentTeam == FortsTeam.BLUE) {
                bluePlayers++;
            }
        }

        /*
         * Игроков без команды распределяем в команду,
         * в которой сейчас меньше участников.
         */
        for (ServerPlayer player : onlinePlayers) {
            FortsTeam currentTeam = teamOf(player.getUUID());

            if (currentTeam != FortsTeam.SPECTATOR) {
                continue;
            }

            if (redPlayers <= bluePlayers) {
                currentTeam = FortsTeam.RED;
                redPlayers++;
            } else {
                currentTeam = FortsTeam.BLUE;
                bluePlayers++;
            }

            players.put(player.getUUID(), currentTeam);
        }

        var level = server.overworld();
        BlockPos worldSpawn = level.getSharedSpawnPos();

        buildTeamFort(
            level,
            worldSpawn.offset(-40, 0, 0),
            FortsTeam.RED
        );

        buildTeamFort(
            level,
            worldSpawn.offset(40, 0, 0),
            FortsTeam.BLUE
        );

        /*
         * Телепортируем каждого игрока к реактору его команды.
         *
         * В Minecraft 1.21.1 эта перегрузка teleportTo принимает:
         * ServerLevel, x, y, z, yaw, pitch.
         *
         * Дополнительного boolean-параметра здесь нет.
         */
        for (ServerPlayer player : onlinePlayers) {
            FortsTeam team = teamOf(player.getUUID());
            BlockPos reactorPosition = reactors.get(team);

            if (reactorPosition == null) {
                continue;
            }

            float yaw = team == FortsTeam.RED
                ? -90.0F
                : 90.0F;

            player.teleportTo(
                level,
                reactorPosition.getX() + 0.5D,
                reactorPosition.getY() + 1.0D,
                reactorPosition.getZ() + 0.5D,
                yaw,
                0.0F
            );
        }
    }

    private void buildTeamFort(
        net.minecraft.server.level.ServerLevel level,
        BlockPos horizontalPosition,
        FortsTeam team
    ) {
        int surfaceY = level.getHeight(
            Heightmap.Types.WORLD_SURFACE,
            horizontalPosition.getX(),
            horizontalPosition.getZ()
        );

        BlockPos origin = new BlockPos(
            horizontalPosition.getX(),
            surfaceY,
            horizontalPosition.getZ()
        );

        BlockPos reactorPosition = StarterFortBuilder.build(
            level,
            origin,
            team,
            structures
        );

        registerReactor(team, reactorPosition);
    }

    private void updateGui(MinecraftServer server) {
        for (
            FortsTeam team :
            new FortsTeam[]{FortsTeam.RED, FortsTeam.BLUE}
        ) {
            int health = reactorHealth(team);
            int maximumHealth = Math.max(
                1,
                FortsConfig.REACTOR_HEALTH.get()
            );

            ServerBossEvent bossBar = reactorBars.computeIfAbsent(
                team,
                ignored -> new ServerBossEvent(
                    Component.literal(
                        team.id().toUpperCase() + " reactor"
                    ),
                    team == FortsTeam.RED
                        ? BossEvent.BossBarColor.RED
                        : BossEvent.BossBarColor.BLUE,
                    BossEvent.BossBarOverlay.PROGRESS
                )
            );

            float progress = health / (float) maximumHealth;
            progress = Math.max(0.0F, Math.min(1.0F, progress));

            bossBar.setProgress(progress);

            TeamResourceBank resourceBank = bank(team);

            String resourcesText = resourceBank == null
                ? ""
                : " | M " + resourceBank.metal()
                    + " E " + resourceBank.energy()
                    + " A " + resourceBank.ammo();

            bossBar.setName(
                Component.literal(
                    team.id().toUpperCase()
                        + " | Reactor "
                        + health
                        + "/"
                        + maximumHealth
                        + resourcesText
                )
            );

            for (
                ServerPlayer player :
                server.getPlayerList().getPlayers()
            ) {
                bossBar.addPlayer(player);
            }
        }
    }
}