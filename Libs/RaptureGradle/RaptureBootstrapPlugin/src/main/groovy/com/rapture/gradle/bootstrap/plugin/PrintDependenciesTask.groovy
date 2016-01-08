package com.rapture.gradle.bootstrap.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class PrintDependenciesTask extends DefaultTask {

    final static TreeSet dependencies = new TreeSet()

    PrintDependenciesTask() {
        project.plugins.apply("project-report")
        dependsOn project.tasks.dependencyReport
    }

    /**
     * Aggregate the unique dependencies of this project, sorted, with version numbers.
     */
    @TaskAction
    void collectDependencies() {
        def f =new File("${project.projectReportDir}/dependencies.txt") 
        if (f.exists()) {
        f.eachLine { line ->
            println("Got a line " + line)
            if ( (line.indexOf("+---") != -1 || line.indexOf("\\---") != -1) && (line.indexOf("+--- project") == -1) && (line.indexOf("\\--- project") == -1))  {
                def match = (line =~ /---\s(.*):(.*):(.*)/)
                println("Working with match " + match)
                def toPrint = "${match[0][1]}:${match[0][2]}:${match[0][3]}"
                println("Got a print " + toPrint)
                if (!toPrint.endsWith("(*)")) { 
                    dependencies.add "$toPrint" 
                }
            }
          }
       }
    }
}
