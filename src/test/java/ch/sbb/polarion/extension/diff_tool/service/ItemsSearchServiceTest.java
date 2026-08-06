package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchCollection;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchResult;
import ch.sbb.polarion.extension.diff_tool.rest.model.search.SearchWorkItem;
import ch.sbb.polarion.extension.generic.util.EnumUtils;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ISeverityOpt;
import com.polarion.alm.tracker.model.IStatusOpt;
import com.polarion.alm.tracker.model.IModule;
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
            assertEquals("project.id:elibrary", result.getQuery());
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

            assertEquals("project.id:elibrary AND (type:task)", result.getQuery());
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
    void testExtremePagingValuesStillCutAValidWindow() {
        // Both the page and the page size come from query parameters, and the window arithmetic is done in
        // long, so no requested value turns 'from'/'to' into an out-of-range index.
        List<String> found = List.of("a", "b", "c");

        SearchResult<String> hugePage = service.toPage(found, "query", Integer.MAX_VALUE, 2, value -> value);
        assertEquals(2, hugePage.getPage());
        assertEquals(2, hugePage.getLastPage());
        assertEquals(List.of("c"), hugePage.getItems());

        SearchResult<String> hugePageSize = service.toPage(found, "query", Integer.MAX_VALUE, Integer.MAX_VALUE, value -> value);
        assertEquals(1, hugePageSize.getPage());
        assertEquals(1, hugePageSize.getLastPage());
        assertEquals(found, hugePageSize.getItems());

        SearchResult<String> negativeValues = service.toPage(found, "query", Integer.MIN_VALUE, Integer.MIN_VALUE, value -> value);
        assertEquals(1, negativeValues.getPage());
        assertEquals(1, negativeValues.getLastPage());
        assertEquals(found, negativeValues.getItems());
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
    void testAHeadingTakesItsTypeFromTheDocument() {
        // A document heading carries no type of its own, and Polarion's own table shows the document's heading
        // type in that column
        IWorkItem heading = headingItem();
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.of(heading)));

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            // Whatever the enum resolves for the heading type, the icon is chosen by its ID
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn("/polarion/icons/something-else.svg");

            SearchWorkItem item = service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0);

            assertEquals("heading", item.getType().getId());
            assertEquals("Heading", item.getType().getName());
            assertEquals("/polarion/ria/images/enums/type_heading.png", item.getType().getIconUrl());
            assertEquals(ItemsSearchService.HEADING_TYPE_ICON_URL, item.getType().getIconUrl());
        }
    }

    @Test
    void testAWorkItemTypedAsHeadingGetsTheSameIcon() {
        // Some documents give their headings the heading type outright, so getType() answers it
        IWorkItem heading = workItem("EL-5");
        ITypeOpt headingType = mock(ITypeOpt.class);
        when(headingType.getId()).thenReturn("heading");
        when(headingType.getName()).thenReturn(null);
        when(heading.getType()).thenReturn(headingType);
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.of(heading)));

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn(null);

            SearchWorkItem item = service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0);

            // ...and the name of an option the enum does not offer falls back to its capitalized ID
            assertEquals("Heading", item.getType().getName());
            assertEquals(ItemsSearchService.HEADING_TYPE_ICON_URL, item.getType().getIconUrl());
        }
    }

    @Test
    void testAnyOtherTypeKeepsTheIconTheEnumResolves() {
        IPObjectList<IWorkItem> found = objectList(List.of(workItem("EL-1")));
        when(trackerProject.queryWorkItems("", "id")).thenReturn(found);

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn("/polarion/icons/task.svg");

            SearchWorkItem item = service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0);

            assertEquals("/polarion/icons/task.svg", item.getType().getIconUrl());
        }
    }

    @Test
    void testAnUntypedItemOutsideADocumentHasNoType() {
        IWorkItem untyped = workItem("EL-5");
        when(untyped.getType()).thenReturn(null);
        when(untyped.getModule()).thenReturn(null);
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.of(untyped)));

        assertNull(service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0).getType());
    }

    @Test
    void testAnOptionWithoutANameRendersItsCapitalizedId() {
        IWorkItem workItem = workItem("EL-1");
        ITypeOpt type = mock(ITypeOpt.class);
        when(type.getId()).thenReturn("obsoleteType");
        when(type.getName()).thenReturn("");
        when(workItem.getType()).thenReturn(type);
        when(trackerProject.queryWorkItems("", "id")).thenReturn(objectList(List.of(workItem)));

        try (MockedStatic<EnumUtils> enumUtils = mockStatic(EnumUtils.class)) {
            enumUtils.when(() -> EnumUtils.getIconUrl(any())).thenReturn(null);

            assertEquals("ObsoleteType", service.searchWorkItems("elibrary", null, null, 1, 20).getItems().get(0).getType().getName());
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
    void testSearchCollectionsRestrictsTheQueryToTheProject() {
        // The restriction is a Lucene term, not a filter over the result: the stub answers the scoped query
        // only, so an unscoped search would find nothing here
        IPObjectList<IBaselineCollection> found = objectList(List.of(collection("c1", "elibrary"), collection("c3", "elibrary")));
        when(dataService.searchInstances(IBaselineCollection.PROTO, "project.id:elibrary", "name")).thenReturn(found);

        SearchResult<SearchCollection> result = service.searchCollections("elibrary", null, 1, 20);

        assertEquals(2, result.getTotalCount());
        assertEquals(List.of("c1", "c3"), result.getItems().stream().map(SearchCollection::getId).toList());
        assertEquals("project.id:elibrary", result.getQuery());
    }

    @Test
    void testSearchCollectionsKeepsTheCallersQueryInsideTheRestriction() {
        IPObjectList<IBaselineCollection> found = objectList(List.of(collection("c1", "elibrary")));
        when(dataService.searchInstances(IBaselineCollection.PROTO, "project.id:elibrary AND (name:a OR name:b)", "name")).thenReturn(found);

        SearchResult<SearchCollection> result = service.searchCollections("elibrary", "name:a OR name:b", 1, 20);

        assertEquals(List.of("c1"), result.getItems().stream().map(SearchCollection::getId).toList());
    }

    @Test
    void testSearchCollectionsMapsFields() {
        IBaselineCollection collection = collection("c1", "elibrary");
        when(dataService.searchInstances(IBaselineCollection.PROTO, "project.id:elibrary AND (name:release*)", "name"))
                .thenReturn(objectList(List.of(collection)));

        SearchResult<SearchCollection> result = service.searchCollections("elibrary", "name:release*", 1, 20);

        SearchCollection mapped = result.getItems().get(0);
        assertTrue(mapped.isReadable());
        assertEquals("Collection c1", mapped.getName());
        assertEquals("John Doe", mapped.getAuthorName());
        assertEquals(1000L, mapped.getCreated());
        assertEquals(2000L, mapped.getUpdated());
        assertEquals("project.id:elibrary AND (name:release*)", result.getQuery());
    }

    @Test
    void testAnUnresolvableCollectionIsReportedInsteadOfFailingThePage() {
        // A stale index entry throws on every attribute read, so nothing may read one before the guard has
        // classified it: it has to produce the explanatory row rather than break the whole page
        IBaselineCollection unresolvable = collection("c2", "elibrary");
        when(unresolvable.getProjectId()).thenThrow(new UnresolvableObjectException("gone"));
        when(unresolvable.getName()).thenThrow(new UnresolvableObjectException("gone"));
        when(unresolvable.isUnresolvable()).thenReturn(true);
        // The collections are built before the stubbing below: they stub themselves, and doing that inside a
        // thenReturn() argument leaves the outer when() unfinished
        IPObjectList<IBaselineCollection> found = objectList(List.of(collection("c1", "elibrary"), unresolvable));
        when(dataService.searchInstances(IBaselineCollection.PROTO, "project.id:elibrary", "name")).thenReturn(found);

        try (MockedStatic<Localization> localization = mockStatic(Localization.class)) {
            localization.when(() -> Localization.getString(eq("richpages.widget.table.unresolvableItem"), any(String[].class)))
                    .thenReturn("Collection c2 cannot be resolved");

            SearchResult<SearchCollection> result = service.searchCollections("elibrary", null, 1, 20);

            assertEquals(2, result.getTotalCount());
            assertTrue(result.getItems().get(0).isReadable());
            SearchCollection reported = result.getItems().get(1);
            assertFalse(reported.isReadable());
            assertEquals("Collection c2 cannot be resolved", reported.getUnavailableMessage());
            assertNull(reported.getName());
        }
    }

    @Test
    void testAnUnreadableCollectionIsReportedInsteadOfMapped() {
        IBaselineCollection unreadable = collection("c2", "elibrary");
        IPObjectPermissions permissions = mock(IPObjectPermissions.class);
        when(permissions.read()).thenReturn(false);
        when(unreadable.can()).thenReturn(permissions);
        IPObjectList<IBaselineCollection> found = objectList(List.of(unreadable));
        when(dataService.searchInstances(IBaselineCollection.PROTO, "project.id:elibrary", "name")).thenReturn(found);

        try (MockedStatic<Localization> localization = mockStatic(Localization.class)) {
            localization.when(() -> Localization.getString("security.cannotread")).thenReturn("You cannot read this collection");

            SearchCollection mapped = service.searchCollections("elibrary", null, 1, 20).getItems().get(0);

            assertFalse(mapped.isReadable());
            assertEquals("You cannot read this collection", mapped.getUnavailableMessage());
            assertNull(mapped.getName());
        }
    }

    @Test
    void testSearchCollectionsToleratesMissingAuthorAndDates() {
        IBaselineCollection collection = collection("c1", "elibrary");
        when(collection.getAuthor()).thenReturn(null);
        when(collection.getCreated()).thenReturn(null);
        when(collection.getUpdated()).thenReturn(null);
        when(dataService.searchInstances(IBaselineCollection.PROTO, "project.id:elibrary", "name"))
                .thenReturn(objectList(List.of(collection)));

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

    /** A document heading: no type of its own, but a document whose heading type stands in for it. */
    private IWorkItem headingItem() {
        IWorkItem heading = workItem("EL-5");
        when(heading.getType()).thenReturn(null);

        ITypeOpt headingType = mock(ITypeOpt.class);
        when(headingType.getId()).thenReturn("heading");
        when(headingType.getName()).thenReturn("Heading");
        IModule module = mock(IModule.class);
        when(module.getHeadingWorkItemType()).thenReturn(headingType);
        when(heading.getModule()).thenReturn(module);

        return heading;
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
