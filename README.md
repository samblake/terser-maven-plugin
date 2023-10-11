# Terser Maven Plugin
Plugin lets you to execute Terser minification for given JavaScript files. 
It requires no npm or node.js, it is plain Java project (based on embedded GraalVM from version 1.4) which works perfectly combined with WebJars.

## Settings, ie buttons and knobs
* **`verbose`** - no surprises, the execution becomes a bit more talkative (default: _false_),
* **`threads`** - number of threads to use when transpiling (default: _1_, will be capped on the amount of processors available)
* **`encoding`** - will apply chosen encoding during files operations (read/write) (default: `Charset.defaultCharset()`),
* **`terserSrc`** - readable path to standalone(!) Babel sources. It can be provided from WebJars dependency, minified 
or development version,
* **`sourceDir`** - base path for JavaScript files you are going to minify,
* **`targetDir`** - result path, note that all sub-directories from `sourceDir` will be preserved,
* **`jsFiles`** - list of JavaScript files (static)  from `sourceDir` to translate,
* **`jsIncludes`** - list of JavaScript files (with simple masks `*`/`?`),
* **`jsExcludes`** - list of exceptions for `jsIncludes`,
* **`suffix`** - optional suffix applied for every minified file,
* **`options`** - options for Terser execution

## Example
```xml
<plugin>
    <groupId>com.github.samblake.terser</groupId>
    <artifactId>terser-maven-plugin</artifactId>
    <version>1.0</version>
    <executions>
        <execution>
            <id>js-minify</id>
            <phase>process-resources</phase>
            <goals>
                <goal>terser</goal>
            </goals>
            <configuration>
                <verbose>true</verbose>
                <threads>4</threads>
                <terserSrc>${project.basedir}/target/classes/assets/jslib/terser.min.js</terserSrc>
                <sourceDir>${project.basedir}/target/classes/assets/</sourceDir>
                <targetDir>${project.basedir}/target/classes/assets/</targetDir>
                <jsSourceIncludes>
                    <jsSourceInclude>src/*.js</jsSourceInclude>
                </jsSourceIncludes>
                <suffix>min</suffix>
                <presets>{toplevel:true}</presets>
            </configuration>
        </execution>
    </executions>
</plugin>
```