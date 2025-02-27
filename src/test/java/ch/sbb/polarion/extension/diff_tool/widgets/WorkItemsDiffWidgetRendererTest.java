package ch.sbb.polarion.extension.diff_tool.widgets;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.ModelObjectsBase;
import com.polarion.alm.shared.api.model.ModelObjectsSearch;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.Renderer;
import com.polarion.alm.shared.api.model.fields.Field;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.model.wi.WorkItem;
import com.polarion.alm.shared.api.model.wi.WorkItemFields;
import com.polarion.alm.shared.api.model.wi.WorkItemPermissions;
import com.polarion.alm.shared.api.model.wi.WorkItemReference;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagSelector;
import com.polarion.alm.shared.api.utils.internal.InternalPolarionUtils;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.subterra.base.data.identification.ILocalId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class WorkItemsDiffWidgetRendererTest {

    @Mock
    private RichPageWidgetCommonContext context;

    @Mock
    private DiffWidgetParams params;

    @Test
    void testConstructor() {
        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);
        assertNotNull(renderer);
    }

    @Test
    void testRenderUnresolvableItem() {
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("richpages.widget.table.unresolvableItem", "path")).thenReturn("Unresolvable item");

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(true);
        WorkItemReference reference = mock(WorkItemReference.class);
        when(item.getReferenceToCurrent()).thenReturn(reference);
        when(reference.toPath()).thenReturn("path");

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        when(tdBuilder.attributes()).thenReturn(attributesBuilder);
        when(attributesBuilder.colspan(anyString())).thenReturn(attributesBuilder);
        when(attributesBuilder.className(anyString())).thenReturn(attributesBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);
        when(tdContentBuilder.text(anyString())).thenReturn(tdContentBuilder);

        renderer.renderItem(builder, item, mock(DataSetWidgetParams.class));

        verify(attributesBuilder, times(1)).colspan("6");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Unresolvable item");
    }

    @Test
    void testRenderNotReadableItem() {
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("security.cannotread")).thenReturn("Not readable item");

        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(false);

        WorkItemPermissions permissions = mock(WorkItemPermissions.class);
        when(item.can()).thenReturn(permissions);
        when(permissions.read()).thenReturn(false);

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlAttributesBuilder attributesBuilder = mock(HtmlAttributesBuilder.class);
        when(tdBuilder.attributes()).thenReturn(attributesBuilder);
        when(attributesBuilder.colspan(anyString())).thenReturn(attributesBuilder);
        when(attributesBuilder.className(anyString())).thenReturn(attributesBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);
        when(tdContentBuilder.text(anyString())).thenReturn(tdContentBuilder);

        renderer.renderItem(builder, item, mock(DataSetWidgetParams.class));

        verify(attributesBuilder, times(1)).colspan("6");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Not readable item");
    }

    @Test
    void testRenderItem() {
        WorkItemsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(WorkItem.class);
        when(item.isUnresolvable()).thenReturn(false);

        WorkItemPermissions permissions = mock(WorkItemPermissions.class);
        when(item.can()).thenReturn(permissions);
        when(permissions.read()).thenReturn(true);

        IWorkItem oldApi = mock(IWorkItem.class);
        when(item.getOldApi()).thenReturn(oldApi);
        IPrototype prototype = mock(IPrototype.class);
        when(oldApi.getPrototype()).thenReturn(prototype);
        when(prototype.getName()).thenReturn(PrototypeEnum.WorkItem.name());
        ILocalId localId = mock(ILocalId.class);
        lenient().when(oldApi.getLocalId()).thenReturn(localId);
        lenient().when(localId.getObjectName()).thenReturn("objectName");

        WorkItemFields fields = mock(WorkItemFields.class);
        when(item.fields()).thenReturn(fields);
        Field field = mock(Field.class);
        when(fields.get(anyString())).thenReturn(field);
        Renderer fieldRenderer = mock(Renderer.class);
        when(field.render()).thenReturn(fieldRenderer);
        when(fieldRenderer.withLinks(anyBoolean())).thenReturn(fieldRenderer);

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(builder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder tdBuilder = mock(HtmlTagBuilder.class);
        when(tagSelector.td()).thenReturn(tdBuilder);

        HtmlContentBuilder tdContentBuilder = mock(HtmlContentBuilder.class);
        when(tdBuilder.append()).thenReturn(tdContentBuilder);

        HtmlTagSelector<HtmlTagBuilder> tdContentTagSelector = mock(HtmlTagSelector.class);
        when(tdContentBuilder.tag()).thenReturn(tdContentTagSelector);

        HtmlTagBuilder checkboxBuilder = mock(HtmlTagBuilder.class);
        when(tdContentTagSelector.byName("input")).thenReturn(checkboxBuilder);

        HtmlAttributesBuilder checkboxAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(checkboxBuilder.attributes()).thenReturn(checkboxAttributesBuilder);
        when(checkboxAttributesBuilder.byName(anyString(), anyString())).thenReturn(checkboxAttributesBuilder);
        when(checkboxAttributesBuilder.className(anyString())).thenReturn(checkboxAttributesBuilder);

        renderer.renderItem(builder, item, mock(DataSetWidgetParams.class));

        verify(checkboxAttributesBuilder, times(1)).byName("type", "checkbox");
        verify(checkboxAttributesBuilder, times(1)).byName("data-type", PrototypeEnum.WorkItem.name());
        verify(checkboxAttributesBuilder, times(1)).byName("data-space", "");
        verify(checkboxAttributesBuilder, times(1)).byName("data-id", "");
        verify(checkboxAttributesBuilder, times(1)).className("export-item");
    }

    private WorkItemsDiffWidgetRenderer mockRenderer(RichPageWidgetCommonContext context, DiffWidgetParams params) {
        Scope scope = mock(Scope.class);
        when(context.getDisplayedScope()).thenReturn(scope);

        InternalReadOnlyTransaction internalReadOnlyTransaction = mock(InternalReadOnlyTransaction.class);
        when(internalReadOnlyTransaction.utils()).thenReturn(mock(InternalPolarionUtils.class));
        ModelObjectsBase modelObjectsBase = mock(ModelObjectsBase.class);
        ModelObjectsSearch modelObjectsSearch = mock(ModelObjectsSearch.class);
        when(modelObjectsSearch.query(null)).thenReturn(modelObjectsSearch);
        when(modelObjectsSearch.sort("id")).thenReturn(modelObjectsSearch);
        when(modelObjectsBase.search()).thenReturn(modelObjectsSearch);
        when(internalReadOnlyTransaction.byEnum(any())).thenReturn(modelObjectsBase);
        when(context.transaction()).thenReturn(internalReadOnlyTransaction);

        when(params.sourceParams()).thenReturn(mock(DataSetWidgetParams.class));

        when(scope.isGlobal()).thenReturn(false);
        return new WorkItemsDiffWidgetRenderer(context, params);
    }
}
