package me.waffles.additional.api;

import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class MojangAPIUtilsTest {
    private static final String RESPONSE = "{\"id\":\"069a79f444e94726a5befca90e38aaf5\"}";
    private static final String UUID = "069a79f4-44e9-4726-a5be-fca90e38aaf5";

    @Before public void reset() { MojangAPIUtils.clearCache(); }

    @Test public void repeatedNamesIgnoreCaseAndSkipHttp() {
        AtomicInteger requests = new AtomicInteger();
        assertEquals(UUID, MojangAPIUtils.fetchUuid("Notch", name -> {
            requests.incrementAndGet(); return RESPONSE;
        }, 0));
        assertEquals(UUID, MojangAPIUtils.fetchUuid("nOtCh", name -> {
            requests.incrementAndGet(); return null;
        }, 1));
        assertEquals(1, requests.get());
    }

    @Test public void expiredEntriesAreRefetched() {
        MojangAPIUtils.fetchUuid("Notch", name -> RESPONSE, 0);
        AtomicInteger requests = new AtomicInteger();
        assertEquals(UUID, MojangAPIUtils.fetchUuid("Notch", name -> {
            requests.incrementAndGet(); return RESPONSE;
        }, MojangAPIUtils.CACHE_TTL_NANOS));
        assertEquals(1, requests.get());
    }

    @Test public void failuresDoNotPoisonCacheAndInvalidNamesDoNotRequest() {
        assertNull(MojangAPIUtils.fetchUuid("Notch", name -> "{\"id\":\"invalid\"}", 0));
        assertEquals(UUID, MojangAPIUtils.fetchUuid("Notch", name -> RESPONSE, 1));
        assertNull(MojangAPIUtils.fetchUuid("../invalid", name -> {
            fail("Invalid usernames must not reach HTTP"); return null;
        }, 0));
    }

    @Test public void clearingCacheForcesNewLookup() {
        MojangAPIUtils.fetchUuid("Notch", name -> RESPONSE, 0);
        MojangAPIUtils.clearCache();
        assertNull(MojangAPIUtils.fetchUuid("Notch", name -> null, 1));
    }
}
