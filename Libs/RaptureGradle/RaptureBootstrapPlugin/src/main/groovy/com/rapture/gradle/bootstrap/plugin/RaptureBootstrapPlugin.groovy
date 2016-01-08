package com.rapture.gradle.bootstrap.plugin

import org.apache.commons.io.FileUtils;
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.repositories.*
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.api.tasks.Upload

import groovy.lang.MissingPropertyException

class RaptureBootstrapPlugin implements Plugin<Project>, ProjectEvaluationListener {

    /*
     * disable default plugins for projects that do not need them.  set to true by defining this property in build.gradle
     */
    boolean raptureBootstrapDisablePlugins = false
    boolean enableExplodedTasks = false

    void apply(Project project) {
        if (project.parent == null) {
            println "RaptureBootstrapPlugin version " + getClass().getPackage().getImplementationVersion()
            project.gradle.addProjectEvaluationListener(this)
        }
        raptureBootstrapDisablePlugins = project.hasProperty('raptureBootstrapDisablePlugins') && project.raptureBootstrapDisablePlugins.equals('true')
        if (!raptureBootstrapDisablePlugins) {
            applyDefaultPlugins(project);
        }
        setupRepos(project);
        setupConfigurations(project);
        new RaptureVersioning().apply(project);
        new ArchivePublisher().apply(project);
        new DependencyPrinter().apply(project);

        addExplodedTasks(project);

            addWrapperTask(project)
            if (project.getParent() == null) {
        }
    }

    void afterEvaluate(Project project, ProjectState state) {
        // Only if the execution was successful
        if (state.executed) {
            project.plugins.withType(org.gradle.api.plugins.ApplicationPlugin) { plugin ->
                // If we are using the application plugin and either distZipDest or installAppDest are
                // set as project properties then we need to update the destinationDir
                if (project.hasProperty('distZipDest')) {
                    project.tasks.distZip { destinationDir = new File(project.getProperty('distZipDest')) }
                }
                if (project.hasProperty('installAppDest')) {
                    project.tasks.installApp { destinationDir = new File(project.getProperty('installAppDest') + File.separator + project.name) }
                }
                // We also need to fix the startup scripts
                setupStartScripts(project)
            }
        }
    }
    
    void beforeEvaluate(Project project) {
        //println "beforeEvaluate: ${project.name}"
    }
    
    void addWrapperTask(Project project) {
        project.tasks.create(name: 'wrapper', type: Wrapper) {
            distributionUrl = 'https://services.gradle.org/distributions/gradle-2.0-all.zip'
        }
    }

    void setupConfigurations(Project project) {
        /**
         * RaptureAppConfig is a runtime-only configuration, should not be used for testing
         */
        project.configurations {
            testCompile.exclude module: 'RaptureAppConfig'
            testRuntime.exclude module: 'RaptureAppConfig'
        }
    }

    void addExplodedTasks(Project project) {
        project.configurations {
            exploded {
                description = 'Dependencies in jars or zips that should be exploded locally'
                transitive = false
            }
        }

        project.tasks.create(name: 'printExplodedDeps') << {
            println "Exploding dependencies within $project.name: " + project.configurations.exploded.collect { File file -> file }
        }

        project.tasks.create(name: 'copyExplodedDeps', type: Copy) {
            into('build')
            project.configurations.exploded.incoming.afterResolve { ResolvableDependencies incoming ->
                incoming.files.each { File file ->
                    if (file.name.endsWith(".jar")) {
                        println("Exploding jar $file")
                        from(project.zipTree(file)) { exclude("META-INF/**") }
                    }
                }
            }
        }

        project.tasks.copyExplodedDeps.dependsOn(project.tasks.printExplodedDeps);
        if (project.tasks.findByPath('compileJava') != null) {
            project.tasks.compileJava.dependsOn(project.tasks.copyExplodedDeps);
        }
    }

    static String getOfficialRepoUrl() {
        Properties props = getRepoProperties();
        return props.getProperty("officialRepoUrl");
    }

