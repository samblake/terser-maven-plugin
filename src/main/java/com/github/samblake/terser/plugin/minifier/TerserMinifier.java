package com.github.samblake.terser.plugin.minifier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.maven.plugin.logging.Log;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.HostAccess;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES;
import static com.oracle.truffle.js.runtime.JSContextOptions.UNHANDLED_REJECTIONS_NAME;
import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;

public class TerserMinifier implements AutoCloseable {
    private static final String CODE_VARIABLE = "code";
    private static final String OPTIONS_VARIABLE = "options";
    private static final String TERSER_EXECUTE = "Terser.minify(%s, %s)";

    private MinificationContext minificationContext;
    private Context executionContext;

    private void initialize(final MinificationContext context) {
        requireNonNull(context);
        if (this.minificationContext == null || !this.minificationContext.equals(context)) {
            this.minificationContext = context;
            initEngine();
        }
    }

    private void initEngine() {
        minificationContext.getLog().debug("Initializing script engine");

        try {
            executionContext = createEngine();

            executionContext.eval(Source.newBuilder("js", minificationContext.getTerserSource()).build());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected static Context createEngine() {
        Engine engine = Engine.newBuilder()
                .option("engine.WarnInterpreterOnly", "false")
                .build();

        return Context.newBuilder()
                .engine(engine)
                .allowExperimentalOptions(true)
                .allowPolyglotAccess(PolyglotAccess.ALL)
                .allowHostAccess(HostAccess.ALL)
                .option(UNHANDLED_REJECTIONS_NAME, "throw")
                .build();
    }

    public synchronized Minification execute(final Minification minification) {
        initialize(minification.getContext());

        final Log log = minificationContext.getLog();
        if (minificationContext.isVerbose()) {
            log.info(format("Minifying %s -> %s", minification.getSource(), minification.getTarget()));
        }

        try (Stream<String> lines = Files.lines(minification.getSource(), minificationContext.getCharset())) {
            final String source = lines.collect(joining(lineSeparator()));

            final Value bindings = executionContext.getBindings("js");
            bindings.putMember(CODE_VARIABLE, source);

            Map<String, Object> options = parseOptions(minification);
            bindings.putMember(OPTIONS_VARIABLE, ProxyObject.fromMap(options));

            String command = format(TERSER_EXECUTE, CODE_VARIABLE, OPTIONS_VARIABLE);
            Value promise = executionContext.eval("js", command);

            Map<String, ?> results = waitForResult(promise);
            try {
                String code = (String)results.get("code");

                if (log.isDebugEnabled()) {
                    log.debug(format("%s result:\n%s", minification.getTarget(), code));
                }

                return ImmutableMinification.copyOf(minification).withResult(code);
            }
            catch (Exception e) {
                log.error("Invalid result: " + results.toString());
                throw new RuntimeException("Invalid result", e);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> parseOptions(Minification minification) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(ALLOW_UNQUOTED_FIELD_NAMES, true);
        return mapper.readValue(minification.getContext().getOptions(), HashMap.class);
    }

    protected static Map<String, ?> waitForResult(Value promise) throws InterruptedException, ExecutionException {
        CompletableFuture<Object> cf = new CompletableFuture<>();
        promise.invokeMember("then", (Consumer<Object>) cf::complete)
                .invokeMember("catch", (Consumer<Throwable>) cf::completeExceptionally);
        return (Map<String,?>)cf.get();
    }

    @Override
    public void close() {
        ofNullable(executionContext).ifPresent(Context::close);
    }

}
