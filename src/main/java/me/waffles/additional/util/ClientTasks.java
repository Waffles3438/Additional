package me.waffles.additional.util;

import net.minecraft.client.Minecraft;
import net.minecraft.text.LiteralText;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Keeps network work off the client thread and chat updates on it. */
public final class ClientTasks {
    private static final Queue<String> MESSAGES = new ConcurrentLinkedQueue<>();
    private static final ExecutorService WORKERS = Executors.newFixedThreadPool(4, task -> {
        Thread thread = new Thread(task, "additional-stats");
        thread.setDaemon(true);
        return thread;
    });

    private ClientTasks() {}

    public static void runAsync(Runnable task) { WORKERS.execute(task); }
    public static void chat(String message) { MESSAGES.add(message); }

    public static void flushChat() {
        Minecraft client = Minecraft.getInstance();
        String message;
        while ((message = MESSAGES.poll()) != null) {
            if (client.player != null) client.player.addMessage(new LiteralText(message));
        }
    }
}
