package me.waffles.additional.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.PlayerInfo;
import org.polyfrost.oneconfig.api.commands.v1.CommandManager;

import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class StatsCommands {
    private StatsCommands() {}

    public static void register() {
        BedwarsStatsCommand bedwars = new BedwarsStatsCommand();
        DuelsStatsCommand duels = new DuelsStatsCommand();
        register("bw", bedwars::execute, bedwars::execute);
        register("d", duels::execute, duels::execute);
    }

    private static void register(String name, Runnable self, Consumer<String> lookup) {
        CommandManager.INSTANCE.register(CommandManager.literal(name)
                .executes(context -> { self.run(); return 1; })
                .then(CommandManager.INSTANCE.argument("username", StringArgumentType.word())
                        .suggests((context, builder) -> suggestPlayers(builder))
                        .executes(context -> {
                            lookup.accept(StringArgumentType.getString(context, "username"));
                            return 1;
                        })));
    }

    private static CompletableFuture<Suggestions> suggestPlayers(SuggestionsBuilder builder) {
        Minecraft client = Minecraft.getInstance();
        if (client.world != null && client.getNetworkHandler() != null) {
            String prefix = builder.getRemaining().toLowerCase(Locale.ROOT);
            for (PlayerInfo info : client.getNetworkHandler().getOnlinePlayers()) {
                String name = info.getProfile().getName();
                if (name != null && name.toLowerCase(Locale.ROOT).startsWith(prefix)) builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }
}
