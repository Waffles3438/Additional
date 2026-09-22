package me.waffles.additional.util;

import com.mojang.authlib.GameProfile;
import org.junit.Test;
import java.util.UUID;
import static org.junit.Assert.*;

public class PlayerIdentityTest {
    @Test public void matchesOnlineProfileCaseInsensitively() {
        UUID id = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        assertEquals(id.toString(), PlayerIdentity.matchingUuid("nOtCh", new GameProfile(id, "Notch")));
        assertNull(PlayerIdentity.matchingUuid("Other", new GameProfile(id, "Notch")));
    }

    @Test public void rejectsOfflineAndIncompleteProfiles() {
        UUID offline = UUID.nameUUIDFromBytes("OfflinePlayer:Notch".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertNull(PlayerIdentity.matchingUuid("Notch", new GameProfile(offline, "Notch")));
        assertNull(PlayerIdentity.matchingUuid("Notch", new GameProfile(null, "Notch")));
        assertNull(PlayerIdentity.matchingUuid("Notch", null));
    }
}
