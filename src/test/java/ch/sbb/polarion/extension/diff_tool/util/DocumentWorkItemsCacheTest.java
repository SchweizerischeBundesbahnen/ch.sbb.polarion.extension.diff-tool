package ch.sbb.polarion.extension.diff_tool.util;

import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.internal.security.UserCredentials;
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
    private final List<UserPrincipal> principles = List.of(new UserPrincipal("testUser1"), new UserPrincipal("testUser2"));
    private final List<UserCredentials> credentials = List.of(new UserCredentials("testKey1", "testLogin1", "testPassword1"),
            new UserCredentials("testKey2", "testLogin2", "testPassword2"));

    @BeforeEach
    void setup() {
        prepareDocumentWorkItems();
        subject = new Subject();
        subject.getPrincipals().addAll(principles);
        subject.getPublicCredentials().addAll(credentials);
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
    void shouldGetWorkItemFromCacheWithDestroyedCredentials() {
        // Arrange
        Subject subject1 = new Subject();
        subject1.getPrincipals().addAll(principles);
        subject1.getPublicCredentials().addAll(credentials);

        Subject subject2 = new Subject();
        subject2.getPrincipals().addAll(principles);
        subject2.getPublicCredentials().addAll(credentials);

        // Act
        // 1. Get workItem form document and save it to cache
        IWorkItem workItem1 = documentWorkItemsCache.getWorkItem(document, subject1, "test-workitem-id-1");
        // 2. Simulate destroy credentials by logout
        subject1.getPublicCredentials().forEach(c -> ((UserCredentials) c).destroy());

        // 3. Get workItem form cache with new subject
        IWorkItem workItem2 = documentWorkItemsCache.getWorkItem(document, subject2, "test-workitem-id-1");

        // Assert
        assertThat(workItem1.getId()).isEqualTo("test-workitem-id-1");
        assertThat(workItem2.getId()).isEqualTo("test-workitem-id-1");
        verify(document, times(1)).getAllWorkItems();
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