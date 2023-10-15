package com.github.samblake.terser.plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class TestUtils {

    public static final String TERSER_JS = "terser-5.21.0.js";
    public static final String SOURCE_MAP_JS = "source-map-0.7.3.js";

    public static Path getBasePath() {
        try {
            return Paths.get(TestUtils.class.getResource("/").toURI());
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getTerserPath() {
        try {
            return Paths.get(TestUtils.class.getResource("/" + TERSER_JS).toURI());
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getSourceMapPath() {
        try {
            return Paths.get(TestUtils.class.getResource("/" + SOURCE_MAP_JS).toURI());
        }
        catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static String getResourceAsString(Class<TestUtils> aClass, String resource) {
        return new BufferedReader(new InputStreamReader(
                aClass.getResourceAsStream(resource)))
                .lines()
                .collect(Collectors.joining("\n"));
    }
}
