package com.rapture.gradle.bootstrap.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Given a list of artifacts or zip files, create debian packages
 * from them
 *
 *  This task can only be run on environments that have the following:
 *    - dpkg-deb
 *    - fakeroot
 *    - alien
 *    - rapture user
 */
class DebianPackageTask extends DefaultTask {

    String versionOutputFilename
    String installDir
    String workDir
    String artifactVersion
    String packagePrefix
    def artifacts = []

    @TaskAction
    def debpkg() {
        new File("$workDir/$versionOutputFilename").delete()
        artifacts.sort()
        artifacts.each {
            def artifactLower = it.toLowerCase()
            if (packagePrefix != null) {
                artifactLower = packagePrefix + artifactLower
            }
            println "Creating debian package: $artifactLower"
            new File("$workDir/$versionOutputFilename").append("$artifactLower : $artifactVersion\n")
            exec("sudo rm -rf $workDir/$it/packaging")
            new File("$workDir/$it/packaging/$installDir").mkdirs()
            new File("$workDir/$it/packaging/DEBIAN").mkdirs()
            new File("$workDir/$it/packaging/DEBIAN/control").withWriter { out ->
                out.writeLine("Package: $artifactLower")
                out.writeLine("Priority: optional")
                out.writeLine("Maintainer: Incapture Technologies")
                out.writeLine("Architecture: all")
                out.writeLine("Version: $artifactVersion")
                out.writeLine("Description: $it")
            }
            new File("$workDir/$it/packaging/$installDir/${it}-${artifactVersion}.zip").bytes = new File("$workDir/${it}-${artifactVersion}.zip").bytes
            exec("sudo chown -R rapture:rapture $workDir/$it/packaging")
            exec("dpkg-deb -z9 -Zgzip --build $workDir/$it/packaging $workDir")
            def user = System.getenv('USER')
            exec("sudo chown -R ${user}:${user} $workDir/$it/packaging")
            println "Creating RPM package: $artifactLower"
            exec("fakeroot alien --scripts -r ${workDir}/${artifactLower}_${artifactVersion}_all.deb")
            exec("mv ./${artifactLower}-${artifactVersion}-2.noarch.rpm ${workDir}")
        }
    }

    def exec(command) {
        def proc = command.execute()
        proc.waitFor()
        printResult(proc)
    }

    def printResult(proc) {
        println "return code: ${proc.exitValue()}"
        println "stderr: ${proc.err.text}"
        println "stdout: ${proc.in.text}"
    }
}
