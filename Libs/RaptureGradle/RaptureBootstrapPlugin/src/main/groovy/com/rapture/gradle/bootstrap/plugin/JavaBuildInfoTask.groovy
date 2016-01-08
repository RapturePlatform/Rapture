package com.rapture.gradle.bootstrap.plugin;

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class JavaBuildInfoTask extends DefaultTask {
    String dir_name = 'build/generated-sources/main/java/rapture/buildinfo'
    String module_name = 'MODULE_NAME_UNDEFINED'

    @TaskAction
    void gen() {
        new File(dir_name).mkdirs();
        def f = new File(dir_name + '/' + module_name + 'BuildInfo.java')
        def git_desc_cmd = "git describe --dirty"
        def git_desc_proc = git_desc_cmd.execute()
        def build_commit = git_desc_proc.text.trim()
        def build_timestamp = (int)(new Date().getTime())
        f.text = "package rapture.buildinfo;\n\npublic class ${module_name}BuildInfo {\n    public static final String commit = \"${build_commit}\";\n    public static final long time = ${build_timestamp}L;\n}"
    }
}
