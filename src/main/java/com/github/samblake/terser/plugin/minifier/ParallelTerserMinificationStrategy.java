package com.github.samblake.terser.plugin.minifier;

import org.apache.maven.plugin.logging.Log;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class ParallelTerserMinificationStrategy implements TerserMinificationStrategy {
    private final Log log;
    private final int threads;

    public ParallelTerserMinificationStrategy(final Log log, final int threads) {
        this.log = requireNonNull(log);
        this.threads = getAvailableThreads(threads);
    }

    @Override
    public Stream<Minification> execute(final Set<Minification> minifications) {
        final ConcurrentLinkedQueue<Minification> queue = new ConcurrentLinkedQueue<>(minifications);

        // Each thread's task is to create a terser minifier and perform as much minification as possible
        final Supplier<Collection<Minification>> task = () -> {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            try (TerserMinifier minifier = new TerserMinifier()) {
                final Set<Minification> minificationResults = new HashSet<>();
                Minification currentMinification;
                while ((currentMinification = queue.poll()) != null) {
                    if (log.isDebugEnabled()) {
                        String name = Thread.currentThread().getName();
                        log.debug(format("[%s] minifying %s", name, currentMinification.getSource()));
                    }
                    minificationResults.add(minifier.execute(currentMinification));
                }
                return minificationResults;
            }
        };

        final Collection<CompletableFuture<Collection<Minification>>> futures = new HashSet<>();
        for (int i = 0; i < threads; i++) {
            futures.add(CompletableFuture.supplyAsync(task));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .flatMap(Collection::stream);
    }

    private int getAvailableThreads(final int threads) {
        final int availableThreads = getRuntime().availableProcessors();
        if (threads < 1) {
            log.warn(format("Invalid number of threads (%d). Setting number of threads to 1", threads));
            return 1;
        }

        if (threads > availableThreads) {
            log.warn(format("Configured number of threads (%d) exceeds the number of available processors (%d), " +
                    "setting number of threads to %2$d", threads, availableThreads));
            return availableThreads;
        }

        return threads;
    }
}