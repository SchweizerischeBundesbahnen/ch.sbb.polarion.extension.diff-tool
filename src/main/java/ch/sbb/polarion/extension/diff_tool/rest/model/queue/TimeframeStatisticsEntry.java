package ch.sbb.polarion.extension.diff_tool.rest.model.queue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.jetbrains.annotations.VisibleForTesting;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Data
public abstract class TimeframeStatisticsEntry {

    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    @JsonIgnore
    final OffsetDateTime rawTimestamp;

    final String timestamp;

    public TimeframeStatisticsEntry() {
        this(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @VisibleForTesting
    public TimeframeStatisticsEntry(OffsetDateTime offsetDateTime) {
        this.rawTimestamp = offsetDateTime;
        this.timestamp = rawTimestamp.format(DATE_FORMATTER);
    }
}
