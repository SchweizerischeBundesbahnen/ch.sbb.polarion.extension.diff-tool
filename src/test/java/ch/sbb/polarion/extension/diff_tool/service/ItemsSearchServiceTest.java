package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.generic.util.EnumUtils;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ISeverityOpt;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.alm.tracker.model.IWorkItemPermissions;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.platform.i18n.Localization;
import com.polarion.platform.persistence.UnresolvableObjectException;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.model.IPObjectPermissions;
import com.polarion.subterra.base.SubterraURI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.AbstractList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ItemsSearchServiceTest {

    @Mock
    private PolarionService polarionService;

    @Mock
    private ITrackerProject trackerProject;

    @Mock
    private ITrackerService trackerService;

    @Mock
    private IDataService dataService;

    private ItemsSearchService service;

    @BeforeEach
    void setUp() {
        service = new ItemsSearchService(polarionService);
        when(polarionService.getTrackerProject(any())).thenReturn(trackerProject);
        when(polarionService.getTrackerService()).thenReturn(trackerService);
        when(trackerService.getDataService()).thenReturn(dataService);
    }

    @Test
    void testSearchWorkItemsReturnsFirstPageAndMapsFields() {
        // The work items are built before the stubbing below: they are mocks themselves, and stubbing one
        // inside a thenReturn() argument leaves the outer when() unfinished.
        IPObjectList<IWorkItem> found = objectList(List.of(workItem("EL-1"), workItem("EL-2"), workItem("EL-3")));
        when(trackerProject.queryWorkItems("", "id")).thenReturn(found);

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn("/polarion/icons/type.svg");

            SearchResult<SearchWorkItem> result = service.searchWorkItems("elibrary", null, null, 1, 2);

            assertEquals(3, result.getTotalCount());
            assertEquals(1, result.getPage());
            assertEquals(2, result.getLastPage());
            assertEquals("", result.getQuery());
            assertEquals(List.of("EL-1", "EL-2"), result.getItems().stream().map(SearchWorkItem::getId).toList());

            SearchWorkItem first = result.getItems().get(0);
            assertTrue(first.isReadable());
            assertEquals("elibrary", first.getProjectId());
            assertEquals("Title of EL-1", first.getTitle());
            assertEquals("task", first.getType().getId());
            assertEquals("Task", first.getType().getName());
            assertEquals("/polarion/icons/type.svg", first.getType().getIconUrl());
            assertEquals("Open", first.getStatus().getName());
            assertEquals("Major", first.getSeverity().getName());
        }
    }

    @Test
    void testSearchWorkItemsUsesTheGivenQueryAndSort() {
        IPObjectList<IWorkItem> found = objectList(List.of(workItem("EL-1")));
        when(trackerProject.queryWorkItems("type:task", "title")).thenReturn(found);

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn(null);

            SearchResult<SearchWorkItem> result = service.searchWorkItems("elibrary", "type:task", "title", 1, 20);

            assertEquals("type:task", result.getQuery());
            assertEquals(1, result.getItems().size());
        }
    }

    @Test
    void testSearchWorkItemsClampsAnOutOfRangePage() {
        IPObjectList<IWorkItem> found = objectList(List.of(workItem("EL-1"), workItem("EL-2"), workItem("EL-3")));
        when(trackerProject.queryWorkItems("", "id")).thenReturn(found);

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn(null);

            SearchResult<SearchWorkItem> beyondLast = service.searchWorkItems("elibrary", null, null, 9, 2);
            assertEquals(2, beyondLast.getPage());
            assertEquals(List.of("EL-3"), beyondLast.getItems().stream().map(SearchWorkItem::getId).toList());

            SearchResult<SearchWorkItem> beforeFirst = service.searchWorkItems("elibrary", null, null, 0, 2);
            assertEquals(1, beforeFirst.getPage());
        }
    }

    @Test
    void testSearchWorkItemsFallsBackToTheDefaultPageSize() {
        IPObjectList<IWorkItem> found = objectList(List.of(workItem("EL-1"), workItem("EL-2")));
        when(trackerProject.queryWorkItems("", "id")).thenReturn(found);

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn(null);

            // 0 would have been a division by zero in the widget's page arithmetic
            SearchResult<SearchWorkItem> result = service.searchWorkItems("elibrary", null, null, 1, 0);

            assertEquals(1, result.getLastPage());
            assertEquals(2, result.getItems().size());
        }
    }

    @Test
    void testSearchWorkItemsReportsAnEmptyResult() {
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.<IWorkItem>of()));

        SearchResult<SearchWorkItem> result = service.searchWorkItems("elibrary", null, null, 1, 20);

        assertEquals(0, result.getTotalCount());
        assertEquals(1, result.getPage());
        assertEquals(1, result.getLastPage());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void testUnresolvableWorkItemIsReportedInsteadOfMapped() {
        IWorkItem unresolvable = workItem("EL-1");
        when(unresolvable.isUnresolvable()).thenReturn(true);
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.of(unresolvable)));

        try (MockedStatic<Localization> localization = mockStatic(Localization.class)) {
            // The service passes a String, so this is the getString(String, String...) overload
            localization.when(() -> Localization.getString(eq("richpages.widget.table.unresolvableItem"), any(String[].class)))
                    .thenReturn("Item EL-1 cannot be resolved");

            SearchWorkItem item = service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0);

            assertFalse(item.isReadable());
            assertEquals("Item EL-1 cannot be resolved", item.getUnavailableMessage());
            assertNull(item.getTitle());
        }
    }

    @Test
    void testUnreadableWorkItemIsReportedInsteadOfMapped() {
        IWorkItem unreadable = workItem("EL-1");
        IWorkItemPermissions permissions = mock(IWorkItemPermissions.class);
        when(permissions.read()).thenReturn(false);
        when(unreadable.can()).thenReturn(permissions);
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.of(unreadable)));

        try (MockedStatic<Localization> localization = mockStatic(Localization.class)) {
            localization.when(() -> Localization.getString("security.cannotread")).thenReturn("You cannot read this item");

            SearchWorkItem item = service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0);

            assertFalse(item.isReadable());
            assertEquals("You cannot read this item", item.getUnavailableMessage());
        }
    }

    @Test
    void testAnItemWhoseIdCannotBeReadStillProducesARow() {
        IWorkItem broken = workItem("EL-1");
        when(broken.getId()).thenThrow(new UnresolvableObjectException("gone"));
        when(broken.isUnresolvable()).thenReturn(true);
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.of(broken)));

        try (MockedStatic<Localization> localization = mockStatic(Localization.class)) {
            localization.when(() -> Localization.getString(eq("richpages.widget.table.unresolvableItem"), any(String[].class)))
                    .thenReturn("This item cannot be resolved");

            SearchWorkItem item = service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0);

            assertNull(item.getId());
            assertFalse(item.isReadable());
            assertEquals("This item cannot be resolved", item.getUnavailableMessage());
        }
    }

    @Test
    void testSearchCollectionsKeepsOnlyTheRequestedProject() {
        IPObjectList<IBaselineCollection> found =
                objectList(List.of(collection("c1", "elibrary"), collection("c2", "otherProject"), collection("c3", "elibrary")));
        when(dataService.searchInstances(IBaselineCollection.PROTO, "", "name")).thenReturn(found);

        SearchResult<SearchCollection> result = service.searchCollections("elibrary", null, 1, 20);

        assertEquals(2, result.getTotalCount());
        assertEquals(List.of("c1", "c3"), result.getItems().stream().map(SearchCollection::getId).toList());
    }

    @Test
    void testSearchCollectionsMapsFields() {
        IBaselineCollection collection = collection("c1", "elibrary");
        when(dataService.searchInstances(IBaselineCollection.PROTO, "name:release*", "name")).thenReturn(objectList(List.of(collection)));

        SearchResult<SearchCollection> result = service.searchCollections("elibrary", "name:release*", 1, 20);

        SearchCollection mapped = result.getItems().get(0);
        assertTrue(mapped.isReadable());
        assertEquals("Collection c1", mapped.getName());
        assertEquals("John Doe", mapped.getAuthorName());
        assertEquals(1000L, mapped.getCreated());
        assertEquals(2000L, mapped.getUpdated());
        assertEquals("name:release*", result.getQuery());
    }

    @Test
    void testSearchCollectionsToleratesMissingAuthorAndDates() {
        IBaselineCollection collection = collection("c1", "elibrary");
        when(collection.getAuthor()).thenReturn(null);
        when(collection.getCreated()).thenReturn(null);
        when(collection.getUpdated()).thenReturn(null);
        when(dataService.searchInstances(IBaselineCollection.PROTO, "", "name")).thenReturn(objectList(List.of(collection)));

        SearchCollection mapped = service.searchCollections("elibrary", null, 1, 20).getItems().get(0);

        assertNull(mapped.getAuthorName());
        assertNull(mapped.getCreated());
        assertNull(mapped.getUpdated());
    }

    private IWorkItem workItem(String id) {
        IWorkItem workItem = mock(IWorkItem.class);
        IWorkItemPermissions permissions = mock(IWorkItemPermissions.class);
        when(permissions.read()).thenReturn(true);
        when(workItem.can()).thenReturn(permissions);
        when(workItem.isUnresolvable()).thenReturn(false);
        when(workItem.getId()).thenReturn(id);
        when(workItem.getProjectId()).thenReturn("elibrary");
        when(workItem.getTitle()).thenReturn("Title of " + id);

        ITypeOpt type = mock(ITypeOpt.class);
        when(type.getId()).thenReturn("task");
        when(type.getName()).thenReturn("Task");
        when(workItem.getType()).thenReturn(type);

        IStatusOpt status = mock(IStatusOpt.class);
        when(status.getId()).thenReturn("open");
        when(status.getName()).thenReturn("Open");
        when(workItem.getStatus()).thenReturn(status);

        ISeverityOpt severity = mock(ISeverityOpt.class);
        when(severity.getId()).thenReturn("major");
        when(severity.getName()).thenReturn("Major");
        when(workItem.getSeverity()).thenReturn(severity);

        return workItem;
    }

    private IBaselineCollection collection(String id, String projectId) {
        IBaselineCollection collection = mock(IBaselineCollection.class);
        IPObjectPermissions permissions = mock(IPObjectPermissions.class);
        when(permissions.read()).thenReturn(true);
        when(collection.can()).thenReturn(permissions);
        when(collection.isUnresolvable()).thenReturn(false);
        when(collection.getId()).thenReturn(id);
        when(collection.getProjectId()).thenReturn(projectId);
        when(collection.getName()).thenReturn("Collection " + id);
        when(collection.getCreated()).thenReturn(new Date(1000L));
        when(collection.getUpdated()).thenReturn(new Date(2000L));

        IUser author = mock(IUser.class);
        when(author.getName()).thenReturn("John Doe");
        when(collection.getAuthor()).thenReturn(author);

        return collection;
    }

    /**
     * A plain list in the shape the Polarion query APIs return. Mocking IPObjectList would mean stubbing the
     * List methods the service walks, which says nothing about the code under test.
     */
    private static <T extends IPObject> IPObjectList<T> objectList(List<T> items) {
        return new TestPObjectList<>(items);
    }

    private static class TestPObjectList<T extends IPObject> extends AbstractList<T> implements IPObjectList<T> {
        private final List<T> items;

        TestPObjectList(List<T> items) {
            this.items = items;
        }

        @Override
        public T get(int index) {
            return items.get(index);
        }

        @Override
        public int size() {
            return items.size();
        }

        @Override
        public IDataService getDataService() {
            return null;
        }

        @Override
        public void resolveAll() {
            // nothing to resolve in a test list
        }

        @Override
        public void resolveFirst(int count) {
            // nothing to resolve in a test list
        }

        @Override
        public void resolve(int from, int to) {
            // nothing to resolve in a test list
        }

        @Override
        public List<SubterraURI> getUrisList() {
            return List.of();
        }
    }
}
