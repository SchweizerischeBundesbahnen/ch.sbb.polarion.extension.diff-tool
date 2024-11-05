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

    @Override
    public @NotNull List<String> getSupportedProperties() {
        List<String> supportedProperties = new ArrayList<>(super.getSupportedProperties());
        supportedProperties.add(CHUNK_SIZE);
        return supportedProperties;
    }

    public static DiffToolExtensionConfiguration getInstance() {
        return (DiffToolExtensionConfiguration) CurrentExtensionConfiguration.getInstance().getExtensionConfiguration();
    }
}
