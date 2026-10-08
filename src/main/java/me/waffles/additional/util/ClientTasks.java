package me.waffles.additional.util;

import net.minecraft.client.Minecraft;
import net.minecraft.text.LiteralText;
import org.polyfrost.oneconfig.utils.v1.Multithreading;

public final class ClientTasks {
    private ClientTasks() {}

    public static void runAsync(Runnable task) { Multithreading.submit(task); }

    public static void chat(String message) {
        Minecraft client = Minecraft.getInstance();
        client.executeTask(() -> {
            if (client.player != null) client.player.addMessage(new LiteralText(message));
            return null;
        });
    }

    /** Queue one complete reply so concurrent stat lookups cannot interleave lines. */
    public static void chatLines(String... messages) {
        Minecraft client = Minecraft.getInstance();
        client.executeTask(() -> {
            if (client.player != null) {
                for (String message : messages) {
                    if (message != null) client.player.addMessage(new LiteralText(message));
                }
            }
            return null;
        });
    }
}
