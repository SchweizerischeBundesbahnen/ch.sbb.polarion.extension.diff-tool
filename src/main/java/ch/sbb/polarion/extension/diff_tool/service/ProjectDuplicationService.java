package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import com.polarion.alm.projects.IProjectLifecycleManager;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IProject;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.ILogger;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * Duplicates a Polarion project by SVN-copying it into the repo-based template area, then
 * delegating to {@link IProjectLifecycleManager#createProject} which uses Polarion's
 * built-in {@code TemplatePrefixProcessor} to rewrite work item IDs and inter-WI references
 * to the new tracker prefix. The temporary template is removed afterwards.
 */
public class ProjectDuplicationService {

    private static final Logger logger = Logger.getLogger(ProjectDuplicationService.class);

    // Polarion internal constants from com.polarion.alm.projects.internal.ProjectLifecycleManager
    static final String TEMPLATES_ROOT_PATH = "/.polarion/projects/templates/";
    static final String TEMPLATE_PROP_FILE = "template.properties";
    static final String TEMPLATE_LOCATION_REPO = "repo";
    static final String PROP_TEMPLATE_LOCATION = "template-location";
    static final String PROP_TRACKER_PREFIX_TO_REPLACE = "trackerPrefixToReplace";
    static final String PROP_PROCESS = "process";
    static final String PARAM_TRACKER_PREFIX = "tracker-prefix";
    static final String PARAM_TEMPLATE_TRACKER_PREFIX_REPLACE = "trackerPrefixToReplace";
    static final String PARAM_PROJECT_NAME = "project-name";

    static final String EPHEMERAL_TEMPLATE_PREFIX = "_diff_tool_dup_";

    private final IProjectService projectService;
    private final ITrackerService trackerService;
    private final IRepositoryService repositoryService;

    public ProjectDuplicationService() {
        this(PlatformContext.getPlatform().lookupService(IProjectService.class),
                PlatformContext.getPlatform().lookupService(ITrackerService.class),
                PlatformContext.getPlatform().lookupService(IRepositoryService.class));
    }

    public ProjectDuplicationService(@NotNull IProjectService projectService,
                                     @NotNull ITrackerService trackerService,
                                     @NotNull IRepositoryService repositoryService) {
        this.projectService = projectService;
        this.trackerService = trackerService;
        this.repositoryService = repositoryService;
    }

    public void duplicate(@NotNull DuplicationRequest request) {
        duplicate(request, null);
    }

    /**
     * @param progressLog optional logger that receives one info message per phase. If supplied,
     *                    these messages also appear in the Polarion job log/report. Pass {@code null}
     *                    when calling outside of a job context.
     */
    public void duplicate(@NotNull DuplicationRequest request, @Nullable ILogger progressLog) {
        long t0 = System.currentTimeMillis();
        phase(progressLog, "[1/5] Validating request and resolving source project '" + request.getSourceProjectId() + "'");
        Context ctx = validate(request);
        phase(progressLog, "      Source location: " + ctx.sourceLocation
                + " ; source tracker prefix: '" + ctx.sourcePrefixWithoutDash + "-'"
                + " ; new tracker prefix: '" + ctx.newPrefixWithoutDash + "-'");

        String templateId = EPHEMERAL_TEMPLATE_PREFIX + UUID.randomUUID().toString().replace("-", "");
        ILocation templateLocation = Location.getLocationWithRepository(IRepositoryService.DEFAULT, TEMPLATES_ROOT_PATH + templateId);
        ILocation targetLocation = Location.getLocationWithRepository(IRepositoryService.DEFAULT, request.getLocation());

        boolean[] templateCreated = {false};
        try {
            phase(progressLog, "[2/5] SVN-copying source project to ephemeral template " + templateLocation.getLocationPath());
            long t1 = System.currentTimeMillis();
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                repositoryService.getConnection(templateLocation).copy(ctx.sourceLocation, templateLocation, false);
                return null;
            });
            templateCreated[0] = true;
            phase(progressLog, "      Template copy committed in " + secs(t1) + " s");

            phase(progressLog, "[3/5] Writing template metadata (template.properties)");
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                writeTemplateProperties(repositoryService.getConnection(templateLocation), templateLocation, templateId, ctx);
                return null;
            });

            Map<String, String> params = new HashMap<>();
            params.put(PARAM_TRACKER_PREFIX, ctx.newPrefixWithoutDash);
            params.put(PARAM_TEMPLATE_TRACKER_PREFIX_REPLACE, ctx.sourcePrefixWithoutDash);
            params.put(PARAM_PROJECT_NAME, request.getTargetProjectId());

            phase(progressLog, "[4/5] Creating project '" + request.getTargetProjectId() + "' at "
                    + request.getLocation() + " from template (renames work item IDs '"
                    + ctx.sourcePrefixWithoutDash + "-*' -> '" + ctx.newPrefixWithoutDash + "-*' and updates references)"
                    + " - this is the longest phase and may take several minutes for large projects");
            long t4 = System.currentTimeMillis();
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                projectService.getLifecycleManager().createProject(targetLocation, request.getTargetProjectId(), templateId, params);
                return null;
            });
            phase(progressLog, "      Project created in " + secs(t4) + " s");
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Project duplication failed: " + e.getMessage(), e);
        } finally {
            if (templateCreated[0]) {
                phase(progressLog, "[5/5] Removing ephemeral template " + templateLocation.getLocationPath());
                deleteTemplateQuietly(templateLocation, templateId, progressLog);
            }
        }
        phase(progressLog, "Done in " + secs(t0) + " s. Open the new project: " + projectUrl(request.getTargetProjectId()));
    }

    public static @NotNull String projectUrl(@NotNull String projectId) {
        String baseUrl = System.getProperty("base.url", "");
        // base.url is configured in polarion.properties without trailing slash, e.g. http://localhost
        return (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl)
                + "/polarion/#/project/" + projectId;
    }

    private void writeTemplateProperties(@NotNull IRepositoryConnection connection,
                                         @NotNull ILocation templateLocation,
                                         @NotNull String templateId,
                                         @NotNull Context ctx) {
        Properties props = new Properties();
        props.setProperty(IProjectLifecycleManager.PROP_NAME, templateId);
        props.setProperty(IProjectLifecycleManager.PROP_DESCR,
                "Ephemeral template created by Diff Tool to duplicate project " + ctx.sourceProjectId);
        props.setProperty(PROP_TEMPLATE_LOCATION, TEMPLATE_LOCATION_REPO);
        props.setProperty(PROP_TRACKER_PREFIX_TO_REPLACE, ctx.sourcePrefixWithoutDash);
        // Forces TemplatePrefixProcessor.handleTrackerPrefixParam to rewrite the <trackerPrefix>
        // field in polarion-project.xml of the new project to the new prefix.
        props.setProperty(PROP_PROCESS, ".polarion/polarion-project.xml");

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            props.store(bos, "Diff Tool ephemeral project-duplication template");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize template.properties", e);
        }

        ILocation propLoc = templateLocation.append(TEMPLATE_PROP_FILE);
        connection.create(propLoc, new ByteArrayInputStream(bos.toByteArray()));
    }

    private @NotNull Context validate(@NotNull DuplicationRequest request) {
        require(request.getSourceProjectId(), "sourceProjectId");
        require(request.getTargetProjectId(), "targetProjectId");
        require(request.getLocation(), "location");
        require(request.getTrackerPrefix(), "trackerPrefix");

        IProject sourceProject = projectService.getProject(request.getSourceProjectId());
        if (sourceProject == null || sourceProject.isUnresolvable()) {
            throw new IllegalArgumentException("Source project '" + request.getSourceProjectId() + "' does not exist");
        }
        IProject existing = projectService.getProject(request.getTargetProjectId());
        if (existing != null && !existing.isUnresolvable()) {
            throw new IllegalArgumentException("Target project '" + request.getTargetProjectId() + "' already exists");
        }

        ITrackerProject sourceTracker = trackerService.getTrackerProject(sourceProject);
        String sourcePrefix = sourceTracker.getTrackerPrefix();
        if (StringUtils.isEmptyTrimmed(sourcePrefix)) {
            throw new IllegalStateException("Source project '" + request.getSourceProjectId() + "' has no tracker prefix");
        }

        Context ctx = new Context();
        ctx.sourceProjectId = request.getSourceProjectId();
        ctx.sourceLocation = sourceProject.getLocation();
        ctx.sourcePrefixWithoutDash = stripTrailingDash(sourcePrefix);
        ctx.newPrefixWithoutDash = stripTrailingDash(request.getTrackerPrefix());
        return ctx;
    }

    private static String stripTrailingDash(@NotNull String prefix) {
        String trimmed = prefix.trim();
        return trimmed.endsWith("-") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static void require(String value, String name) {
        if (StringUtils.isEmptyTrimmed(value)) {
            throw new IllegalArgumentException("Parameter '" + name + "' must be provided");
        }
    }

    private void deleteTemplateQuietly(@NotNull ILocation templateLocation, @NotNull String templateId, @Nullable ILogger progressLog) {
        try {
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                IRepositoryConnection connection = repositoryService.getConnection(templateLocation);
                if (connection.exists(templateLocation)) {
                    connection.delete(templateLocation);
                }
                return null;
            });
        } catch (Exception e) {
            String msg = "Failed to remove ephemeral template '" + templateId + "': " + e.getMessage();
            logger.warn(msg);
            if (progressLog != null) {
                // ILogger has no warn() — surface as info to keep the job overall OK.
                progressLog.info("WARNING: " + msg);
            }
        }
    }

    private static void phase(@Nullable ILogger progressLog, @NotNull String message) {
        logger.info(message);
        if (progressLog != null) {
            progressLog.info(message);
        }
    }

    private static String secs(long sinceEpochMs) {
        return String.format("%.1f", (System.currentTimeMillis() - sinceEpochMs) / 1000.0);
    }

    private static class Context {
        String sourceProjectId;
        ILocation sourceLocation;
        String sourcePrefixWithoutDash;
        String newPrefixWithoutDash;
    }
}
