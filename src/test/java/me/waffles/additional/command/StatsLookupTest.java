package me.waffles.additional.command;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.waffles.additional.Additional;
import me.waffles.additional.api.StatsProviderUtils;
import me.waffles.additional.playerData.PlayerProfile;
import me.waffles.additional.util.StatsCache;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class StatsLookupTest {
    private static final JsonObject PLAYER = JsonParser.parseString(
            "{\"player\":{\"displayname\":\"Player\",\"newPackageRank\":\"VIP\","
                    + "\"achievements\":{\"bedwars_level\":12},\"stats\":{"
                    + "\"Bedwars\":{\"final_kills_bedwars\":10,\"final_deaths_bedwars\":2},"
                    + "\"Duels\":{\"kills\":5,\"deaths\":2,\"wins\":12,\"losses\":3}}}}").getAsJsonObject();

    @Before
    public void setUp() {
        Additional.bedwarsStatsList = StatsCache.create();
        Additional.duelsStatsList = StatsCache.create();
        Additional.playerProfileList = StatsCache.create();
    }

    @Test
    public void oneResponseCachesTheProfileAndBothModes() {
        JsonObject guild = JsonParser.parseString("{\"tag\":\"TAG\",\"tagColor\":\"GOLD\"}").getAsJsonObject();

        StatsLookup.Snapshot snapshot = StatsLookup.store(
                "player", StatsProviderUtils.captureCacheGeneration(), PLAYER, guild, null);

        assertNotNull(snapshot);
        assertEquals(12, Additional.bedwarsStatsList.getIfPresent("player").getBedwarsStar());
        // /d can print from this /bw lookup without another request.
        assertEquals(2.5, Additional.duelsStatsList.getIfPresent("player").getDuelsKDR(), 0.0);
        assertEquals("§6[TAG]", Additional.playerProfileList.getIfPresent("player").getGuildTag());
        assertSame(Additional.duelsStatsList.getIfPresent("player"), snapshot.duels());
    }

    @Test
    public void currentProfileIsKeptWhenOnlyStatsWereMissing() {
        PlayerProfile cached = new PlayerProfile("Player", "§7", "");

        StatsLookup.Snapshot snapshot = StatsLookup.store(
                "player", StatsProviderUtils.captureCacheGeneration(), PLAYER, null, cached);

        assertNotNull(snapshot);
        assertSame(cached, snapshot.profile());
        assertNull(Additional.playerProfileList.getIfPresent("player"));
        assertNotNull(Additional.bedwarsStatsList.getIfPresent("player"));
    }

    @Test
    public void responsesFromBeforeACacheClearAreDropped() {
        long generation = StatsProviderUtils.captureCacheGeneration();
        StatsProviderUtils.invalidateCacheGeneration();

        assertNull(StatsLookup.store("player", generation, PLAYER, new JsonObject(), null));
        assertNull(Additional.bedwarsStatsList.getIfPresent("player"));
        assertNull(Additional.duelsStatsList.getIfPresent("player"));
        assertNull(Additional.playerProfileList.getIfPresent("player"));
    }
}
