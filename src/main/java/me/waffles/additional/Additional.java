package me.waffles.additional;

import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import me.waffles.additional.util.BotUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import me.waffles.additional.command.BedwarsStatsCommand;
import me.waffles.additional.command.DuelsStatsCommand;
import me.waffles.additional.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import me.waffles.additional.mixin.EntityLivingBaseAccessor;
import me.waffles.additional.playerData.Bedwars;
import me.waffles.additional.playerData.Duels;
import me.waffles.additional.util.EldestRemovalMap;
import me.waffles.additional.playerData.PlayerProfile;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@Mod(modid = Additional.MODID, name = Additional.NAME, version = Additional.VERSION)
public class Additional {

    public static final String MODID = "@MOD_ID@";
    public static final String NAME = "@MOD_NAME@";
    public static final String VERSION = "@MOD_VERSION@";

    public static ModConfig config;
    public static EldestRemovalMap<String, Duels> duelsStatsList;
    public static EldestRemovalMap<String, Bedwars> bedwarsStatsList;
    public static EldestRemovalMap<String, PlayerProfile> playerProfileList;

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        config = new ModConfig();
        duelsStatsList = new EldestRemovalMap<>(ModConfig.maxCacheSize);
        bedwarsStatsList = new EldestRemovalMap<>(ModConfig.maxCacheSize);
        playerProfileList = new EldestRemovalMap<>(ModConfig.maxCacheSize);
        CommandManager.INSTANCE.registerCommand(new BedwarsStatsCommand());
        CommandManager.INSTANCE.registerCommand(new DuelsStatsCommand());
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
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
}
