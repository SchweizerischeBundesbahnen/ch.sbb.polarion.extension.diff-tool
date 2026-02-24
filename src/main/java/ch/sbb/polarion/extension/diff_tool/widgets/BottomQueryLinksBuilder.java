package ch.sbb.polarion.extension.diff_tool.widgets;

import com.polarion.alm.shared.api.model.PrototypeEnum;
import com.polarion.alm.shared.api.model.rp.parameter.DataSet;
import com.polarion.alm.shared.api.model.rp.widget.RichPageWidgetCommonContext;
import com.polarion.alm.shared.api.utils.html.HtmlContentBuilder;
import com.polarion.alm.shared.api.utils.html.HtmlTagBuilder;
import com.polarion.alm.shared.api.utils.links.HtmlLinkFactory;
import com.polarion.alm.shared.api.utils.links.PortalLink;
import com.polarion.alm.shared.util.StringUtils;
import org.jetbrains.annotations.NotNull;

public class BottomQueryLinksBuilder {
    private final PrototypeEnum allowedPrototype;
    private final DataSet dataSet;
    private final RichPageWidgetCommonContext context;
    private final int topItems;
    private boolean withShortMessages = false;

    public BottomQueryLinksBuilder(@NotNull RichPageWidgetCommonContext context, @NotNull PrototypeEnum allowedPrototype, @NotNull DataSet dataSet, int topItems) {
        this.context = context;
        this.allowedPrototype = allowedPrototype;
        this.dataSet = dataSet;
        this.topItems = topItems;
    }

    @NotNull
    public BottomQueryLinksBuilder withShortMessages() {
        this.withShortMessages = true;
        return this;
    }

    public void render(@NotNull HtmlTagBuilder tagBuilder) {
        tagBuilder.attributes().className("polarion-rpw-table-footer");
        HtmlContentBuilder contentBuilder = tagBuilder.append();
        this.renderCounts(contentBuilder);
        if (!this.isInCollectionScope()) {
            this.renderOpenInTable(contentBuilder);
        }

        this.renderShowQuery(contentBuilder);
    }

    private boolean isInCollectionScope() {
        return allowedPrototype == PrototypeEnum.BaselineCollection;
    }

    private void renderCounts(@NotNull HtmlContentBuilder builder) {
        PortalLink link = this.dataSet.openInTableLink();
        HtmlTagBuilder tag = builder.tag().div();
        tag.attributes().className("polarion-rpw-table-counts");
        if (link != null) {
            tag = tag.append().tag().a();
            tag.attributes().href(link).target("_top");
        }

        if (this.withShortMessages) {
            this.renderShortCountsMessage(tag.append());
        } else {
            this.renderCountsMessage(tag.append());
        }

    }

    private void renderCountsMessage(@NotNull HtmlContentBuilder builder) {
        String message;
        if (this.dataSet.size() <= this.topItems) {
            message = this.context.localization().getString("form.modules.label.showMulti.item", "" + this.dataSet.size());
        } else {
            message = this.context.localization().getString("form.modules.label.showMultiOf.item", "" + this.topItems, "" + this.dataSet.size());
        }

        builder.text(message);
    }

    private void renderShortCountsMessage(@NotNull HtmlContentBuilder builder) {
        builder.text(this.context.localization().getString("richpages.widget.kanbanBoard.showMoreThenTopItems", "" + this.topItems, "" + this.dataSet.size()));
    }

    private void renderOpenInTable(@NotNull HtmlContentBuilder builder) {
        PortalLink link = this.dataSet.openInTableLink();
        if (link != null) {
            HtmlTagBuilder div = builder.tag().div();
            div.attributes().className("polarion-rpw-table-open-in-table");
            HtmlTagBuilder a = div.append().tag().a();
            a.attributes().href(link).target("_blank");
            a.append().tag().img().attributes().src(HtmlLinkFactory.fromEncodedRelativeUrl("/polarion/ria/images/portlet/portletOpenInTable.png"))
                    .title(this.context.localization().getString("macro.general.openInTable"));
        }
    }

    private void renderShowQuery(@NotNull HtmlContentBuilder builder) {
        if (!this.context.target().isPdf()) {
            if (!StringUtils.isEmptyTrimmed(this.dataSet.queryToShow())) {
                String uid = this.context.generateUniqueElementId();
                HtmlTagBuilder tag = builder.tag().div();
                tag.attributes().className("polarion-rpw-table-show-query");
                tag.append().tag().img().attributes().src(HtmlLinkFactory.fromEncodedRelativeUrl("/polarion/ria/images/portlet/info.png"))
                        .title(this.context.localization().getString("macro.general.showQuery"))
                        .onClick("var style = getElementById('" + uid + "').style; style.display = (style.display == 'block') ? 'none' : 'block';");
                HtmlTagBuilder div = builder.tag().div();
                div.attributes().id(uid).className("polarion-rpw-table-query");
                div.append().textWithFormatting(this.dataSet.queryToShow());
            }
        }
    }

}
