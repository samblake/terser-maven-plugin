package com.github.samblake.terser.plugin;

import com.github.samblake.terser.plugin.minifier.Minification;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class MinificationInitializerTest {
    private TerserMojo terserMojo;

    @Before
    public void setUp() {
        terserMojo = new TerserMojo();
        terserMojo.setEncoding("UTF-8");
        terserMojo.setTerserSrc(TestUtils.getTerserPath().toFile());
        terserMojo.setSourceDir(TestUtils.getBasePath().toFile());
        terserMojo.setTargetDir(Paths.get("foo").toFile());
        terserMojo.setOptions("{}}");
    }

    @Test
    public void shouldNotFailForEmptyParameters() {
        //given
        //terserMojo
        //when
        Set<Minification> minifications = new MinificationInitializer(terserMojo).getMinification();
        //then
        assertThat(minifications).isEmpty();
    }

    @Test
    public void shouldGetFilesFromStaticList() {
        //given
        terserMojo.setJsSourceFile("/src/test.js");
        //when
        Set<Minification> minifications = new MinificationInitializer(terserMojo).getMinification();
        //then
        assertThat(getSourceFilesNames(minifications)).containsOnly("test.js");
    }


    @Test
    public void shouldGetFilesFromIncludesList() {
        //given
        terserMojo.setJsSourceInclude("/src/a/test-*.js");
        //when
        Set<Minification> minifications = new MinificationInitializer(terserMojo).getMinification();
        //Then
        assertThat(getSourceFilesNames(minifications)).containsOnly("test-es6.js", "test-nullish.js", "test-async.js");
    }

    @Test
    public void shouldGetFilesFromIncludesListUsingFileSeparator() {
        //given
        terserMojo.setJsSourceInclude(File.separator + "src" + File.separator + "a" + File.separator + "test-*.js");
        //when
        Set<Minification> minifications = new MinificationInitializer(terserMojo).getMinification();
        //Then
        assertThat(getSourceFilesNames(minifications)).containsOnly("test-es6.js", "test-nullish.js", "test-async.js");
    }

    @Test
    public void shouldGetFilesFromIncludesListApplyingExclude() {
        //Given
        terserMojo.setJsSourceInclude("/src/a/test-*.js");
        terserMojo.setJsSourceExclude("/src/a/*nullish*");
        //when
        Set<Minification> minifications = new MinificationInitializer(terserMojo).getMinification();
        //Then
        assertThat(getSourceFilesNames(minifications)).containsOnly("test-es6.js", "test-async.js");
    }

    @Test
    public void shouldGetFilesFromAllParameters() {
        //given
        terserMojo.setJsSourceFile("/src/test.js");
        terserMojo.setJsSourceInclude("/src/a/test-*.js");
        terserMojo.setJsSourceExclude("/src/a/*es6.js");
        //when
        Set<Minification> minifications = new MinificationInitializer(terserMojo).getMinification();
        //then
        assertThat(getSourceFilesNames(minifications)).containsOnly("test.js", "test-nullish.js", "test-async.js");
    }

    @Test
    public void shouldMapRelatively() {
        //given
        Path targetDirectory = Paths.get("some", "target", "path");
        terserMojo.setTargetDir(targetDirectory.toFile());
        terserMojo.setJsSourceFile("/src/test.js");
        MinificationInitializer minificationInitializer = new MinificationInitializer(terserMojo);
        //when
        Set<Minification> minifications = minificationInitializer.getMinification();
        //then
        Path targetFile = minifications.iterator().next().getTarget();
        assertThat(targetDirectory.relativize(targetFile)).isEqualTo(Paths.get("src", "test.js"));
    }

    @Test
    public void shouldAddSuffix() {
        //given
        terserMojo.setJsSourceFile("/src/test.js");
        terserMojo.setSuffix("min");
        //when
        Set<Minification> minifications = new MinificationInitializer(terserMojo).getMinification();
        //then
        String fileName = minifications.iterator().next().getTarget().getFileName().toString();
        assertThat(fileName).isEqualTo("test.min.js");
    }

    private Stream<String> getSourceFilesNames(Set<Minification> minifications) {
        return minifications.parallelStream()
                .map(Minification::getSource)
                .map(Path::getFileName)
                .map(Path::toString);
    }

}