    static String getOfficialDeployUrl() {
        Properties props = getRepoProperties();
        return props.getProperty("officialDeployUrl");
    }

    static Properties getRepoProperties() {
        InputStream inStream = RaptureBootstrapPlugin.getClassLoader().getResourceAsStream("repo.properties");

        Properties props = new Properties();
        props.load(inStream);
        try {
            inStream.close();
        }
        catch (Exception ignore) {
        }
        return props;
    }

    void applyDefaultPlugins(Project project) {
        project.getPlugins().apply("java");
        project.getPlugins().apply("maven");
        if (project.gradle.gradleVersion < '2.3') {
            // Apply the application plugin for backwards compatibility with branch builds
            project.getPlugins().apply("application");
        }

        setupJavaProperties(project);

        setupLicense(project)
        setupIdea(project);

        String buildDir = project.buildDir
        project.ext {
            generatedMainJava = "$buildDir/generated-sources/main/java"
            generatedMainRes = "$buildDir/generated-sources/main/resources"
            generatedTestJava = "$buildDir/generated-sources/test/java"
            generatedTestRes = "$buildDir/generated-sources/test/resources"
        }

        addPathFromProperty(project, "main", "java", "generatedMainJava")
        addPathFromProperty(project, "test", "java", "generatedTestJava")
        addPathFromProperty(project, "main", "resources", "generatedMainRes")
        addPathFromProperty(project, "test", "resources", "generatedTestRes")
    }

