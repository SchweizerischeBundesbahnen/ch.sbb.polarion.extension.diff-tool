package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.TransactionalExecutorExtension;
import com.polarion.alm.tracker.internal.url.IAttachmentUrlResolver;
import com.polarion.alm.tracker.internal.url.PolarionUrlResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class, TransactionalExecutorExtension.class})
class ResourceProviderTest {
    private ResourceProvider resourceProvider;

    @BeforeEach
    void setUp() {
        resourceProvider = new ResourceProvider();
    }

    @Test
    void testGetResourceAsBytesSuccess() {
        String resource = "validResource";
        byte[] expectedData = "testData".getBytes();
        InputStream inputStream = new ByteArrayInputStream(expectedData);

        try (MockedStatic<PolarionUrlResolver> iAttachmentUrlResolver = mockStatic(PolarionUrlResolver.class)) {
            IAttachmentUrlResolver resolver = mock(IAttachmentUrlResolver.class);
            iAttachmentUrlResolver.when(PolarionUrlResolver::getInstance).thenReturn(resolver);

            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenReturn(inputStream);

            byte[] result = resourceProvider.getResourceAsBytes(resource);

            assertArrayEquals(expectedData, result);
        }
    }

    @Test
    void testGetResourceAsBytesResourceNotFound() {
        String resource = "invalidResource";

        try (MockedStatic<PolarionUrlResolver> iAttachmentUrlResolver = mockStatic(PolarionUrlResolver.class)) {
            IAttachmentUrlResolver resolver = mock(IAttachmentUrlResolver.class);
            iAttachmentUrlResolver.when(PolarionUrlResolver::getInstance).thenReturn(resolver);
            when(resolver.canResolve(resource)).thenReturn(false);

            byte[] result = resourceProvider.getResourceAsBytes(resource);

            assertArrayEquals(new byte[0], result);
        }
    }

    @Test
    void testGetResourceAsBytesExceptionHandling() {
        String resource = "errorResource";
        try (MockedStatic<PolarionUrlResolver> iAttachmentUrlResolver = mockStatic(PolarionUrlResolver.class)) {
            IAttachmentUrlResolver resolver = mock(IAttachmentUrlResolver.class);
            iAttachmentUrlResolver.when(PolarionUrlResolver::getInstance).thenReturn(resolver);
            when(resolver.canResolve(resource)).thenReturn(true);
            when(resolver.resolve(resource)).thenThrow(new RuntimeException("Test Exception"));

            byte[] result = resourceProvider.getResourceAsBytes(resource);

            assertArrayEquals(new byte[0], result);
        }
    }
}
