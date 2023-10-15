package com.github.samblake.terser.plugin;

import com.github.samblake.terser.plugin.minifier.ImmutableMinification;
import com.github.samblake.terser.plugin.minifier.ImmutableMinificationContext;
import com.github.samblake.terser.plugin.minifier.Minification;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TargetFileWriterTest {
    private static final Path TMP_DIRECTORY = Paths.get(System.getProperty("java.io.tmpdir"));
    private static final String TEST_INPUT = "test";
    private static final String TEST_MAP = "map";

    @Mock
    private Log log;

    @Test
    public void shouldWriteFile() throws Exception {
        // Given
        ImmutableMinificationContext context = ImmutableMinificationContext.builder()
                .terserSource(new File("/"))
                .options("{}")
                .charset(UTF_8)
                .log(log).build();

        Minification minification = ImmutableMinification.builder()
                .source(Paths.get("foo"))
                .target(TMP_DIRECTORY.resolve(Paths.get("src", "test.js")))
                .result(TEST_INPUT)
                .context(context)
                .build();

        // When
        TargetFileWriter.writeTargetFile(minification);

        // Then
        byte[] bytes = Files.readAllBytes(TMP_DIRECTORY.resolve(Paths.get("src", "test.js")));
        assertThat(bytes).isEqualTo(TEST_INPUT.getBytes());
    }

    @Test
    public void shouldWriteMap() throws Exception {
        // Given
        ImmutableMinificationContext context = ImmutableMinificationContext.builder()
                .terserSource(new File("/"))
                .options("{ sourceMap: true }")
                .charset(UTF_8)
                .log(log).build();

        Minification minification = ImmutableMinification.builder()
                .source(Paths.get("foo"))
                .target(TMP_DIRECTORY.resolve(Paths.get("src", "test.js")))
                .result(TEST_INPUT)
                .sourceMap(TEST_MAP)
                .context(context)
                .build();

        // When
        TargetFileWriter.writeMapFile(minification);

        // Then
        byte[] bytes = Files.readAllBytes(TMP_DIRECTORY.resolve(Paths.get("src", "test.js.map")));
        assertThat(bytes).isEqualTo(TEST_MAP.getBytes());
    }
}