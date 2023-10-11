package com.github.samblake.terser.plugin.minifier;

import java.util.Set;
import java.util.stream.Stream;

public interface TerserMinificationStrategy {
    Stream<Minification> execute(Set<Minification> minifications);
}