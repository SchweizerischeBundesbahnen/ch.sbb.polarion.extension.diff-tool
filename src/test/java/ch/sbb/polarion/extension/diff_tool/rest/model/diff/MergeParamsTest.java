package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeParamsTest {

    @Test
    void testBuilderCreatesInstance() {
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        List<MergeWorkItemsPair> pairs = List.of(pair);

        MergeParams params = MergeParams.builder()
                .mergeDirection(MergeDirection.LEFT_TO_RIGHT)
                .linkRole("testRole")
                .configName("customConfig")
                .configCacheBucketId("bucket123")
                .pairs(pairs)
                .build();

        assertEquals(MergeDirection.LEFT_TO_RIGHT, params.getMergeDirection());
        assertEquals("testRole", params.getLinkRole());
        assertEquals("customConfig", params.getConfigName());
        assertEquals("bucket123", params.getConfigCacheBucketId());
        assertEquals(pairs, params.getPairs());
    }

    @Test
    void testNoArgsConstructor() {
        MergeParams params = new MergeParams();

        assertNull(params.getMergeDirection());
        assertNull(params.getLinkRole());
        assertNull(params.getConfigCacheBucketId());
        assertNull(params.getPairs());
    }

    @Test
    void testAllArgsConstructor() {
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        List<MergeWorkItemsPair> pairs = List.of(pair);

        MergeParams params = new MergeParams(
                MergeDirection.RIGHT_TO_LEFT,
                "role",
                LinkRoleDirection.DIRECT,
                "config",
                "bucketId",
                false,
                pairs
        );

        assertEquals(MergeDirection.RIGHT_TO_LEFT, params.getMergeDirection());
        assertEquals("role", params.getLinkRole());
        assertEquals("config", params.getConfigName());
        assertEquals("bucketId", params.getConfigCacheBucketId());
        assertEquals(pairs, params.getPairs());
    }

    @Test
    void testGetConfigNameReturnsDefaultWhenNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MergeParams params = mapper.readValue("{\"configName\": null}", MergeParams.class);

        assertEquals(NamedSettings.DEFAULT_NAME, params.getConfigName());
    }

    @Test
    void testGetConfigNameReturnsDefaultWhenMissing() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MergeParams params = mapper.readValue("{}", MergeParams.class);

        assertEquals(NamedSettings.DEFAULT_NAME, params.getConfigName());
    }

    @Test
    void testGetConfigNameReturnsSetValueWhenNotNull() {
        MergeParams params = new MergeParams();
        params.setConfigName("myConfig");

        assertEquals("myConfig", params.getConfigName());
    }

    @Test
    void testGetLinkRoleDirectionReturnsDefaultWhenNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MergeParams params = mapper.readValue("{\"linkRoleDirection\": null}", MergeParams.class);

        assertEquals(LinkRoleDirection.DIRECT, params.getLinkRoleDirection());
    }

    @Test
    void testGetLinkRoleDirectionReturnsDefaultWhenMissing() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        MergeParams params = mapper.readValue("{}", MergeParams.class);

        assertEquals(LinkRoleDirection.DIRECT, params.getLinkRoleDirection());
    }

    @Test
    void testSettersAndGetters() {
        MergeParams params = new MergeParams();
        MergeWorkItemsPair pair = new MergeWorkItemsPair();
        List<MergeWorkItemsPair> pairs = List.of(pair);

        params.setMergeDirection(MergeDirection.LEFT_TO_RIGHT);
        params.setLinkRole("newRole");
        params.setConfigName("newConfig");
        params.setConfigCacheBucketId("newBucket");
        params.setPairs(pairs);

        assertEquals(MergeDirection.LEFT_TO_RIGHT, params.getMergeDirection());
        assertEquals("newRole", params.getLinkRole());
        assertEquals("newConfig", params.getConfigName());
        assertEquals("newBucket", params.getConfigCacheBucketId());
        assertEquals(pairs, params.getPairs());
    }

    @Test
    void testEqualsAndHashCode() {
        List<MergeWorkItemsPair> pairs = List.of(new MergeWorkItemsPair());

        MergeParams params1 = MergeParams.builder()
                .mergeDirection(MergeDirection.LEFT_TO_RIGHT)
                .linkRole("role")
                .configName("config")
                .configCacheBucketId("bucket")
                .pairs(pairs)
                .build();

        MergeParams params2 = MergeParams.builder()
                .mergeDirection(MergeDirection.LEFT_TO_RIGHT)
                .linkRole("role")
                .configName("config")
                .configCacheBucketId("bucket")
                .pairs(pairs)
                .build();

        assertEquals(params1, params2);
        assertEquals(params1.hashCode(), params2.hashCode());
    }

    @Test
    void testToString() {
        MergeParams params = MergeParams.builder()
                .mergeDirection(MergeDirection.LEFT_TO_RIGHT)
                .linkRole("role")
                .linkRoleDirection(LinkRoleDirection.REVERSE)
                .build();

        String toString = params.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("MergeParams"));
        assertTrue(toString.contains("mergeDirection=LEFT_TO_RIGHT"));
        assertTrue(toString.contains("linkRole=role"));
        assertTrue(toString.contains("linkRoleDirection=REVERSE"));
    }
}
