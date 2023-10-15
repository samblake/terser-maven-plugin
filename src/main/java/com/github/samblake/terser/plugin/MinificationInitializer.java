package com.github.samblake.terser.plugin;

import com.github.samblake.terser.plugin.minifier.ImmutableMinification;
import com.github.samblake.terser.plugin.minifier.ImmutableMinificationContext;
import com.github.samblake.terser.plugin.minifier.Minification;
import com.github.samblake.terser.plugin.minifier.MinificationContext;
import org.codehaus.plexus.util.DirectoryScanner;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.io.FilenameUtils.indexOfExtension;

class MinificationInitializer {
    private final TerserMojo terserMojo;

    MinificationInitializer(final TerserMojo terserMojo) {
        this.terserMojo = requireNonNull(terserMojo);
    }

    Set<Minification> getMinification() {
        final Set<ImmutableMinification.Builder> minifications = new HashSet<>();

        final MinificationContext context = ImmutableMinificationContext.builder()
                .terserSource(terserMojo.getTerserSrc())
                .sourceMapSource(Optional.ofNullable(terserMojo.getSourceMapSrc()))
                .charset(Charset.forName(terserMojo.getEncoding()))
                .log(terserMojo.getLog())
                .isVerbose(terserMojo.isVerbose())
                .options(terserMojo.getOptions())
                .build();
        
        addStaticFiles(minifications);
        addPatternMatchedFiles(minifications);
        
        return minifications.stream()
                .map(minification -> minification.context(context).build())
                .collect(toSet());
    }

    private void addStaticFiles(final Set<ImmutableMinification.Builder> sourceFiles) {
        // Add statically added files
        terserMojo.getJsSourceFiles().stream()
                .map(this::removeLeadingSlash)
                .map(this::resolveAgainstSourceDirectory)
                .filter(Files::exists)
                .map(this::toMinificationBuilder)
                .forEach(sourceFiles::add);
    }

    private Path resolveAgainstSourceDirectory(final String sourcePath) {
        return terserMojo.getSourceDir().toPath().resolve(sourcePath);
    }

    private void addPatternMatchedFiles(final Set<ImmutableMinification.Builder> sourceFiles) {
        // Add pattern matched files
        if (!terserMojo.getJsSourceIncludes().isEmpty()) {
            Stream.of(getIncludesDirectoryScanner().getIncludedFiles())
                    .map(this::resolveAgainstSourceDirectory)
                    .map(this::toMinificationBuilder)
                    .forEach(sourceFiles::add);
        }
    }

    private ImmutableMinification.Builder toMinificationBuilder(final Path sourceFile) {
        return ImmutableMinification.builder()
                .source(sourceFile)
                .target(determineTargetPath(sourceFile));
    }

    private Path determineTargetPath(final Path sourceFile) {
        final Path relativePath = getRelativePath(sourceFile);
        final String suffix = terserMojo.getSuffix() == null ? "" : terserMojo.getSuffix();

        String inName = sourceFile.getFileName().toString();
        String outName = suffix.isEmpty() ? inName : insertSuffix(inName, suffix);

        return terserMojo.getTargetDir().toPath().resolve(relativePath).resolve(outName);
    }

    private String insertSuffix(String fileName, String suffix) {
        int extensionIndex = indexOfExtension(fileName);
        int insertIndex = extensionIndex == -1 ? fileName.length() : extensionIndex;
        String before = fileName.substring(0, insertIndex);
        String after = fileName.substring(insertIndex);
        return before + "." + suffix + after;
    }

    private String removeLeadingSlash(final String subject) {
        if (subject.startsWith(File.separator) || subject.startsWith("/")) {
            return subject.substring(1);
        }
        return subject;
    }

    private DirectoryScanner getIncludesDirectoryScanner() {
        final DirectoryScanner scanner = new DirectoryScanner();
        scanner.setIncludes(checkFileSeparator(terserMojo.getJsSourceIncludes()));
        scanner.setExcludes(checkFileSeparator(terserMojo.getJsSourceExcludes()));
        scanner.addDefaultExcludes();
        scanner.setBasedir(terserMojo.getSourceDir());
        scanner.scan();
        return scanner;
    }

    /**
     * If '/' is used in the given paths, and the system file separator is not
     * '/', replace '/' with the {@link File#separator}. This is required for
     * using {@link DirectoryScanner}
     */
    private String[] checkFileSeparator(final List<String> paths) {
        return paths.stream()
                .map(this::replaceFileSeparator)
                .map(this::removeLeadingSlash)
                .toArray(String[]::new);
    }

    private String replaceFileSeparator(String path) {
        return File.separatorChar != '/' && path.contains("/")
                ? path.replace("/", File.separator)
                : path;
    }

    private Path getRelativePath(final Path sourceFile) {
        String path = terserMojo.getSourceDir()
                .toURI()
                .relativize(sourceFile.getParent().toFile().toURI())
                .getPath();

        return Paths.get(path);
    }
}