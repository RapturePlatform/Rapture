package com.rapture.gradle.bootstrap.plugin

import org.gradle.api.Project
import org.gradle.BuildAdapter
import org.gradle.BuildResult

/**
 * This class is responsible for setting up a task to print out a sorted list of unique dependencies and their versions
 * for the current project.
 *
 * @author dukenguyen 
 */
class DependencyPrinter {

    void apply(Project project) {
        project.task('printDependencies', type: PrintDependenciesTask)        
        // only print out once for the top-level project
        if (project.parent == null) {
            project.gradle.addBuildListener(new BuildAdapter(){
                void buildFinished(BuildResult result) {
                    if (!PrintDependenciesTask.dependencies.isEmpty()) {
                        new File("${project.projectReportDir}/uniqueAndSortedDependencies.txt").withWriter { out ->
                            PrintDependenciesTask.dependencies.each {
                                out.println it
                                println it
                            }
                        }
                    }
                }
            });
        }
    }


}
