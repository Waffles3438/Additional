package org.polyfrost.example;

import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import cc.polyfrost.oneconfig.utils.hypixel.LocrawUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.commons.lang3.ArrayUtils;
import org.polyfrost.example.command.BedwarsStatsCommand;
import org.polyfrost.example.command.DuelsStatsCommand;
import org.polyfrost.example.config.ModConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.polyfrost.example.mixin.EntityLivingBaseAccessor;
import org.polyfrost.example.util.Bedwars;
import org.polyfrost.example.util.Duels;
import org.polyfrost.example.util.EldestRemovalMap;
import org.polyfrost.example.util.Ranks;

import java.util.HashMap;

@Mod(modid = Addition.MODID, name = Addition.NAME, version = Addition.VERSION)
public class Addition {

    public static final String MODID = "@ID@";
    public static final String NAME = "@NAME@";
    public static final String VERSION = "@VER@";
    public static ModConfig config;
    public static int maxSize = 16;
    public static EldestRemovalMap<String, Duels> duelsStatsList = new EldestRemovalMap<>(maxSize);
    public static EldestRemovalMap<String, Bedwars> bedwarsStatsList = new EldestRemovalMap<>(maxSize);
    public static EldestRemovalMap<String, Ranks> playerRanks = new EldestRemovalMap<>(maxSize);
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
        if (Minecraft.getMinecraft().thePlayer != null && ModConfig.ndj && e.phase.equals(TickEvent.Phase.START) && ((EntityLivingBaseAccessor) Minecraft.getMinecraft().thePlayer).getJumpTicks() > ModConfig.jumpTicks) {
            ((EntityLivingBaseAccessor) Minecraft.getMinecraft().thePlayer).setJumpTicks(ModConfig.jumpTicks);
        }
    }

    public static final String[] games = {"BEDWARS_FOUR_FOUR", "BEDWARS_EIGHT_TWO", "BEDWARS_FOUR_THREE", "BEDWARS_FOUR_FOUR", "BEDWARS_FOUR_FOUR"};

    @SubscribeEvent
    public void onRecieve(ClientChatReceivedEvent e) {
        if (!HypixelUtils.INSTANCE.isHypixel() || !LocrawUtil.INSTANCE.isInGame()) return;
        if (ArrayUtils.contains(games, LocrawUtil.INSTANCE.getLocrawInfo()) && e.toString().contains("Protect your bed and destroy the enemy beds.") && ModConfig.guildSniper) {
            StringBuilder result = new StringBuilder();
            for(String uuid : ModConfig.guildMembers) {
                if(getAllPlayersFromTabList().containsKey(uuid)) {
                    result.append(getAllPlayersFromTabList().get(uuid)).append(", ");
                }
            }

            if (result.toString().endsWith(", ")) {
                result = new StringBuilder(result.substring(0, result.length() - 2) + " are from " + ModConfig.guildName);
            }
            UChat.chat(result.toString());
        }
    }

    public HashMap<String, String> getAllPlayersFromTabList() {
        HashMap<String, String> playerList = new HashMap<>();
        NetHandlerPlayClient netHandler = Minecraft.getMinecraft().getNetHandler();
        if (netHandler != null) {
            for (NetworkPlayerInfo info : netHandler.getPlayerInfoMap()) {
                playerList.put(info.getGameProfile().getId().toString(), info.getGameProfile().getName());
            }
        }
        return playerList;
    }
}
