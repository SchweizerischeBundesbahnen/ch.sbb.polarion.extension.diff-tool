package ch.sbb.polarion.extension.diff_tool.util;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.internal.security.UserPrincipal;
import com.polarion.subterra.base.location.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.Subject;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentWorkItemsCacheTest {
    private final DocumentWorkItemsCache documentWorkItemsCache = new DocumentWorkItemsCache();
    @Mock
    private IModule document;
    @Mock
    private IWorkItem workItem1;
    @Mock
    private IWorkItem workItem2;
    @Mock
    private IWorkItem workItem3;
    private Subject subject;

    @BeforeEach
    void setup() {
        prepareDocumentWorkItems();
        subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal("testUser"));
    }

    @Test
    void shouldFindDocumentInCache() {
        // Arrange
        documentWorkItemsCache.cacheWorkItemsFromDocument(document, subject);

        // Act
        IWorkItem workItem = documentWorkItemsCache.getWorkItem(document, subject, "test-workitem-id-2");

        // Assert
        assertThat(workItem.getId()).isEqualTo("test-workitem-id-2");
        verify(document).getAllWorkItems();
    }

    @Test
    void shouldFindDocumentInCacheWithNullSubject() {
        // Arrange
        documentWorkItemsCache.cacheWorkItemsFromDocument(document, null);

        // Act
        IWorkItem workItem = documentWorkItemsCache.getWorkItem(document, null, "test-workitem-id-2");

        // Assert
        assertThat(workItem.getId()).isEqualTo("test-workitem-id-2");
        verify(document).getAllWorkItems();
    }

    @Test
    void shouldReturnNullForUnknownWorkItem() {
        // Arrange
        documentWorkItemsCache.cacheWorkItemsFromDocument(document, subject);

        // Act
        IWorkItem workItem = documentWorkItemsCache.getWorkItem(document, subject, "unknown");

        // Assert
        assertThat(workItem).isNull();
        verify(document).getAllWorkItems();
    }

    @Test
    void shouldReturnNullForAnotherUser() {
        // Arrange
        documentWorkItemsCache.cacheWorkItemsFromDocument(document, subject);

        Subject anotherSubject = new Subject();
        subject.getPrincipals().add(new UserPrincipal("anotherUser"));
        // cache will try to reinitialize workItems, if document is not cached for another user
        when(document.getAllWorkItems()).thenReturn(List.of());

        // Act
        IWorkItem workItem = documentWorkItemsCache.getWorkItem(document, anotherSubject, "test-workitem-id-1");

        // Assert
        assertThat(workItem).isNull();
        verify(document, times(2)).getAllWorkItems();
    }

    @Test
    void shouldReinitializeCacheAfterEviction() {
        // Arrange
        documentWorkItemsCache.cacheWorkItemsFromDocument(document, subject);
        documentWorkItemsCache.evictDocumentFromCache("test-project-id", Location.getLocation(null, "testPath", "testRevision"), "testRevision", subject);
        // cache will try to reinitialize workItems, if document is not cached
        when(document.getAllWorkItems()).thenReturn(List.of());

        // Act
        IWorkItem workItem = documentWorkItemsCache.getWorkItem(document, subject, "test-workitem-id-1");

        // Assert
        assertThat(workItem).isNull();
        verify(document, times(2)).getAllWorkItems();
    }

    @Test
    void shouldReinitializeCacheForAnotherLocation() {
        // Arrange
        documentWorkItemsCache.cacheWorkItemsFromDocument(document, subject);
        when(document.getModuleLocation()).thenReturn(Location.getLocation(null, "testPath", "newRevision"));
        // cache will try to reinitialize workItems, if document is not cached
        when(document.getAllWorkItems()).thenReturn(List.of());

        // Act
        IWorkItem workItem = documentWorkItemsCache.getWorkItem(document, subject, "test-workitem-id-1");

        // Assert
        assertThat(workItem).isNull();
        verify(document, times(2)).getAllWorkItems();
    }

    @Test
    void shouldReinitializeCacheForAnotherRevision() {
        // Arrange
        documentWorkItemsCache.cacheWorkItemsFromDocument(document, subject);
        when(document.getRevision()).thenReturn(null);
        // cache will try to reinitialize workItems, if document is not cached
        when(document.getAllWorkItems()).thenReturn(List.of());

        // Act
        IWorkItem workItem = documentWorkItemsCache.getWorkItem(document, subject, "test-workitem-id-1");

        // Assert
        assertThat(workItem).isNull();
        verify(document, times(2)).getAllWorkItems();
    }

    private void prepareDocumentWorkItems() {
        when(document.getProjectId()).thenReturn("test-project-id");
        when(document.getModuleLocation()).thenReturn(Location.getLocation(null, "testPath", "testRevision"));
        when(document.getRevision()).thenReturn("testRevision");
        when(workItem1.getId()).thenReturn("test-workitem-id-1");
        when(workItem2.getId()).thenReturn("test-workitem-id-2");
        when(workItem3.getId()).thenReturn("test-workitem-id-3");
        when(document.getAllWorkItems()).thenReturn(List.of(workItem1, workItem2, workItem3));
    }
}