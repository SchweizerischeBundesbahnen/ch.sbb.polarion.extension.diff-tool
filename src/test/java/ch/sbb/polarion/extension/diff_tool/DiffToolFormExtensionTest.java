package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class DiffToolFormExtensionTest {
    @Test
    void testConstructor() {
        DiffToolFormExtension diffToolFormExtension = new DiffToolFormExtension();
        assertNotNull(diffToolFormExtension);
    }
}
