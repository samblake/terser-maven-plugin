package com.github.samblake.terser.plugin.minifier;

import org.apache.maven.plugin.logging.Log;
import org.immutables.value.Value;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Optional;

@Value.Immutable
public interface MinificationContext {

    File getTerserSource();

    @Value.Default
    default Optional<File> getSourceMapSource() {
        return Optional.empty();
    }

    Charset getCharset();

    Log getLog();

    @Value.Default
    default boolean isVerbose() {
        return false;
    }

    String getOptions();

}
