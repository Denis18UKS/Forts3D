package ru.denis.forts3d.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.denis.forts3d.config.FortsConfig;
import ru.denis.forts3d.game.FortsMatchManager;
import ru.denis.forts3d.game.FortsTeam;
import ru.denis.forts3d.game.Technology;
import ru.denis.forts3d.registry.ModItems;

public final class FortsCommands {
    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(
            Commands.literal("forts")
                .then(Commands.literal("status")
                    .executes(context -> status(context.getSource())))
                .then(joinCommand())
                .then(kitCommand())
                .then(giveAdminCommand())
                .then(matchCommand())
                .then(resourceCommand())
                .then(technologyCommand())
                .then(configCommand())
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> joinCommand() {
        return Commands.literal("join")
            .then(Commands.argument("team", StringArgumentType.word())
                .suggests((context, builder) -> {
                    builder.suggest("red");
                    builder.suggest("blue");
                    builder.suggest("spectator");
                    return builder.buildFuture();
                })
                .executes(context -> {
                    String id = StringArgumentType.getString(context, "team");
                    if (!id.equalsIgnoreCase("red")
                        && !id.equalsIgnoreCase("blue")
                        && !id.equalsIgnoreCase("spectator")) {
                        context.getSource().sendFailure(Component.literal(
                            "Команда должна быть red, blue или spectator"));
                        return 0;
                    }
                    var player = context.getSource().getPlayerOrException();
                    FortsTeam team = FortsTeam.byId(id);
                    return FortsMatchManager.get(context.getSource().getServer())
                        .join(player, team) ? 1 : 0;
                }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> kitCommand() {
        return Commands.literal("kit")
            .executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var manager = FortsMatchManager.get(context.getSource().getServer());
                if (manager.bank(manager.teamOf(player.getUUID())) == null) {
                    context.getSource().sendFailure(Component.literal(
                        "Сначала вступите в команду"));
                    return 0;
                }
                give(player, new ItemStack(ModItems.BUILD_TOOL.get()));
                give(player, new ItemStack(ModItems.REPAIR_TOOL.get()));
                give(player, new ItemStack(ModItems.WOOD_BEAM.get(), 64));
                give(player, new ItemStack(ModItems.METAL_BEAM.get(), 32));
                give(player, new ItemStack(ModItems.ARMOR_PLATE.get(), 16));
                give(player, new ItemStack(ModItems.POWER_CABLE.get(), 32));
                give(player, new ItemStack(ModItems.BATTERY.get(), 4));
                give(player, new ItemStack(ModItems.RESEARCH_CONSOLE.get(), 1));
                give(player, new ItemStack(ModItems.MACHINE_GUN.get(), 1));
                context.getSource().sendSuccess(
                    () -> Component.literal("Стартовый комплект Forts выдан"),
                    false
                );
                return 1;
            });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> giveAdminCommand() {
        return Commands.literal("giveadmin")
            .requires(source -> source.hasPermission(2))
            .executes(context -> {
                give(
                    context.getSource().getPlayerOrException(),
                    ModItems.ADMIN_TABLET.toStack()
                );
                return 1;
            });
    }

    private static LiteralArgumentBuilder<CommandSourceStack> matchCommand() {
        return Commands.literal("match")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("create").executes(context -> {
                FortsMatchManager.get(context.getSource().getServer()).create();
                context.getSource().sendSuccess(
                    () -> Component.literal("Новый матч подготовлен"),
                    true
                );
                return 1;
            }))
            .then(Commands.literal("start").executes(context -> {
                var server = context.getSource().getServer();
                FortsMatchManager.get(server).start(server);
                return 1;
            }))
            .then(Commands.literal("pause").executes(context -> {
                var server = context.getSource().getServer();
                return FortsMatchManager.get(server).pause(server) ? 1 : 0;
            }))
            .then(Commands.literal("stop").executes(context -> {
                var server = context.getSource().getServer();
                FortsMatchManager.get(server).stop(server);
                return 1;
            }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> resourceCommand() {
        return Commands.literal("resource")
            .requires(source -> source.hasPermission(2))
            .then(Commands.argument("team", StringArgumentType.word())
                .suggests((context, builder) -> {
                    builder.suggest("red");
                    builder.suggest("blue");
                    return builder.buildFuture();
                })
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        builder.suggest("metal");
                        builder.suggest("energy");
                        builder.suggest("ammo");
                        return builder.buildFuture();
                    })
                    .then(Commands.argument(
                        "amount",
                        LongArgumentType.longArg(-1_000_000, 1_000_000)
                    ).executes(context -> {
                        var manager = FortsMatchManager.get(
                            context.getSource().getServer());
                        FortsTeam team = FortsTeam.byId(
                            StringArgumentType.getString(context, "team"));
                        var bank = manager.bank(team);
                        if (bank == null) {
                            return 0;
                        }

                        long value = LongArgumentType.getLong(context, "amount");
                        switch (StringArgumentType.getString(context, "type")) {
                            case "metal" -> bank.addMetal(value);
                            case "energy" -> bank.addEnergy(value);
                            case "ammo" -> bank.addAmmo(value);
                            default -> {
                                return 0;
                            }
                        }
                        return 1;
                    }))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> technologyCommand() {
        return Commands.literal("tech")
            .then(Commands.literal("list").executes(context -> {
                var player = context.getSource().getPlayerOrException();
                var manager = FortsMatchManager.get(context.getSource().getServer());
                var bank = manager.bank(manager.teamOf(player.getUUID()));
                if (bank == null) {
                    return 0;
                }
                context.getSource().sendSuccess(
                    () -> Component.literal("Технологии: " + bank.technologies()),
                    false
                );
                return 1;
            }))
            .then(Commands.literal("unlock")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("team", StringArgumentType.word())
                    .then(Commands.argument("technology", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            for (Technology technology : Technology.values()) {
                                builder.suggest(technology.name().toLowerCase());
                            }
                            return builder.buildFuture();
                        })
                        .executes(context -> unlockTechnology(context.getSource(), context)))));
    }

    private static int unlockTechnology(
        CommandSourceStack source,
        com.mojang.brigadier.context.CommandContext<CommandSourceStack> context
    ) {
        var manager = FortsMatchManager.get(source.getServer());
        FortsTeam team = FortsTeam.byId(
            StringArgumentType.getString(context, "team"));
        var bank = manager.bank(team);
        if (bank == null) {
            return 0;
        }
        try {
            Technology technology = Technology.valueOf(
                StringArgumentType.getString(context, "technology").toUpperCase());
            return bank.unlock(technology) ? 1 : 0;
        } catch (IllegalArgumentException exception) {
            return 0;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> configCommand() {
        return Commands.literal("config")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("list").executes(context -> {
                context.getSource().sendSuccess(() -> Component.literal(
                    "collapse=" + FortsConfig.STRUCTURAL_COLLAPSE.get()
                        + ", worldDamage=" + FortsConfig.WORLD_DAMAGE.get()
                        + ", friendlyFire=" + FortsConfig.FRIENDLY_FIRE.get()
                        + ", weatherBallistics=" + FortsConfig.WEATHER_BALLISTICS.get()
                        + ", requireTech=" + FortsConfig.REQUIRE_TECH.get()
                        + ", maxProjectiles=" + FortsConfig.MAX_PROJECTILES.get()
                        + ", buildRadius=" + FortsConfig.BUILD_RADIUS.get()
                ), false);
                return 1;
            }))
            .then(booleanConfigCommand(
                "collapse",
                FortsConfig.STRUCTURAL_COLLAPSE
            ))
            .then(booleanConfigCommand(
                "worldDamage",
                FortsConfig.WORLD_DAMAGE
            ))
            .then(booleanConfigCommand(
                "friendlyFire",
                FortsConfig.FRIENDLY_FIRE
            ))
            .then(booleanConfigCommand(
                "weatherBallistics",
                FortsConfig.WEATHER_BALLISTICS
            ))
            .then(booleanConfigCommand(
                "requireTech",
                FortsConfig.REQUIRE_TECH
            ))
            .then(booleanConfigCommand(
                "limitedBuildZone",
                FortsConfig.LIMITED_BUILD_ZONE
            ))
            .then(Commands.literal("maxProjectiles")
                .then(Commands.argument(
                    "value",
                    IntegerArgumentType.integer(16, 8192)
                ).executes(context -> {
                    FortsConfig.MAX_PROJECTILES.set(
                        IntegerArgumentType.getInteger(context, "value"));
                    return 1;
                })))
            .then(Commands.literal("buildRadius")
                .then(Commands.argument(
                    "value",
                    IntegerArgumentType.integer(16, 1024)
                ).executes(context -> {
                    FortsConfig.BUILD_RADIUS.set(
                        IntegerArgumentType.getInteger(context, "value"));
                    return 1;
                })));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> booleanConfigCommand(
        String name,
        ModConfigSpec.BooleanValue configValue
    ) {
        return Commands.literal(name)
            .then(Commands.argument("value", BoolArgumentType.bool())
                .executes(context -> {
                    configValue.set(BoolArgumentType.getBool(context, "value"));
                    return 1;
                }));
    }

    private static int status(CommandSourceStack source) {
        var manager = FortsMatchManager.get(source.getServer());
        source.sendSuccess(() -> Component.literal(
            "Forts 3D: " + manager.state()
                + ", " + manager.remainingTicks() / 20 + "s"
                + ", projectiles=" + manager.activeProjectileCount()
                + ", structures=" + manager.structures().nodes().size()
        ), false);
        for (FortsTeam team : FortsTeam.playingTeams()) {
            var bank = manager.bank(team);
            if (bank != null) {
                source.sendSuccess(() -> Component.literal(
                    team.color() + team.id()
                        + " M=" + bank.metal()
                        + " E=" + bank.energy() + "/" + bank.capacity()
                        + " A=" + bank.ammo() + "/" + bank.ammoCapacity()
                        + " R=" + manager.reactorHealth(team)
                ), false);
            }
        }
        return 1;
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private FortsCommands() {}
}
