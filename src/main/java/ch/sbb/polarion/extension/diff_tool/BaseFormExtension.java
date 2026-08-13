package ch.sbb.polarion.extension.diff_tool;

import ch.sbb.polarion.extension.diff_tool.rest.model.HandleReferencesType;
import ch.sbb.polarion.extension.diff_tool.service.PolarionService;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.SharedContext;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.utils.html.HtmlFragmentBuilder;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.IFormExtensionContext;
import com.polarion.core.util.EscapeChars;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.persistence.model.IPObject;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BaseFormExtension implements IFormExtension {
    private static final Logger logger = Logger.getLogger(BaseFormExtension.class);

    /**
     * The single placeholder in the fragment HTML. It sits in a {@code data-props} attribute the React
     * panel reads with {@code JSON.parse(host.dataset.props)}; see ui/src/formext/panelProps.ts.
     */
    private static final String PANEL_PROPS_PLACEHOLDER = "{PANEL_PROPS}";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String htmlFileName;
    private final String formExtensionDefaultLabel;
    private final boolean allowEmptyLinkRole;
    PolarionService polarionService = new PolarionService();

    public BaseFormExtension(String htmlFileName, String formExtensionDefaultLabel, boolean allowEmptyLinkRole) {
        this.htmlFileName = htmlFileName;
        this.formExtensionDefaultLabel = formExtensionDefaultLabel;
        this.allowEmptyLinkRole = allowEmptyLinkRole;
    }

    /** An id/label pair for one dropdown entry. */
    public record IdName(String id, String name) {
    }

    /** One "Referenced workitems" behaviour; {@code description} becomes the option's tooltip. */
    public record HandleReferencesOption(String id, String title, String description) {
    }

    /**
     * Everything the panel needs that is already known here, serialised into the fragment in one go.
     * <p>
     * Server-injected rather than fetched by the panel: this fragment is rendered on every document
     * open, so fetching the projects, link roles and configuration names would add three REST
     * round-trips to a pane the user may never expand - and two of those lists have no endpoint. The
     * mirror image of this record is {@code PanelProps} in ui/src/formext/panelProps.ts; the field names
     * are the contract between them.
     */
    public record PanelProps(
            String sourceProjectId,
            String sourceSpaceId,
            String sourceDocument,
            String sourceDocumentTitle,
            String sourceRevision,
            List<IdName> projects,
            List<IdName> linkRoles,
            List<String> configurations,
            List<HandleReferencesOption> handleReferencesTypes) {
    }

    @Override
    @Nullable
    public String render(@NotNull IFormExtensionContext context) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(
                transaction -> renderForm(transaction.context(), context.object().getOldApi()));
    }

    private @NotNull String renderForm(@NotNull SharedContext context, @NotNull IPObject object) {
        HtmlFragmentBuilder builder = context.createHtmlFragmentBuilderFor().gwt();

        if (object instanceof IModule module) {
            String form = ScopeUtils.getFileContent("webapp/diff-tool/html/%s.html".formatted(htmlFileName));
            builder.html(form.replace(PANEL_PROPS_PLACEHOLDER, serializeProps(buildProps(module))));
        }

        builder.finished();
        return builder.toString();
    }

    @VisibleForTesting
    @NotNull PanelProps buildProps(@NotNull IModule module) {
        List<IdName> projects = polarionService.getProjects().stream().map(BaseFormExtension::toIdName).toList();

        List<ILinkRoleOpt> linkRoles = new ArrayList<>(polarionService.getLinkRoles(module.getProjectId()));
        if (allowEmptyLinkRole) {
            linkRoles.add(0, null);
        }

        return new PanelProps(
                module.getProjectId(),
                module.getModuleFolder(),
                module.getModuleName(),
                module.getTitleOrName(),
                Objects.requireNonNullElse(module.getRevision(), ""),
                projects,
                linkRoles.stream().map(BaseFormExtension::toIdName).toList(),
                configurationNames(module.getProjectId()),
                handleReferencesOptions());
    }

    /**
     * The props as an HTML-attribute-safe string. {@link EscapeChars#forHTMLAttribute(String)} turns
     * {@code "} into {@code &quot;} (and {@code '}, {@code <}, {@code >}, {@code &} into their
     * entities), which the browser decodes back to the exact JSON when the panel reads
     * {@code dataset.props}.
     * <p>
     * This replaces the legacy {@code fillParams} helper, which wrapped each value in double quotes with
     * <b>no escaping at all</b> and interpolated the result into a {@code <link onload='...'>} attribute
     * - so a document whose title contained a quote broke out of the attribute and injected script into
     * the Document Properties pane.
     */
    @VisibleForTesting
    static @NotNull String serializeProps(@NotNull PanelProps props) {
        try {
            return EscapeChars.forHTMLAttribute(objectMapper.writeValueAsString(props));
        } catch (JsonProcessingException e) {
            // Rendering an empty-but-valid object keeps the pane usable (empty dropdowns) instead of
            // leaving invalid JSON in the attribute, which would blank the panel entirely.
            logger.error("Could not serialize the diff-tool panel properties", e);
            return "{}";
        }
    }

    private static @NotNull IdName toIdName(@NotNull IProject project) {
        return new IdName(project.getId(), Objects.requireNonNullElse(project.getName(), project.getId()));
    }

    /** {@code null} is the synthetic "no link role" entry copy-tool prepends; it carries an empty id. */
    private static @NotNull IdName toIdName(@Nullable ILinkRoleOpt linkRole) {
        return linkRole == null
                ? new IdName("", "none")
                : new IdName(linkRole.getId(), "%s / %s".formatted(linkRole.getName(), linkRole.getOppositeName()));
    }

    /**
     * Diff configuration names in the document's project scope, or the single default name when the scope
     * defines none. Never empty, so the panel can preselect the first entry.
     */
    private @NotNull List<String> configurationNames(@NotNull String projectId) {
        Collection<SettingName> settingNames = getSettingNames(ScopeUtils.getScopeFromProject(projectId));
        return settingNames.isEmpty()
                ? List.of(NamedSettings.DEFAULT_NAME)
                : settingNames.stream().map(SettingName::getName).toList();
    }

    @SuppressWarnings("unchecked")
    private @NotNull Collection<SettingName> getSettingNames(@NotNull String scope) {
        try {
            return NamedSettingsRegistry.INSTANCE.getByFeatureName(DiffSettings.FEATURE_NAME).readNames(scope);
        } catch (IllegalStateException ex) {
            if ("There is already a transaction.".equals(ex.getMessage())) {
                return Collections.emptyList();
            } else {
                throw ex;
            }
        }
    }

    private static @NotNull List<HandleReferencesOption> handleReferencesOptions() {
        return Arrays.stream(HandleReferencesType.values())
                .map(type -> new HandleReferencesOption(type.name(), type.getTitle(), type.getDescription()))
                .toList();
    }

    @Override
    @Nullable
    public String getIcon(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return null;
    }

    @Override
    @Nullable
    public String getLabel(@NotNull IPObject object, @Nullable Map<String, String> attributes) {
        return ObjectUtils.firstNonNull(attributes == null ? null : attributes.get("label"), formExtensionDefaultLabel);
    }

}
