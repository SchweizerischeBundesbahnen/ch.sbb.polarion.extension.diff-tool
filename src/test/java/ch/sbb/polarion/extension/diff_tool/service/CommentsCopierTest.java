package ch.sbb.polarion.extension.diff_tool.service;

import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.model.IComment;
import com.polarion.alm.tracker.model.ICommentBase;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IModuleComment;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.types.Text;
import com.polarion.platform.security.ISecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.security.PrivilegedAction;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"unchecked", "rawtypes"})
class CommentsCopierTest {

    @Mock
    private PolarionService polarionService;
    @Mock
    private ISecurityService securityService;

    private CommentsCopier commentsCopier;

    @BeforeEach
    void init() {
        commentsCopier = new CommentsCopier(polarionService);
        when(polarionService.getSecurityService()).thenReturn(securityService);
        when(securityService.doAsSystemUser(any(PrivilegedAction.class)))
                .thenAnswer(invocation -> ((PrivilegedAction<?>) invocation.getArgument(0)).run());
    }

    @Test
    void testCommentsOfAWorkItemAreCopiedOntoItsCopyWithTheirIdsMapped() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem targetWorkItem = mock(IWorkItem.class);
        IComment sourceComment = comment(IComment.class, "5", "Please check this");
        when(sourceWorkItem.getRootComments(true)).thenReturn(new PObjectListStub(List.of(sourceComment)));
        IComment createdComment = comment(IComment.class, "17", "Please check this");
        when(targetWorkItem.createComment(sourceComment.getText())).thenReturn(createdComment);

        Map<String, String> idMapping = commentsCopier.copyComments(sourceWorkItem, targetWorkItem);

        assertEquals(Map.of("5", "17"), idMapping);
        verify(createdComment).save();
    }

    @Test
    void testACopiedCommentKeepsWhoWroteItAndWhen() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem targetWorkItem = mock(IWorkItem.class);
        IUser author = mock(IUser.class);
        Date created = new Date();
        IComment sourceComment = comment(IComment.class, "5", "Please check this");
        when(sourceComment.getAuthor()).thenReturn(author);
        when(sourceComment.getCreated()).thenReturn(created);
        when(sourceWorkItem.getRootComments(true)).thenReturn(new PObjectListStub(List.of(sourceComment)));
        IComment createdComment = comment(IComment.class, "17", "Please check this");
        when(targetWorkItem.createComment(any())).thenReturn(createdComment);

        commentsCopier.copyComments(sourceWorkItem, targetWorkItem);

        verify(createdComment).setValue(ICommentBase.KEY_CREATED, created);
        // only a system user may say who wrote a comment
        verify(createdComment).setValue(ICommentBase.KEY_AUTHOR, author);
        verify(securityService).doAsSystemUser(any(PrivilegedAction.class));
    }

    @Test
    void testAWholeCommentThreadIsCopied() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem targetWorkItem = mock(IWorkItem.class);
        IComment sourceRoot = comment(IComment.class, "5", "Please check this");
        IComment sourceReply = comment(IComment.class, "6", "Checked");
        when(sourceRoot.getChildComments()).thenReturn(new PObjectListStub(List.of(sourceReply)));
        when(sourceRoot.isResolvedComment()).thenReturn(true);
        when(sourceWorkItem.getRootComments(true)).thenReturn(new PObjectListStub(List.of(sourceRoot)));

        IComment createdRoot = comment(IComment.class, "17", "Please check this");
        IComment createdReply = comment(IComment.class, "18", "Checked");
        when(targetWorkItem.createComment(sourceRoot.getText())).thenReturn(createdRoot);
        when(createdRoot.createChildComment(sourceReply.getText())).thenReturn(createdReply);

        Map<String, String> idMapping = commentsCopier.copyComments(sourceWorkItem, targetWorkItem);

        assertEquals(Map.of("5", "17", "6", "18"), idMapping);
        // a thread is resolved as a whole, so the flag is set once the replies are there
        InOrder order = inOrder(createdReply, createdRoot);
        order.verify(createdReply).save();
        order.verify(createdRoot).setResolvedComment(true);
    }

    @Test
    void testOnlyTheAskedForCommentsOfADocumentAreCopied() {
        IModule sourceModule = mock(IModule.class);
        IModule targetModule = mock(IModule.class);
        IModuleComment wanted = comment(IModuleComment.class, "5", "About this paragraph");
        IModuleComment unwanted = comment(IModuleComment.class, "9", "About another paragraph");
        when(sourceModule.getRootComments(true)).thenReturn(new PObjectListStub(List.of(wanted, unwanted)));
        IModuleComment createdComment = comment(IModuleComment.class, "17", "About this paragraph");
        when(targetModule.createComment(wanted.getText())).thenReturn(createdComment);

        Map<String, String> idMapping = commentsCopier.copyComments(sourceModule, targetModule, comment -> "5".equals(comment.getId()), null);

        assertEquals(Map.of("5", "17"), idMapping);
        verify(targetModule, never()).createComment(unwanted.getText());
    }

    @Test
    void testAnObjectWithoutCommentsGetsNone() {
        IWorkItem sourceWorkItem = mock(IWorkItem.class);
        IWorkItem targetWorkItem = mock(IWorkItem.class);
        when(sourceWorkItem.getRootComments(true)).thenReturn(new PObjectListStub(List.of()));

        assertTrue(commentsCopier.copyComments(sourceWorkItem, targetWorkItem).isEmpty());
        verify(targetWorkItem, never()).createComment(any());
    }

    private <T extends ICommentBase<T>> T comment(Class<T> commentClass, String id, String text) {
        T comment = mock(commentClass);
        when(comment.getId()).thenReturn(id);
        when(comment.getText()).thenReturn(Text.plain(text));
        return comment;
    }
}
