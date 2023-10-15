package com.github.samblake.terser.plugin.minifier;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Paths;
import java.util.Optional;

import static com.github.samblake.terser.plugin.TestUtils.getBasePath;
import static com.github.samblake.terser.plugin.TestUtils.getSourceMapPath;
import static com.github.samblake.terser.plugin.TestUtils.getTerserPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TerserMinifierTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Log log = new SystemStreamLog();

    private com.github.samblake.terser.plugin.minifier.ImmutableMinificationContext.Builder contextBuilder = com.github.samblake.terser.plugin.minifier.ImmutableMinificationContext.builder().
            isVerbose(true)
            .log(log)
            .terserSource(getTerserPath().toFile())
            .charset(UTF_8);

    @Test
    public void shouldMinifyEs6File() {
        //given
        Minification minification = com.github.samblake.terser.plugin.minifier.ImmutableMinification.builder()
                .source(getBasePath().resolve(Paths.get("src", "a", "test-es6.js")))
                .target(Paths.get("foo"))
                .context(contextBuilder.options("{}").build())
                .build();
        //when
        minification = new TerserMinifier().execute(minification);
        //then
        assertThat(minification.getResult()).get().isEqualTo(
                "let numbers=[4,9,16,25,29],first=numbers.find(myFunction);function myFunction(n,u,t){return value>18}");
    }

    @Test
    public void shouldMinifyToplevelEs6File() {
        //given
        Minification minification = com.github.samblake.terser.plugin.minifier.ImmutableMinification.builder()
                .source(getBasePath().resolve(Paths.get("src", "a", "test-es6.js")))
                .target(Paths.get("foo"))
                .context(contextBuilder.options("{toplevel:true}").build())
                .build();
        //when
        minification = new TerserMinifier().execute(minification);
        //then
        assertThat(minification.getResult()).get().isEqualTo(
                "[4,9,16,25,29].find((function(n,u,e){return value>18}));");
    }

    @Test
    public void shouldMinifyNullishCoalescing() {
        //given
        Minification minification = com.github.samblake.terser.plugin.minifier.ImmutableMinification.builder()
                .source(getBasePath().resolve(Paths.get("src", "a", "test-nullish.js")))
                .target(Paths.get("foo"))
                .context(contextBuilder.options("{}").build())
                .build();
        //when
        minification = new TerserMinifier().execute(minification);
        //then
        assertThat(minification.getResult()).get().isEqualTo(
                "function foo(n){return n??1}");
    }

    @Test
    public void shouldGenerateSourceMaps() {
        //given
        Minification minification = com.github.samblake.terser.plugin.minifier.ImmutableMinification.builder()
                .source(getBasePath().resolve(Paths.get("src", "a", "test-es6.js")))
                .target(Paths.get("foo"))
                .context(contextBuilder
                        .sourceMapSource(Optional.of(getSourceMapPath().toFile()))
                        .options("{sourceMap: true}")
                        .build())
                .build();
        //when
        minification = new TerserMinifier().execute(minification);
        //then
        assertThat(minification.getSourceMap()).get().isEqualTo(
                "{\"version\":3,\"sources\":[\"0\"],\"names\":[\"numbers\",\"first\",\"find\",\"myFunction\",\"index\",\"array\",\"value\"],\"mappings\":"
                        + "\"AAAA,IAAIA,QAAU,CAAC,EAAG,EAAG,GAAI,GAAI,IACzBC,MAAQD,QAAQE,KAAKC,YAEzB,SAASA,WAAWF,EAAOG,EAAOC,GAC9B,OAAOC,MAAQ,EACnB\"}");
    }

}