package com.rapture.gradle.bootstrap.plugin;

import static org.junit.Assert.*

import groovy.json.JsonSlurper;

import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Test

class RaptureBootstrapPluginTest {

    Project project;
    String testUrl = "file:/test";

    @Before
    public void setup() {
        project = ProjectBuilder.builder().build();
    }

    @Test
    public void testRepo() {
        project.apply plugin: RaptureBootstrapPlugin
        assertEquals(4, project.repositories.size());
    }

    @Test
    public void testRepo2() {
        project.apply plugin: RaptureBootstrapPlugin
        project.repositories { mavenCentral() };
        project.repositories { mavenLocal() };
        ExtraPropertiesExtension props = project.extensions.extraProperties;
        Map map = props.getProperties();
        assertEquals(6, project.repositories.size());
        assertFalse(project.repositories.any {
            return it.url.toString() == testUrl
        });
    }

    @Test
    public void testRepo3() {
        project.apply plugin: RaptureBootstrapPlugin;
        project.ext.incaptureRepoUrl = testUrl;
        assertFalse(project.repositories.any {
            return it.url.toString() == testUrl
        });
    }

    @Test
    public void testRepo4() {
        project.ext.downloadRepoUrl = testUrl;
        project.apply plugin: RaptureBootstrapPlugin;
        assertTrue(project.repositories.any {
            return it.url.toString() == testUrl
        });
    }

    @Test
    public void testUpload() {
        project.ext.uploadRepoUrl = testUrl;
        project.apply plugin: RaptureBootstrapPlugin;
        assertTrue(project.uploadArchives.repositories.any {
            return it.repository.url.toString() == testUrl
        });
    }

    @Test
    public void testUpload2() {
        project.apply plugin: RaptureBootstrapPlugin;
        assertFalse(project.uploadArchives.repositories.any {
            return it.repository.url.toString() == testUrl
        });
        assertTrue(project.uploadArchives.repositories.size() > 0);
    }

    @Test
    public void testReadFile() {
        assertTrue(RaptureBootstrapPlugin.getOfficialDeployUrl().size() > 0);
        assertTrue(RaptureBootstrapPlugin.getOfficialRepoUrl().size() > 0);
    }

    @Test
    public void testEnablePlugins() {
        project.ext.raptureBootstrapDisablePlugins = 'false'
        project.apply plugin: RaptureBootstrapPlugin
        assertTrue('java plugin should be applied', project.plugins.hasPlugin('java'))
//        assertTrue('application plugin should be applied', project.plugins.hasPlugin('application'))
        assertTrue('rapture bootstrap plugin should be applied', project.plugins.hasPlugin('rapture-bootstrap'))
    }

    @Test
    public void testDisablePlugins() {
        project.ext.raptureBootstrapDisablePlugins = 'true'
        project.apply plugin: RaptureBootstrapPlugin
        assertFalse('java plugin should not be applied', project.plugins.hasPlugin('java'))
        assertTrue('rapture bootstrap plugin should be applied', project.plugins.hasPlugin('rapture-bootstrap'))
    }

    @Test
    public void testVersioning() {
        project.version = '1.1.9'
        project.apply plugin: RaptureBootstrapPlugin
        assertEquals('Version is wrong', '1.1.9.99999999999999', project.version)
    }

    @Test
    public void testVersioningWithNoVersion() {
        project.apply plugin: RaptureBootstrapPlugin
        assertEquals('Version is wrong when unspecified', Project.DEFAULT_VERSION, project.version)
    }
         
