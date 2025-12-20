package me.waffles.addition;

import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import me.waffles.addition.command.BedwarsStatsCommand;
import me.waffles.addition.command.DuelsStatsCommand;
import me.waffles.addition.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import me.waffles.addition.mixin.EntityLivingBaseAccessor;
import me.waffles.addition.util.Bedwars;
import me.waffles.addition.util.Duels;
import me.waffles.addition.util.EldestRemovalMap;
import me.waffles.addition.util.PlayerProfile;

@Mod(modid = Addition.MODID, name = Addition.NAME, version = Addition.VERSION)
public class Addition {

    public static final String MODID = "@ID@";
    public static final String NAME = "@NAME@";
    public static final String VERSION = "@VER@";
    public static ModConfig config;
    public static int maxSize = 16;
    public static EldestRemovalMap<String, Duels> duelsStatsList = new EldestRemovalMap<>(maxSize);
    public static EldestRemovalMap<String, Bedwars> bedwarsStatsList = new EldestRemovalMap<>(maxSize);
    public static EldestRemovalMap<String, PlayerProfile> playerProfileList = new EldestRemovalMap<>(maxSize);
    public static EldestRemovalMap<String, String> properPlayerNames = new EldestRemovalMap<>(maxSize);

    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);
        config = new ModConfig();
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
}
