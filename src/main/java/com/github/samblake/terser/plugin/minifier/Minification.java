package com.github.samblake.terser.plugin.minifier;

import org.immutables.value.Value;

import java.nio.file.Path;
import java.util.Optional;

@Value.Immutable
public interface Minification {
    MinificationContext getContext();

    Path getSource();

    Path getTarget();

    Optional<String> getResult();

    Optional<String> getSourceMap();
}
