package ch.sbb.polarion.extension.diff_tool.properties;

import ch.sbb.polarion.extension.generic.properties.CurrentExtensionConfiguration;
import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;
import ch.sbb.polarion.extension.generic.util.Discoverable;
import com.polarion.core.config.impl.SystemValueReader;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Discoverable
public class DiffToolExtensionConfiguration extends ExtensionConfiguration {
    public static final String CHUNK_SIZE = "chunk.size";
    public static final String CHUNK_SIZE_DESCRIPTION = "The number of <a href='#fine-tuning-the-communication-between-polarion-and-diff-tool-extension'>parallel executed requests</a> to the server";
    public static final Integer CHUNK_SIZE_DEFAULT_VALUE = 2;

    public static final String CHAPTER_MERGE_TIMEOUT = "chapter.merge.timeout";
    public static final String CHAPTER_MERGE_TIMEOUT_DESCRIPTION = "Minutes a chapter merge may run before it is given up on";
    public static final Integer CHAPTER_MERGE_TIMEOUT_DEFAULT_VALUE = 60;

    public static final String CHAPTER_MERGE_RESULT_TIMEOUT = "chapter.merge.result.timeout";
    public static final String CHAPTER_MERGE_RESULT_TIMEOUT_DESCRIPTION = "Minutes the result of a finished chapter merge is kept in memory";
    public static final Integer CHAPTER_MERGE_RESULT_TIMEOUT_DEFAULT_VALUE = 30;

    public Integer getChunkSize() {
        return SystemValueReader.getInstance().readInt(getPropertyPrefix() + CHUNK_SIZE, CHUNK_SIZE_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    public String getChunkSizeDescription() {
        return CHUNK_SIZE_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    public Integer getChunkSizeDefaultValue() {
        return CHUNK_SIZE_DEFAULT_VALUE;
    }

    public Integer getChapterMergeTimeout() {
        return SystemValueReader.getInstance().readInt(getPropertyPrefix() + CHAPTER_MERGE_TIMEOUT, CHAPTER_MERGE_TIMEOUT_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    public String getChapterMergeTimeoutDescription() {
        return CHAPTER_MERGE_TIMEOUT_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    public Integer getChapterMergeTimeoutDefaultValue() {
        return CHAPTER_MERGE_TIMEOUT_DEFAULT_VALUE;
    }

    public Integer getChapterMergeResultTimeout() {
        return SystemValueReader.getInstance().readInt(getPropertyPrefix() + CHAPTER_MERGE_RESULT_TIMEOUT, CHAPTER_MERGE_RESULT_TIMEOUT_DEFAULT_VALUE);
    }

    @SuppressWarnings("unused")
    public String getChapterMergeResultTimeoutDescription() {
        return CHAPTER_MERGE_RESULT_TIMEOUT_DESCRIPTION;
    }

    @SuppressWarnings("unused")
    public Integer getChapterMergeResultTimeoutDefaultValue() {
        return CHAPTER_MERGE_RESULT_TIMEOUT_DEFAULT_VALUE;
    }

    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add(CHUNK_SIZE);
        supportedProperties.add(CHAPTER_MERGE_TIMEOUT);
        supportedProperties.add(CHAPTER_MERGE_RESULT_TIMEOUT);
        return supportedProperties;
    }

    public static DiffToolExtensionConfiguration getInstance() {
        return (DiffToolExtensionConfiguration) CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
    }
}
