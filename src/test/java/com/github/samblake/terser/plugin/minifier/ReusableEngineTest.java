package com.github.samblake.terser.plugin.minifier;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.github.samblake.terser.plugin.TestUtils.TERSER_JS;
import static com.github.samblake.terser.plugin.minifier.TerserMinifier.waitForResult;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Locale.US;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;

public class ReusableEngineTest {
    private static final String EXPECTED_RESULT = "var x=function(n,r){return n*r};";

    private final Log log = new SystemStreamLog();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testTerser() throws Exception {
        Stopwatch stopwatch = new Stopwatch();
        InputStream inputStream = ReusableEngineTest.class.getResourceAsStream("/" + TERSER_JS);
        InputStreamReader fileReader = new InputStreamReader(inputStream, UTF_8);

        try (final Context engine = TerserMinifier.createEngine()) {
            engine.eval(Source.newBuilder("js", fileReader, TERSER_JS).build());

            log.info("Initialized script engine in " + stopwatch.stop());

            stopwatch.start();

            String source = new BufferedReader(new InputStreamReader(
                    ReusableEngineTest.class.getResourceAsStream("/src/test.js")))
                    .lines().collect(joining());

            String srcBinding = "minificationSource";
            final Value bindings = engine.getBindings("js");
            bindings.putMember(srcBinding, source);

            Value promise = engine.eval("js", "Terser.minify(" + srcBinding + ", {})");
            String result = (String)waitForResult(promise).get("code");

            stopwatch.stop();
            log.info("Minified source in " + stopwatch);
            log.info("Source:");
            log.info(source);
            log.info("Result:");
            log.info(result);

            assertThat(result).isEqualTo(EXPECTED_RESULT);
        }
    }

    /*
     * This test shows that re-using an engine to minify multiple sources in parallel is
     * not a good idea. When using a multithreaded executor {@link Executors#newFixedThreadPool(int)},
     * errors start to occur while minifying.
     *
     * However, because loading the terser library into the engine takes quite some time, it is a good
     * idea to eval the minified terser once, and then reuse the engine to minify each source file.
     */
    @Test
    public void testReuseEngine() throws Exception {
        log.info("Initialize script engine ...");

        Stopwatch stopwatch = new Stopwatch();
        InputStream inputStream = ReusableEngineTest.class.getResourceAsStream("/" + TERSER_JS);
        InputStreamReader fileReader = new InputStreamReader(inputStream, UTF_8);

        try (final Context engine = TerserMinifier.createEngine()) {
            engine.eval(Source.newBuilder("js", fileReader, TERSER_JS).build());

            log.info("Initialized script engine in " + stopwatch.stop());

            final List<Exception> exc = new ArrayList<>();
            Runnable task = () -> {
                try (InputStream stream = ReusableEngineTest.class.getResourceAsStream("/src/test.js")) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                        String source = reader.lines().collect(joining());
                        String srcBinding = "minificationSrc";
                        final Value bindings = engine.getBindings("js");
                        bindings.putMember(srcBinding, source);

                        Value promise = engine.eval("js", "Terser.minify(" + srcBinding + ", {})");
                        String result = (String)waitForResult(promise).get("code");

                        assertThat(result).isEqualTo(EXPECTED_RESULT);
                    }
                }
                catch (Exception e) {
                    exc.add(e);
                }
            };

            stopwatch.start();
            int n = 100;
            log.info(format("Minifying %d sources ...", n));
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            for (int i = 0; i < n; i++) {
                executorService.submit(task);
            }
            executorService.shutdown();
            executorService.awaitTermination(1, MINUTES);
            log.info(format("Minified %d sources in %s", n, stopwatch.stop()));

            assertThat(executorService.isTerminated()).isTrue();

            if (!exc.isEmpty()) {
                log.error(format("%d exceptions occurred during multithreaded minification.", exc.size()));
                throw exc.get(0);
            }
        }
    }

    static class Stopwatch {

        private final DecimalFormat decimalFormat = (DecimalFormat) NumberFormat.getInstance(US);

        private long start;
        private long end;

        {
            DecimalFormatSymbols decimalFormatSymbols = decimalFormat.getDecimalFormatSymbols();
            decimalFormatSymbols.setGroupingSeparator(' ');
            decimalFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        }

        public Stopwatch() {
            start();
        }

        public void start() {
            start = System.nanoTime();
        }

        Stopwatch stop() {
            end = System.nanoTime();
            return this;
        }

        public String toString() {
            return format("%sms", decimalFormat.format((end - start) / 1000000));
        }

    }

}