    void setupStartScripts(Project project) {
        project.startScripts {
            // add etc folder to the classpath to pick up cfg files
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                classpath = project.files(['%APP_HOME%\\etc'])
            } else {
                classpath = project.files([
                        '$APP_HOME/lib/*',
                        '$APP_HOME/etc'
                ])
            }
            doLast {
                // gradle always puts extra classpaths relative to the lib folder, so we have to replace
                windowsScript.text = windowsScript.text.replace('%APP_HOME%\\lib\\etc', '%APP_HOME%\\lib\\*;%APP_HOME%\\etc')
                unixScript.text = unixScript.text.replace('$APP_HOME/lib/etc', '$APP_HOME/etc')
            }
        }

    }

    void setupJavaProperties(Project project) {
        project.targetCompatibility = 1.7
        project.sourceCompatibility = 1.7


        project.test {
            beforeTest { descriptor ->
                project.logger.lifecycle("Running test: " + descriptor)
            }
        }

    }

    void setupLicense(Project project) {
        project.getPlugins().apply("license");

        URL url = getClass().getClassLoader().getResource("license/rapture.license");
        File file = File.createTempFile("rapture", ".license");

        FileUtils.copyURLToFile(url, file);
        file.deleteOnExit();

        project.license {
            ignoreFailures = true
            ext.year = Calendar.getInstance().get(Calendar.YEAR)
            header file
            mapping('rfx', 'JAVADOC_STYLE')
            excludes(["**/*.json", "**/*.txt"])
        }
    }

    public static void addPathFromProperty(Project project, String first, String second, String property) {
        if (property != null && project.hasProperty(property)) {
            String path = project.ext."$property"
            addPath(project, first, second, path)
        }
    }

    public static void addPath(Project project, String first, String second, String path) {
        File file = new File(path)
        if (project.hasProperty('sourceSets') && project.sourceSets.hasProperty(first) && project.sourceSets."$first".hasProperty(second)) {
            project.sourceSets."$first"."$second".srcDir path
        }
    }

    void setupIdea(Project project) {
        project.getPlugins().apply("idea");
        project.idea.module {
            excludeDirs -= project.file(project.buildDir) //1
            project.buildDir.listFiles({ d, f -> f != 'generated-sources' } as FilenameFilter).each { excludeDirs += it }
            sourceDirs += project.file('src/main/java')
            sourceDirs += project.file('src/main/resources')
            sourceDirs += project.file('build/generated-sources/antlr')
            sourceDirs += project.file('build/generated-sources/antlr3')
            sourceDirs += project.file('build/generated-sources/main/java')
            sourceDirs += project.file('build/generated-sources/main/resources')
        }

        project.idea.module.iml {
            whenMerged { module ->
                def gradleDeps = module.dependencies
                def addDeps = { mod, list ->
                    list.each {
                        def id = it.moduleVersion.id;
                        if (id.group.contains("rapture") || id.group.contains("incapture")) {
                            println("Adding module for $id.group $id.module $id.name $id.version")
                            mod.dependencies.add(new org.gradle.plugins.ide.idea.model.ModuleDependency(it.name, 'COMPILE'))
                        }
                    }
                }
                module.dependencies = new LinkedHashSet()
                project.configurations.each {
                    def artifactsList = it.resolvedConfiguration.resolvedArtifacts
                    addDeps(module, artifactsList)
                }
                module.dependencies.addAll(gradleDeps)
            }
        }

    }

    File setupKeyFile() {
        URL url = getClass().getClassLoader().getResource("keys/id_rsa");
        File file = File.createTempFile("rsa", ".private");
        FileUtils.copyURLToFile(url, file);
        file.deleteOnExit();
        return file;
    }

    void setupRepos(Project project) {
        //First, add mavenLocal. We always search that first
        project.repositories { mavenLocal() }

        if (project.hasProperty("downloadRepoUrl")) {
            addCustomRepo(project, project.downloadRepoUrl);
            // allow addition of official repo in addition to custom repo
            if (project.hasProperty("addOfficialRepo")) {
                addOfficialRepo(project);
            }
        } else {
            addOfficialRepo(project);
        }

        if (!raptureBootstrapDisablePlugins) {
            if (project.hasProperty("uploadRepoUrl")) {
                addCustomUpload(project, project.uploadRepoUrl);
            } else {
                addOfficialUpload(project);
            }
        }

        //Add third party repos next
        project.repositories {
            mavenCentral()
            maven {
                url "https://plugins.gradle.org/m2/"
            }
        }
    }

    /**
     * Add a maven repo for retrieval and publishing that's located in a custom path/url. It assumes auth is not required
     * @param project
     * @param customRepoUrl
     */
    void addCustomRepo(Project project, String customRepoUrl) {
        project.repositories {
            maven {
                if (project.hasProperty('downloadRepoUsername') && project.hasProperty('downloadRepoPassword')) {
                    credentials {
                        username = project.downloadRepoUsername
                        password = project.downloadRepoPassword
                    }
                }
                name = "incaptureRepo"
                url = customRepoUrl
            }
        }
    }

    void addCustomUpload(Project project, String customUploadUrl) {
        project.configurations { deployerJars }
        project.dependencies { deployerJars 'org.apache.maven.wagon:wagon:2.8' }
        project.dependencies { deployerJars 'org.apache.maven.wagon:wagon-ssh:2.8' }

        project.uploadArchives {
            repositories {
                mavenDeployer {
                    configuration = project.configurations.deployerJars
                    name = 'sshDeployer'
                    repository(url: customUploadUrl)
                }
            }
        }
    }

    /**
     * Add the official maven repo for publishing. Auth is required 
     * @param project
     */
    void addOfficialRepo(Project project) {
        project.repositories {
            maven {
                credentials {
                    username = "rapture"
                    password = "raptureRelease"
                }
                name = "incaptureRepo"
                url = getOfficialRepoUrl()
            }
        }
    }

    /**
     * Add the official maven repo for publishing, auth is required
     * @param project
     */
    void addOfficialUpload(Project project) {
        File privateKey = setupKeyFile();

        project.configurations { deployerJars }
        project.dependencies { deployerJars 'org.apache.maven.wagon:wagon:2.8' }
        project.dependencies { deployerJars 'org.apache.maven.wagon:wagon-ssh:2.8' }

        project.uploadArchives {
            repositories {
                mavenDeployer {
                    configuration = project.configurations.deployerJars
                    name = 'sshDeployer'
                    repository(url: getOfficialDeployUrl()) {
                        authentication(userName: 'ubuntu', privateKey: privateKey.getAbsolutePath(), passphrase: "raptur3")
                    }
                }
            }
        }
    }
}