    @Test
    public void testDependencyVersioning() {
        project.version = '1.1.1'
        project.ext.RaptureNew = '1.1.2.+'
        project.apply plugin: RaptureBootstrapPlugin
        assertEquals('RaptureNew version is wrong', '1.1.2.+', project.ext.RaptureNew)
        assertEquals('Version is wrong', '1.1.1.99999999999999', project.version)

        project = ProjectBuilder.builder().build()
        project.ext.RaptureNew = '1.7.2.+'
        project.ext.Vienna = '1.8.2.+'
        String versionToTest1 = 'RaptureNew=1.5.4.20130822104523'
        String versionToTest2 = 'Vienna=1.1.0.20130822104523'
        def tempFile = new File(RaptureVersioning.DEPENDENCIES_FILE)
        tempFile.deleteOnExit()
        tempFile.withWriter { w ->
            w.writeLine(versionToTest1)
            w.writeLine(versionToTest2)
        }
        project.apply plugin: RaptureBootstrapPlugin
        assertEquals('Platform version is wrong', '1.5.4.20130822104523', project.ext.RaptureNew)
        assertEquals('Vienna version is wrong', '1.1.0.20130822104523', project.ext.Vienna)
    } 

    @Test
    public void testDestinationDirOverride() {
        project.ext.distZipDest = '/tmp'
        project.ext.installAppDest = '/tmp'
        project.apply plugin: RaptureBootstrapPlugin
//        assertEquals('/tmp', project.tasks.distZip.destinationDir.path);
//        assertEquals('/tmp' + File.separator + project.name, project.tasks.installApp.destinationDir.path);
    }

    @Test
    public void testDestinationDirOverrideWithoutPlugin() {
        project.ext.raptureBootstrapDisablePlugins = 'true'
        project.ext.distZipDest = '/tmp'
        project.apply plugin: RaptureBootstrapPlugin
        // nothing to assert here.  if it fails, it will fail to load plugin
    }

    @Test
    public void testTimestamp() {
        String ts = '20140131112233'
        new File(RaptureVersioning.TIMESTAMP_FILE).with {
            deleteOnExit()
            write ts
        } 
        project.apply plugin: RaptureBootstrapPlugin
        assertEquals('Timestamp is wrong', ts, project.ext.timestamp)
    }

    @Test
    public void testPluginVersioning() {
        String content = '{"depends":{},"plugin":"Research","description":"Some Research repos"}';
        File testDir = new File('testplugin')
        testDir.mkdirs()
        testDir.deleteOnExit()
        new File('testplugin/plugin.txt').with {
            deleteOnExit()
            write content
        }
        project.version = '1.1.10';
        project.apply plugin: RaptureBootstrapPlugin
        project.tasks.applyPluginManifestVersion.execute()
        String output = "${project.buildDir}/testplugin/plugin.txt"
        String ret = new File(output).text
        def slurper = new JsonSlurper()
        def result = slurper.parseText(ret);
        assertEquals("Research", result.plugin)
        assertEquals("Some Research repos", result.description)
        assertEquals([:], result.depends)
        assertEquals(['major':1, 'minor':1, 'release':10, 'timestamp':99999999999999], result.version)
    }

    @Test
    public void testPluginVersioningExistingVersion() {
        String content = '{"depends":{},"plugin":"MyTestResearch","description":"This plugin installs heisenberg","version":{"major":0,"minor":0,"release":0,"timestamp":0}}';
        File testDir = new File('testplugin')
        testDir.mkdirs()
        testDir.deleteOnExit()
        new File('testplugin/plugin.txt').with {
            deleteOnExit()
            write content
        }
        project.version = '1.1.11';
        project.apply plugin: RaptureBootstrapPlugin
        project.tasks.applyPluginManifestVersion.execute()
        String output = "${project.buildDir}/testplugin/plugin.txt"
        String ret = new File(output).text
        def slurper = new JsonSlurper()
        def result = slurper.parseText(ret);
        assertEquals("MyTestResearch", result.plugin)
        assertEquals("This plugin installs heisenberg", result.description)
        assertEquals([:], result.depends)
        assertEquals(['major':1, 'minor':1, 'release':11, 'timestamp':99999999999999], result.version)
    }
  
    @Test
    public void testDebianPackageTask() {
        project.version = '1.1.1';
        project.apply plugin: RaptureBootstrapPlugin
        def task = project.task('debpkg', type: com.rapture.gradle.bootstrap.plugin.DebianPackageTask)
        assertTrue(task instanceof com.rapture.gradle.bootstrap.plugin.DebianPackageTask)
    }
}
