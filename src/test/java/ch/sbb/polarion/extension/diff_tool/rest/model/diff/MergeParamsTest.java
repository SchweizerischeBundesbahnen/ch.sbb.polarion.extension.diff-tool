package ch.sbb.polarion.extension.diff_tool.rest.model.diff;

import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeParamsTest {

    @Test
    void testBuilderCreatesInstance() {
        WorkItemsPair pair = new WorkItemsPair();
        List<WorkItemsPair> pairs = List.of(pair);

        MergeParams params = MergeParams.builder()
                .direction(MergeDirection.LEFT_TO_RIGHT)
                .linkRole("testRole")
                .configName("customConfig")
                .configCacheBucketId("bucket123")
                .pairs(pairs)
                .build();

        assertEquals(MergeDirection.LEFT_TO_RIGHT, params.getDirection());
        assertEquals("testRole", params.getLinkRole());
        assertEquals("customConfig", params.getConfigName());
        assertEquals("bucket123", params.getConfigCacheBucketId());
        assertEquals(pairs, params.getPairs());
    }

    @Test
    void testNoArgsConstructor() {
        MergeParams params = new MergeParams();

        assertNull(params.getDirection());
        assertNull(params.getLinkRole());
        assertNull(params.getConfigCacheBucketId());
        assertNull(params.getPairs());
    }

    @Test
    void testAllArgsConstructor() {
        WorkItemsPair pair = new WorkItemsPair();
        List<WorkItemsPair> pairs = List.of(pair);

        MergeParams params = new MergeParams(
                MergeDirection.RIGHT_TO_LEFT,
                "role",
                "config",
                "bucketId",
                false,
                pairs
        );

        assertEquals(MergeDirection.RIGHT_TO_LEFT, params.getDirection());
        assertEquals("role", params.getLinkRole());
        assertEquals("config", params.getConfigName());
        assertEquals("bucketId", params.getConfigCacheBucketId());
        assertEquals(pairs, params.getPairs());
    }

    @Test
    void testGetConfigNameReturnsDefaultWhenNull() {
        MergeParams params = new MergeParams();
        params.setConfigName(null);

        assertEquals(NamedSettings.DEFAULT_NAME, params.getConfigName());
    }

    @Test
    void testGetConfigNameReturnsSetValueWhenNotNull() {
        MergeParams params = new MergeParams();
        params.setConfigName("myConfig");

        assertEquals("myConfig", params.getConfigName());
    }

    @Test
    void testSettersAndGetters() {
        MergeParams params = new MergeParams();
        WorkItemsPair pair = new WorkItemsPair();
        List<WorkItemsPair> pairs = List.of(pair);

        params.setDirection(MergeDirection.LEFT_TO_RIGHT);
        params.setLinkRole("newRole");
        params.setConfigName("newConfig");
        params.setConfigCacheBucketId("newBucket");
        params.setPairs(pairs);

        assertEquals(MergeDirection.LEFT_TO_RIGHT, params.getDirection());
        assertEquals("newRole", params.getLinkRole());
        assertEquals("newConfig", params.getConfigName());
        assertEquals("newBucket", params.getConfigCacheBucketId());
        assertEquals(pairs, params.getPairs());
    }

    @Test
    void testEqualsAndHashCode() {
        List<WorkItemsPair> pairs = List.of(new WorkItemsPair());

        MergeParams params1 = MergeParams.builder()
                .direction(MergeDirection.LEFT_TO_RIGHT)
                .linkRole("role")
                .configName("config")
                .configCacheBucketId("bucket")
                .pairs(pairs)
                .build();

        MergeParams params2 = MergeParams.builder()
                .direction(MergeDirection.LEFT_TO_RIGHT)
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
                .direction(MergeDirection.LEFT_TO_RIGHT)
                .linkRole("role")
                .build();

        String toString = params.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("MergeParams"));
        assertTrue(toString.contains("direction=LEFT_TO_RIGHT"));
        assertTrue(toString.contains("linkRole=role"));
    }
}
