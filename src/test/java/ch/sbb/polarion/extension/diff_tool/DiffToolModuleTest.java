package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.ui.server.forms.extensions.FormExtensionContribution;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
public class DiffToolModuleTest {
    @Test
    void testGetFormExtensionContribution() {
        DiffToolModule diffToolModule = new DiffToolModule();
        String expectedId = "diff-tool";
        Class<?> expectedClass = DiffToolFormExtension.class;

        FormExtensionContribution contribution = diffToolModule.getFormExtensionContribution();

        assertNotNull(contribution);
        assertEquals(expectedClass, contribution.getExtensionClass());
        assertEquals(expectedId, contribution.getId());
    }
}
