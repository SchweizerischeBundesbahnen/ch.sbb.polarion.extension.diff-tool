package ch.sbb.polarion.extension.diff_tool.widgets;

import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.shared.api.Scope;
import com.polarion.alm.shared.api.model.ModelObject;
import com.polarion.alm.shared.api.model.ModelObjectsBase;
import com.polarion.alm.shared.api.model.ModelObjectsSearch;
import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.Renderer;
import com.polarion.alm.shared.api.model.ModelObjectPermissions;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollection;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionFields;
import com.polarion.alm.shared.api.model.baselinecollection.BaselineCollectionReference;
import com.polarion.alm.shared.api.model.fields.Field;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.transaction.internal.InternalReadOnlyTransaction;
import com.polarion.alm.shared.api.utils.SharedLocalization;
import com.polarion.alm.shared.api.utils.html.HtmlAttributesBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagSelector;
import com.polarion.alm.shared.api.utils.internal.InternalPolarionUtils;
import com.polarion.alm.tracker.model.baselinecollection.IBaselineCollection;
import com.polarion.platform.persistence.model.IPrototype;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class CollectionsDiffWidgetRendererTest {

    @Mock
    private RichPageWidgetCommonContext context;

    @Mock
    private DiffWidgetParams params;

    @Mock
    private PolarionService polarionService;

    @Test
    void testConstructor() {
        CollectionsDiffWidgetRenderer renderer = mockRenderer(context, params);
        assertNotNull(renderer);
    }

    @Test
    void testRenderUnresolvableItem() {
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("richpages.widget.table.unresolvableItem", "path")).thenReturn("Unresolvable item");

        CollectionsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(BaselineCollection.class);
        when(item.isUnresolvable()).thenReturn(true);
        BaselineCollectionReference reference = mock(BaselineCollectionReference.class);
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

        verify(attributesBuilder, times(1)).colspan("5");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Unresolvable item");
    }

    @Test
    void testRenderNotReadableItem() {
        SharedLocalization localization = mock(SharedLocalization.class);
        when(context.localization()).thenReturn(localization);
        when(localization.getString("security.cannotread")).thenReturn("Not readable item");

        CollectionsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(BaselineCollection.class);
        when(item.isUnresolvable()).thenReturn(false);

        ModelObjectPermissions permissions = mock(ModelObjectPermissions.class);
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

        verify(attributesBuilder, times(1)).colspan("5");
        verify(attributesBuilder, times(1)).className("polarion-rpw-table-not-readable-cell");
        verify(tdContentBuilder, times(1)).text("Not readable item");
    }

    @Test
    void testRenderItem() {
        CollectionsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder builder = mock(HtmlContentBuilder.class);
        ModelObject item = mock(BaselineCollection.class);
        when(item.isUnresolvable()).thenReturn(false);

        ModelObjectPermissions permissions = mock(ModelObjectPermissions.class);
        when(item.can()).thenReturn(permissions);
        when(permissions.read()).thenReturn(true);

        IBaselineCollection oldApi = mock(IBaselineCollection.class);
        when(item.getOldApi()).thenReturn(oldApi);
        IPrototype prototype = mock(IPrototype.class);
        when(oldApi.getPrototype()).thenReturn(prototype);
        when(prototype.getName()).thenReturn(PrototypeEnum.BaselineCollection.name());
        lenient().when(oldApi.getValue("id")).thenReturn("collection-id");

        BaselineCollectionFields fields = mock(BaselineCollectionFields.class);
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

        HtmlTagBuilder radioBuilder = mock(HtmlTagBuilder.class);
        when(tdContentTagSelector.byName("input")).thenReturn(radioBuilder);

        HtmlAttributesBuilder radioAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(radioBuilder.attributes()).thenReturn(radioAttributesBuilder);
        when(radioAttributesBuilder.byName(anyString(), anyString())).thenReturn(radioAttributesBuilder);
        when(radioAttributesBuilder.className(anyString())).thenReturn(radioAttributesBuilder);

        DataSetWidgetParams dataSetParams = mock(DataSetWidgetParams.class);
        when(dataSetParams.side()).thenReturn(DataSetWidgetParams.Side.SOURCE);

        renderer.renderItem(builder, item, dataSetParams);

        verify(radioAttributesBuilder, times(1)).byName("type", "radio");
        verify(radioAttributesBuilder, times(1)).byName("name", "source-collection");
        verify(radioAttributesBuilder, times(1)).byName("data-type", PrototypeEnum.BaselineCollection.name());
        verify(radioAttributesBuilder, times(1)).byName("data-id", "collection-id");
        verify(radioAttributesBuilder, times(1)).className("export-item");
    }

    @Test
    void testRenderHeaderRow() {
        CollectionsDiffWidgetRenderer renderer = mockRenderer(context, params);

        HtmlContentBuilder parentContentBuilder = mock(HtmlContentBuilder.class);

        HtmlTagSelector<HtmlTagBuilder> tagSelector = mock(HtmlTagSelector.class);
        when(parentContentBuilder.tag()).thenReturn(tagSelector);

        HtmlTagBuilder row = mock(HtmlTagBuilder.class);
        when(tagSelector.tr()).thenReturn(row);

        HtmlAttributesBuilder rowAttributesBuilder = mock(HtmlAttributesBuilder.class);
        when(row.attributes()).thenReturn(rowAttributesBuilder);

        HtmlContentBuilder rowContentBuilder = mock(HtmlContentBuilder.class);
        when(row.append()).thenReturn(rowContentBuilder);

        HtmlTagSelector<HtmlTagBuilder> rowTagSelector = mock(HtmlTagSelector.class);
        when(rowContentBuilder.tag()).thenReturn(rowTagSelector);

        HtmlTagBuilder thBuilder = mock(HtmlTagBuilder.class);
        when(rowTagSelector.th()).thenReturn(thBuilder);

        HtmlContentBuilder thContentBuilder = mock(HtmlContentBuilder.class);
        when(thBuilder.append()).thenReturn(thContentBuilder);
        when(thContentBuilder.text(anyString())).thenReturn(thContentBuilder);

        renderer.renderHeaderRow(parentContentBuilder);

        verify(rowAttributesBuilder, times(1)).className("polarion-rpw-table-header-row");
        // 1 empty header for radio button column + 4 field columns = 5 th elements
        verify(rowTagSelector, times(5)).th();
    }

    private CollectionsDiffWidgetRenderer mockRenderer(RichPageWidgetCommonContext context, DiffWidgetParams params) {
        Scope scope = mock(Scope.class);
        lenient().when(context.getDisplayedScope()).thenReturn(scope);

        InternalReadOnlyTransaction internalReadOnlyTransaction = mock(InternalReadOnlyTransaction.class);
        lenient().when(internalReadOnlyTransaction.utils()).thenReturn(mock(InternalPolarionUtils.class));
        ModelObjectsBase modelObjectsBase = mock(ModelObjectsBase.class);
        ModelObjectsSearch modelObjectsSearch = mock(ModelObjectsSearch.class);
        lenient().when(modelObjectsSearch.query(null)).thenReturn(modelObjectsSearch);
        lenient().when(modelObjectsSearch.sort("name")).thenReturn(modelObjectsSearch);
        lenient().when(modelObjectsBase.search()).thenReturn(modelObjectsSearch);
        lenient().when(internalReadOnlyTransaction.byEnum(any())).thenReturn(modelObjectsBase);
        lenient().when(context.transaction()).thenReturn(internalReadOnlyTransaction);

        lenient().when(params.sourceParams()).thenReturn(mock(DataSetWidgetParams.class));

        lenient().when(scope.isGlobal()).thenReturn(false);
        return new CollectionsDiffWidgetRenderer(context, params, polarionService);
    }
}
