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
}
