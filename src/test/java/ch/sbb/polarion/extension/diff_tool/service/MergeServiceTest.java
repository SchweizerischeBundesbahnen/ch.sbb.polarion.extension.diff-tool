package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.diff_tool.rest.model.DocumentIdentifier;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeDirection;
import ch.sbb.polarion.extension.diff_tool.rest.model.diff.MergeResult;
import ch.sbb.polarion.extension.diff_tool.settings.DiffSettings;
import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("diff-tool")
public class MergeServiceTest {
    @Mock
    private PolarionService polarionService;
    @Mock
    private SettingsService settingsService;

    private MergeService mergeService;

    @BeforeEach
    void init() {
        mergeService = new MergeService(polarionService);
    }

    @Test
    void testMergeFailedDueToStructuralChanges() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));

        DocumentIdentifier documentIdentifier1 = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("doc1").revision("rev1").moduleXmlRevision("rev1").build();
        DocumentIdentifier documentIdentifier2 = DocumentIdentifier.builder().projectId("project2").spaceId("space2").name("doc2").revision("rev2").moduleXmlRevision("rev2").build();

        IModule module1 = mock(IModule.class);
        ILocation location1 = mock(ILocation.class);
        when(location1.getLocationPath()).thenReturn("space1/doc1");
        when(module1.getModuleLocation()).thenReturn(location1);
        when(polarionService.getModule(documentIdentifier1)).thenReturn(module1);

        IModule module2 = mock(IModule.class);
        ILocation location2 = mock(ILocation.class);
        when(location2.getLocationPath()).thenReturn("space2/doc2");
        when(module2.getModuleLocation()).thenReturn(location2);
        when(module2.getLastRevision()).thenReturn("rev3");
        when(polarionService.getModule(documentIdentifier2)).thenReturn(module2);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));

        MergeResult mergeResult = mergeService.mergeWorkItems(documentIdentifier1, documentIdentifier2, MergeDirection.LEFT_TO_RIGHT,
                "any", Collections.emptyList(), null, "any", false);
        assertFalse(mergeResult.isSuccess());
        assertTrue(mergeResult.isTargetModuleHasStructuralChanges());
    }

    @Test
    void testMergeFailedDueToNotAuthorized() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new DiffSettings(settingsService)));

        DocumentIdentifier documentIdentifier1 = DocumentIdentifier.builder().projectId("project1").spaceId("space1").name("doc1").revision("rev1").moduleXmlRevision("rev1").build();
        DocumentIdentifier documentIdentifier2 = DocumentIdentifier.builder().projectId("project2").spaceId("space2").name("doc2").revision("rev2").moduleXmlRevision("rev2").build();

        IModule module1 = mock(IModule.class);
        ILocation location1 = mock(ILocation.class);
        when(location1.getLocationPath()).thenReturn("space1/doc1");
        when(module1.getModuleLocation()).thenReturn(location1);
        when(polarionService.getModule(documentIdentifier1)).thenReturn(module1);

        IModule module2 = mock(IModule.class);
        ILocation location2 = mock(ILocation.class);
        when(location2.getLocationPath()).thenReturn("space2/doc2");
        when(module2.getModuleLocation()).thenReturn(location2);
        when(module2.getLastRevision()).thenReturn("rev2");
        when(polarionService.getModule(documentIdentifier2)).thenReturn(module2);
        when(polarionService.getLinkRoleById(anyString(), any())).thenReturn(mock(ILinkRoleOpt.class));

        when(polarionService.userAuthorizedForMerge(any())).thenReturn(false);

        MergeResult mergeResult = mergeService.mergeWorkItems(documentIdentifier1, documentIdentifier2, MergeDirection.LEFT_TO_RIGHT,
                "any", Collections.emptyList(), null, "any", false);
        assertFalse(mergeResult.isSuccess());
        assertTrue(mergeResult.isMergeNotAuthorized());
    }
}
