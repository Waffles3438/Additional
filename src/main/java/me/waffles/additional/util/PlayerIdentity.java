package me.waffles.additional.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.PlayerInfo;
import net.minecraft.client.network.handler.ClientPlayNetworkHandler;

public final class PlayerIdentity {
    private PlayerIdentity() {}

    /** Called by commands on the client thread; never retain the live player list. */
    public static String findOnlineUuid(String username) {
        if (username == null) return null;
        Minecraft client = Minecraft.getInstance();
        ClientPlayNetworkHandler network = client.getNetworkHandler();
        if (client.world == null || network == null) return null;
        for (PlayerInfo info : network.getOnlinePlayers()) {
            String uuid = matchingUuid(username, info.getProfile());
            if (uuid != null) return uuid;
        }
        return null;
    }

    static String matchingUuid(String username, GameProfile profile) {
        // Offline/generated profiles cannot identify a Mojang account reliably.
        if (username == null || profile == null || profile.getId() == null
                || profile.getId().version() != 4 || !username.equalsIgnoreCase(profile.getName())) return null;
        return profile.getId().toString();
    }
}
