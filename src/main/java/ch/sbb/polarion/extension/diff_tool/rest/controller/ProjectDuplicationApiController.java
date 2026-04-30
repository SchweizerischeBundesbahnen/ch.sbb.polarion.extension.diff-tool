package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationJobInfo;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.DuplicationRequest;
import ch.sbb.polarion.extension.diff_tool.rest.model.duplication.ProjectInfo;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.inject.Singleton;
import javax.ws.rs.Path;
import java.util.List;

@Singleton
@Secured
@Path("/api")
public class ProjectDuplicationApiController extends ProjectDuplicationInternalController {

    @Override
    public List<ProjectInfo> listProjects() {
        return polarionService.callPrivileged(super::listProjects);
    }

    @Override
    public DuplicationJobInfo duplicateProject(DuplicationRequest request) {
        return polarionService.callPrivileged(() -> super.duplicateProject(request));
    }

    @Override
    public List<DuplicationJobInfo> listDuplicationJobs() {
        return polarionService.callPrivileged(super::listDuplicationJobs);
    }
}
