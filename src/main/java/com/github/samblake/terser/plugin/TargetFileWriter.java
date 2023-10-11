package com.github.samblake.terser.plugin;

import com.github.samblake.terser.plugin.minifier.Minification;
import org.apache.maven.plugin.logging.Log;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

class TargetFileWriter {

    static void writeTargetFile(final Minification minification) {
        writeTargetFile(minification, Minification::getResult, Minification::getTarget);
    }

    static void writeMapFile(final Minification minification) {
        writeTargetFile(minification, Minification::getResult, m -> {
            String fileName = m.getTarget().getFileName().toString();
            Path parent = m.getTarget().getParent();
            return parent.resolve(fileName + ".map");
        });
    }

    private static void writeTargetFile(final Minification minification,
            Function<Minification, Optional<String>> contentLoader,
            Function<Minification, Path> targetLoader) {

        final Log log = minification.getContext().getLog();
        final Charset charset = minification.getContext().getCharset();

        try {
            Path target = targetLoader.apply(minification);

            log.debug(format("writing to %s", target));
            Files.createDirectories(target.getParent());

            final byte[] bytes = contentLoader.apply(minification)
                    .orElseThrow(() -> new IllegalStateException(
                            "No result for minification. Cannot write minification (" + minification + ")"))
                    .getBytes(charset);

            Files.write(target, bytes);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
