package me.waffles.additional;

import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import me.waffles.additional.render.NameTagESP;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import me.waffles.additional.command.BedwarsStatsCommand;
import me.waffles.additional.command.DuelsStatsCommand;
import me.waffles.additional.command.TabListPlayerNameArgumentParser;
import me.waffles.additional.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import me.waffles.additional.mixin.EntityLivingBaseAccessor;
import me.waffles.additional.playerData.Bedwars;
import me.waffles.additional.playerData.Duels;
import com.google.common.cache.Cache;
import me.waffles.additional.util.StatsCache;
import me.waffles.additional.playerData.PlayerProfile;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@Mod(modid = Additional.MODID, name = Additional.NAME, version = Additional.VERSION)
public class Additional {

    public static final String MODID = "@MOD_ID@";
    public static final String NAME = "@MOD_NAME@";
    public static final String VERSION = "@MOD_VERSION@";

    private int cacheCleanupTicks;

    public static ModConfig config;
    public static Cache<String, Duels> duelsStatsList;
    public static Cache<String, Bedwars> bedwarsStatsList;
    public static Cache<String, PlayerProfile> playerProfileList;

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new NameTagESP());
        config = new ModConfig();
        duelsStatsList = StatsCache.create();
        bedwarsStatsList = StatsCache.create();
        playerProfileList = StatsCache.create();
        CommandManager.INSTANCE.addParser(new TabListPlayerNameArgumentParser());
        CommandManager.INSTANCE.registerCommand(new BedwarsStatsCommand());
        CommandManager.INSTANCE.registerCommand(new DuelsStatsCommand());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        // Release expired entries even when no further stats commands are run.
        if (e.phase == TickEvent.Phase.END && ++cacheCleanupTicks >= 1200) {
            cacheCleanupTicks = 0;
            bedwarsStatsList.cleanUp();
            duelsStatsList.cleanUp();
            playerProfileList.cleanUp();
        }
        if (Minecraft.getMinecraft().thePlayer != null && ModConfig.ndj && e.phase.equals(TickEvent.Phase.START)) {
            if(((EntityLivingBaseAccessor) Minecraft.getMinecraft().thePlayer).getJumpTicks() > ModConfig.jumpTicks){
                ((EntityLivingBaseAccessor) Minecraft.getMinecraft().thePlayer).setJumpTicks(ModConfig.jumpTicks);
            }
        }
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        BotUtils.clearCache();
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
        // Hypixel is a BungeeCord network, so moving between lobbies and games swaps the
        // WorldClient without ever firing ClientDisconnectionFromServerEvent. Clearing only
        // on disconnect meant a classification made in one game was reused for the rest of
        // the session, and a player who had since left the tab list was never re-evaluated.
        BotUtils.clearCache();
    }
}
