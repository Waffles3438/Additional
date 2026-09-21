package me.waffles.additional.command;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Leaves the typed value untouched so any username can be looked up through the
 * Mojang API, while offering tab-list names as completion suggestions.
 */
public class TabListPlayerNameArgumentParser {
    public static List<String> complete(String current) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPlayNetworkHandler netHandler = minecraft.getNetworkHandler();
        if (minecraft.world == null || netHandler == null) {
            return Collections.emptyList();
        }

        String prefix = current == null ? "" : current.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (PlayerInfo playerInfo : netHandler.getOnlinePlayers()) {
            GameProfile profile = playerInfo.getProfile();
            if (profile == null || profile.getName() == null) {
                continue;
            }
            String name = profile.getName();
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                matches.add(name);
            }
        }
        return matches;
    }
}
