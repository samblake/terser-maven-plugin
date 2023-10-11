package com.github.samblake.terser.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.file.Paths;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class TerserMojoTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void shouldFailForNotExistedTerserPath() throws MojoFailureException, MojoExecutionException {
        // Given
        final TerserMojo terserMojo = getTerserMojo();
        terserMojo.setTerserSrc(TestUtils.getBasePath().resolve("terser.js").toFile());

        // Expect
        expectedException.expect(MojoFailureException.class);

        // When
        terserMojo.execute();
    }

    @Test
    public void shouldDoNothingForMissingSourceFiles() throws MojoFailureException, MojoExecutionException {
        // Given
        final TerserMojo terserMojo = getTerserMojo();
        terserMojo.setJsSourceFiles(Collections.emptyList());
        terserMojo.setJsSourceIncludes(Collections.emptyList());

        // When
        terserMojo.execute();

        // Then
        // Pass
    }

    @Test
    public void shouldRunCompleteExecution() throws MojoFailureException, MojoExecutionException {
        // Given
        final TerserMojo terserMojo = getTerserMojo();

        // When
        terserMojo.execute();

        // Then
        assertThat(Paths.get(System.getProperty("java.io.tmpdir")).resolve(Paths.get("src", "a"))).exists();
    }

    private TerserMojo getTerserMojo() {
        TerserMojo terserMojo = new TerserMojo();
        terserMojo.setVerbose(true);
        terserMojo.setTerserSrc(TestUtils.getTerserPath().toFile());
        terserMojo.setSourceDir(TestUtils.getBasePath().toFile());
        terserMojo.setTargetDir(Paths.get(System.getProperty("java.io.tmpdir")).toFile());
        terserMojo.setJsSourceFile("/src/test.js");
        terserMojo.setJsSourceInclude("/src/a/*.js");
        terserMojo.setJsSourceExclude("/src/a/*nullish.js");
        terserMojo.setSuffix("min");
        terserMojo.setOptions("{}");
        terserMojo.setEncoding("UTF-8");
        return terserMojo;
    }
}