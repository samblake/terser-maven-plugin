package com.github.samblake.terser.plugin;

import com.github.samblake.terser.plugin.minifier.Minification;
import com.github.samblake.terser.plugin.minifier.ParallelTerserMinificationStrategy;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;

@Mojo(name = "terser", defaultPhase = LifecyclePhase.PROCESS_RESOURCES, threadSafe = true)
public class TerserMojo extends AbstractMojo {
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose = false;

    @Parameter(property = "threads", defaultValue = "1")
    private int threads = 1;

    @Parameter(property = "terserSrc", required = true)
    private File terserSrc;

    @Parameter(property = "sourceDir", required = true)
    private File sourceDir;

    @Parameter(property = "targetDir", required = true)
    private File targetDir;

    @Parameter(property = "jsSourceFiles", alias = "jsFiles")
    private List<String> jsSourceFiles = new ArrayList<>();

    @Parameter(property = "jsSourceIncludes", alias = "jsIncludes")
    private List<String> jsSourceIncludes = new ArrayList<>();

    @Parameter(property = "jsSourceExcludes", alias = "jsExcludes")
    private List<String> jsSourceExcludes = new ArrayList<>();

    @Parameter(property = "suffix", defaultValue = "min")
    private String suffix;

    @Parameter(property = "options", defaultValue = "{}")
    private String options;

    @Parameter(property = "encoding")
    private String encoding = defaultCharset().name();

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Charset charset = Charset.forName(encoding);
        if (verbose) {
            getLog().info("Run in the verbose mode.");
            getLog().info(format("Charset: %s.", charset));
            getLog().debug(toString());
        }

        if (!terserSrc.exists() || !terserSrc.canRead()) {

            getLog().error(terserSrc.getAbsolutePath());

            getLog().error("Given Terser file is not reachable.");
            throw new MojoFailureException("Given Terser file is not reachable.");
        }

        if (options.isEmpty()) {
            throw new MojoFailureException("No Terser options defined.");
        }

        if (jsSourceFiles.isEmpty() && jsSourceIncludes.isEmpty()) {
            getLog().warn("No source files provided, nothing to do.");
            return;
        }

        final Set<Minification> minifications = new MinificationInitializer(this).getMinification();
        if (minifications.isEmpty()) {
            getLog().info("No files found to minify.");
            return;
        }

        if (verbose) {
            getLog().info(format("Found %s files to minify.", minifications.size()));
        }

        try {
            new ParallelTerserMinificationStrategy(getLog(), threads)
                    .execute(minifications)
                    .parallel()
                    .forEach(this::writeFiles);
        }
        catch (Exception e) {
            throw new MojoExecutionException("Failed on Terser minification execution.", e);
        }

        getLog().info("Terser minification execution successful.");
    }

    private void writeFiles(Minification minification) {
        TargetFileWriter.writeTargetFile(minification);
        if (minification.getSourceMap().isPresent()) {
            TargetFileWriter.writeMapFile(minification);
        }
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public int getThreads() {
        return this.threads;
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    public File getTerserSrc() {
        return this.terserSrc;
    }

    public void setTerserSrc(File terserSrc) {
        this.terserSrc = terserSrc;
    }

    public File getSourceDir() {
        return this.sourceDir;
    }

    public void setSourceDir(File sourceDir) {
        this.sourceDir = sourceDir;
    }

    public File getTargetDir() {
        return this.targetDir;
    }

    public void setTargetDir(File targetDir) {
        this.targetDir = targetDir;
    }

    public List<String> getJsSourceFiles() {
        return this.jsSourceFiles;
    }

    public void setJsSourceFiles(List<String> jsSourceFiles) {
        this.jsSourceFiles = jsSourceFiles;
    }

    public List<String> getJsSourceIncludes() {
        return this.jsSourceIncludes;
    }

    public void setJsSourceIncludes(List<String> jsSourceIncludes) {
        this.jsSourceIncludes = jsSourceIncludes;
    }

    public List<String> getJsSourceExcludes() {
        return this.jsSourceExcludes;
    }

    public void setJsSourceExcludes(List<String> jsSourceExcludes) {
        this.jsSourceExcludes = jsSourceExcludes;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public String getOptions() {
        return this.options;
    }

    public void setOptions(String options) {
        this.options = options;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public void setJsSourceFile(String jsSourceFile) {
        jsSourceFiles.add(jsSourceFile);
    }

    public void setJsSourceInclude(String jsSourceInclude) {
        jsSourceIncludes.add(jsSourceInclude);
    }

    public void setJsSourceExclude(String jsSourceExclude) {
        this.jsSourceExcludes.add(jsSourceExclude);
    }

    @Override
    public String toString() {
        return "TerserMojo{" +
                "verbose=" + verbose +
                ", threads=" + threads +
                ", terserSrc=" + terserSrc +
                ", sourceDir=" + sourceDir +
                ", targetDir=" + targetDir +
                ", jsSourceFiles=" + jsSourceFiles +
                ", jsSourceIncludes=" + jsSourceIncludes +
                ", jsSourceExcludes=" + jsSourceExcludes +
                ", suffix='" + suffix + '\'' +
                ", options='" + options + '\'' +
                ", encoding='" + encoding + '\'' +
                '}';
    }
}