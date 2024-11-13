package ch.sbb.polarion.extension.diff_tool.service.cleaners;

import com.polarion.alm.tracker.model.IWorkItem;
import org.jetbrains.annotations.NotNull;

public interface ListCleaner {
    void clean(@NotNull IWorkItem workItem);
}
