package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.security.auth.Subject;
import jakarta.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestContextUtilTest {

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldReturnUserSubject() {
        // Arrange
        ServletRequestAttributes requestAttributes = mock(ServletRequestAttributes.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Subject subject = mock(Subject.class);
        when(requestAttributes.getRequest()).thenReturn(request);
        when(request.getAttribute("user_subject")).thenReturn(subject);
        RequestContextHolder.setRequestAttributes(requestAttributes);

        // Act
        Subject resultSubject = RequestContextUtil.getUserSubject();

        // Assert
        assertThat(resultSubject).isEqualTo(subject);
    }

    @Test
    void shouldThrowIllegalStateExceptionByNullAttributes() {
        assertThatThrownBy(RequestContextUtil::getUserSubject)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("request attributes");
    }

    @Test
    void shouldKeepTheSessionOfTheRequestUserForWorkTheRequestSchedules() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestContextUtil.keepSessionAlive();

        verify(request).setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE);
    }

    @Test
    void shouldGiveTheSessionBackWhenThereIsNothingToKeepItFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestContextUtil.releaseSession();

        verify(request).removeAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT);
    }

    @Test
    void shouldKeepNoSessionOutsideOfARequest() {
        assertThatNoException().isThrownBy(RequestContextUtil::keepSessionAlive);
        assertThatNoException().isThrownBy(RequestContextUtil::releaseSession);
    }
}
