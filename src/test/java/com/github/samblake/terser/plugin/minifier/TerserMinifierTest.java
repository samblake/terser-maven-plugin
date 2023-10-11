package com.github.samblake.terser.plugin.minifier;

import com.github.samblake.terser.plugin.TestUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class TerserMinifierTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private Log log = new SystemStreamLog();

    private ImmutableMinificationContext.Builder contextBuilder = ImmutableMinificationContext.builder()
            .isVerbose(true)
            .log(log)
            .terserSource(TestUtils.getTerserPath().toFile())
            .charset(UTF_8);

    @Test
    public void shouldMinifyEs6File() {
        //given
        Minification minification = ImmutableMinification.builder()
                .source(TestUtils.getBasePath().resolve(Paths.get("src", "a", "test-es6.js")))
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
        Minification minification = ImmutableMinification.builder()
                .source(TestUtils.getBasePath().resolve(Paths.get("src", "a", "test-es6.js")))
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
        Minification minification = ImmutableMinification.builder()
                .source(TestUtils.getBasePath().resolve(Paths.get("src", "a", "test-nullish.js")))
                .target(Paths.get("foo"))
                .context(contextBuilder.options("{}").build())
                .build();
        //when
        minification = new TerserMinifier().execute(minification);
        //then
        assertThat(minification.getResult()).get().isEqualTo(
                "function foo(n){return n??1}");
    }

    /*@Test
    public void shouldGenerateSourceMaps() {
        //given
        Minification minification = ImmutableMinification.builder()
                .source(TestUtils.getBasePath().resolve(Paths.get("src", "a", "test-es6.js")))
                .target(Paths.get("foo"))
                .context(contextBuilder.options("{sourceMap: {\n"
                        + "        filename: \"out.js\",\n"
                        + "        url: \"out.js.map\"\n"
                        + "    }}").build())
                .build();
        //when
        minification = new TerserMinifier().execute(minification);
        //then
        assertThat(minification.getSourceMap()).get().isEqualTo(
                "[4,9,16,25,29].find((function(n,u,e){return value>18}));");
    }*/

}