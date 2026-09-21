package me.waffles.additional.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;

import java.util.function.Consumer;

public final class StatsCommands {
    private StatsCommands() {}

    public static void register() {
        BedwarsStatsCommand bedwars = new BedwarsStatsCommand();
        DuelsStatsCommand duels = new DuelsStatsCommand();
        register("bw", bedwars::execute, bedwars::execute);
        register("d", duels::execute, duels::execute);
    }

    private static void register(String name, Runnable self, Consumer<String> player) {
        CommandManager.INSTANCE.register(CommandManager.literal(name)
                .executes(context -> { self.run(); return 1; })
                .then(CommandManager.INSTANCE.argument("player", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            TabListPlayerNameArgumentParser.complete(builder.getRemaining())
                                    .forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            player.accept(StringArgumentType.getString(context, "player"));
                            return 1;
                        })));
    }
}
