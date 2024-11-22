package ch.sbb.polarion.extension.diff_tool.rest.controller;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentDuplicateParams;
import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.CommunicationSettings;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Document;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.DocumentRevision;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.Space;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.WorkItemField;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.util.ExtensionInfo;
import com.polarion.alm.tracker.model.IStatusOpt;

import javax.ws.rs.Path;
import java.util.Collection;
import java.util.List;

@Secured
@Path("/api")
public class UtilityApiController extends UtilityInternalController {
    @Override
    public Collection<Space> getSpaces(String projectId) {
        return polarionService.callPrivileged(() -> super.getSpaces(projectId));
    }

    @Override
    public List<Document> getDocuments(String projectId, String spaceId) {
        return polarionService.callPrivileged(() -> super.getDocuments(projectId, spaceId));
    }

    @Override
    public List<DocumentRevision> getDocumentRevisions(String projectId, String spaceId, String docName) {
        return polarionService.callPrivileged(() -> super.getDocumentRevisions(projectId, spaceId, docName));
    }

    @Override
    public List<WorkItemField> getAllWorkItemFields(String projectId) {
        return polarionService.callPrivileged(() -> super.getAllWorkItemFields(projectId));
    }

    @Override
    public Collection<IStatusOpt> getAllWorkItemStatuses(String projectId) {
        return polarionService.callPrivileged(() -> super.getAllWorkItemStatuses(projectId));
    }

    @Override
    public DocumentIdentifier createDocumentDuplicate(String projectId, String spaceId, String documentName, String revision, DocumentDuplicateParams documentDuplicateParams) {
        return polarionService.callPrivileged(() -> super.createDocumentDuplicate(projectId, spaceId, documentName, revision, documentDuplicateParams));
    }

    @Override
    public CommunicationSettings getCommunicationSettings() {
        return polarionService.callPrivileged(super::getCommunicationSettings);
    }

    @Override
    public ExtensionInfo getExtensionInfo() {
        return polarionService.callPrivileged(super::getExtensionInfo);
    }

}
