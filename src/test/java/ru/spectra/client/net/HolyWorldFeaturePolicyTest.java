package ru.spectra.client.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class HolyWorldFeaturePolicyTest {
    private final HolyWorldFeaturePolicy policy = new HolyWorldFeaturePolicy();
    private final Object connection = new Object();

    private String begin() {
        return JsonParser.parseString(policy.begin(connection,
                List.of("autoeat", "zoom", "fullbright"))).getAsJsonObject().get("id").getAsString();
    }

    private static String response(String id, String blocklist) {
        return "{\"id\":\"" + id + "\",\"ok\":true,\"payload\":{\"blocklist\":" + blocklist + "}}";
    }

    @Test
    void requestUsesDocumentedMethodStableClientAndCompleteFeatureList() {
        JsonObject request = JsonParser.parseString(policy.begin(connection,
                List.of("autoeat", "zoom", "fullbright"))).getAsJsonObject();
        assertEquals("checkFeatures", request.get("method").getAsString());
        assertEquals("spectra", request.getAsJsonObject("payload").get("client").getAsString());
        assertEquals(3, request.getAsJsonObject("payload").getAsJsonArray("features").size());
        assertNotNull(java.util.UUID.fromString(request.get("id").getAsString()));
    }

    @Test
    void noFeatureCanRunUntilAValidAnswerArrives() {
        assertFalse(policy.allows("zoom"));
        begin();
        assertFalse(policy.allows("zoom"));
        assertFalse(policy.isVerified());
    }

    @Test
    void blockedFeatureIsClosedAndAllowedFeatureOpens() {
        String id = begin();
        assertTrue(policy.receive(connection, response(id, "[\"autoeat\"]")));
        assertTrue(policy.isVerified());
        assertFalse(policy.allows("autoeat"));
        assertTrue(policy.allows("zoom"));
        assertTrue(policy.allows("fullbright"));
        assertFalse(policy.allows("unsubmitted"));
    }

    @Test
    void validEmptyBlocklistIsNotTreatedAsAnError() {
        String id = begin();
        assertTrue(policy.receive(connection, response(id, "[]")));
        assertTrue(policy.allows("zoom"));
    }

    @Test
    void wrongRequestIdCannotUnlockFeaturesOrConsumeTheRealResponse() {
        String id = begin();
        assertFalse(policy.receive(connection, response("wrong-id", "[]")));
        assertFalse(policy.allows("zoom"));
        assertTrue(policy.receive(connection, response(id, "[]")));
        assertTrue(policy.allows("zoom"));
    }

    @Test
    void answerFromOldConnectionCannotUnlockNewConnectionEvenWithMatchingUuid() {
        String id = begin();
        assertFalse(policy.receive(new Object(), response(id, "[]")));
        assertFalse(policy.allows("zoom"));
        assertTrue(policy.receive(connection, response(id, "[]")));
    }

    @Test
    void aPreviousRequestCannotBeReplayedAfterReconnect() {
        String oldId = begin();
        policy.disconnect();
        String newId = begin();
        assertNotEquals(oldId, newId);
        assertFalse(policy.receive(connection, response(oldId, "[]")));
        assertFalse(policy.allows("zoom"));
        assertTrue(policy.receive(connection, response(newId, "[\"zoom\"]")));
        assertFalse(policy.allows("zoom"));
    }

    @Test
    void duplicateResponseCannotChangeAnAlreadyAppliedBlocklist() {
        String id = begin();
        policy.receive(connection, response(id, "[\"autoeat\"]"));
        assertFalse(policy.receive(connection, response(id, "[]")));
        assertFalse(policy.allows("autoeat"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "\"zoom\"", "[null]", "[1]", "[true]",
            "[\"unsubmitted\"]", "[\"zoom\",\"zoom\"]"})
    void malformedOrUnsubmittedBlocklistNeverUnlocksFeatures(String blocklist) {
        String id = begin();
        policy.receive(connection, response(id, blocklist));
        assertFalse(policy.isVerified());
        assertFalse(policy.allows("zoom"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "\"true\"", "1", "null"})
    void successMustBeABooleanTrue(String ok) {
        String id = begin();
        policy.receive(connection, "{\"id\":\"" + id + "\",\"ok\":" + ok
                + ",\"payload\":{\"blocklist\":[]}}");
        assertFalse(policy.allows("zoom"));
    }

    @Test
    void timeoutAndApiErrorDoNotGrantAccess() {
        String id = begin();
        policy.fail(connection);
        assertFalse(policy.receive(connection, response(id, "[]")));
        assertFalse(policy.allows("zoom"));
        id = begin();
        policy.receive(connection, "{\"id\":\"" + id + "\",\"ok\":false,\"error\":\"RATE_LIMITED\"}");
        assertFalse(policy.allows("zoom"));
    }

    @Test
    void oversizedAndUnrelatedPushMessagesCannotUnlockAnything() {
        begin();
        assertFalse(policy.receive(connection, " ".repeat(16_385)));
        assertFalse(policy.receive(connection, "{\"event\":\"checkFeatures\",\"payload\":{\"blocklist\":[]}}"));
        assertFalse(policy.receive(connection, "{"));
        assertFalse(policy.allows("zoom"));
    }

    @Test
    void holyWorldHostMatchingCannotBeTriggeredBySubstringSpoofing() {
        assertTrue(HolyWorldFeaturePolicy.isHolyWorldHost("mc.holyworld.ru:25565"));
        assertTrue(HolyWorldFeaturePolicy.isHolyWorldHost("holyworld.me."));
        assertTrue(HolyWorldFeaturePolicy.isHolyWorldHost(" PLAY.HOLYWORLD.RU "));
        assertFalse(HolyWorldFeaturePolicy.isHolyWorldHost("holyworld.ru.evil.example"));
        assertFalse(HolyWorldFeaturePolicy.isHolyWorldHost("fakeholyworld.ru"));
        assertFalse(HolyWorldFeaturePolicy.isHolyWorldHost(null));
    }

    @Test
    void featureIdsMatchCommonApiSpellingsAndDoNotDependOnObfuscatedClassNames() {
        assertEquals("fullbright", HolyWorldFeaturePolicy.featureId("Full Bright"));
        assertEquals("autoeat", HolyWorldFeaturePolicy.featureId("Auto Eat"));
        assertEquals("hwhelper", HolyWorldFeaturePolicy.featureId("HW Helper"));
        assertThrows(IllegalArgumentException.class, () -> HolyWorldFeaturePolicy.featureId("?!"));
    }

    @Test
    void emptyDuplicateAndInvalidRequestsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> policy.begin(connection, List.of()));
        assertThrows(IllegalArgumentException.class, () -> policy.begin(connection, List.of("zoom", "zoom")));
        assertThrows(IllegalArgumentException.class, () -> policy.begin(connection, List.of("bad id")));
    }
}
