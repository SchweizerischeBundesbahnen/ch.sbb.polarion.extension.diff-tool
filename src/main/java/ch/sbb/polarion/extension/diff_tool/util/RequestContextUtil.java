package ch.sbb.polarion.extension.diff_tool.util;

import ch.sbb.polarion.extension.generic.rest.filter.AuthenticationFilter;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import lombok.experimental.UtilityClass;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import org.jetbrains.annotations.Nullable;
import javax.security.auth.Subject;

@UtilityClass
public final class RequestContextUtil {

    @Nullable
    public static Subject getUserSubject() {
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            return (Subject) requestAttributes.getRequest().getAttribute(AuthenticationFilter.USER_SUBJECT);
        } else {
            throw new IllegalStateException("Cannot find request attributes in the request context");
        }
    }

    /**
     * Keeps the session of the current user past the response of this request, for work the request only schedules:
     * {@link LogoutFilter} ends that session as soon as the response is written, which is long before a background
     * merge has done anything, and a merge which runs as a user whose session was ended is answered by Polarion
     * with unresolvable objects.
     * <p>
     * The work which was scheduled ends that session itself when it is over.
     */
    public static void keepSessionAlive() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.setAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
        }
    }

    /**
     * Gives the session of the current user back to {@link LogoutFilter}, for a request whose scheduling of the work
     * it wanted to keep that session for came to nothing. Nothing else would end that session: the work which was to
     * end it is not running.
     */
    public static void releaseSession() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            requestAttributes.removeAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST);
        }
    }
}
