package ch.sbb.polarion.extension.diff_tool.util;

import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.internal.url.IUrlResolver;
import com.polarion.alm.tracker.internal.url.PolarionUrlResolver;
import com.polarion.core.util.StreamUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.internal.ExecutionThreadMonitor;

import java.io.IOException;
import java.io.InputStream;

public class ResourceProvider {

    private static final Logger logger = Logger.getLogger(ResourceProvider.class);

    public byte[] getResourceAsBytes(String resource) {
        // Non-default icons are getting via project and thus requires open transaction
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            try {
                return getResourceAsBytesImpl(resource);
            } catch (Exception e) {
                logger.error("Error loading resource '" + resource, e);
            } finally {
                ExecutionThreadMonitor.checkForInterruption();
            }
            return new byte[0];
        });
    }

    private byte[] getResourceAsBytesImpl(String resource) throws IOException {
        IUrlResolver resolver = PolarionUrlResolver.getInstance();
        if (resolver.canResolve(resource)) {
            InputStream stream = resolver.resolve(resource);
            if (stream != null) {
                byte[] result = StreamUtils.suckStreamThenClose(stream);
                if (result.length > 0) {
                    return result;
                }
            }
        }
        return new byte[0];
    }
}
