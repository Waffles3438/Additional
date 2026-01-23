package me.waffles.additional;

import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import me.waffles.additional.command.BedwarsStatsCommand;
import me.waffles.additional.command.DuelsStatsCommand;
import me.waffles.additional.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import me.waffles.additional.mixin.EntityLivingBaseAccessor;
import me.waffles.additional.util.Bedwars;
import me.waffles.additional.util.Duels;
import me.waffles.additional.util.EldestRemovalMap;
import me.waffles.additional.util.PlayerProfile;

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
