package com.incapture.rapgen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

class GenSdkApi extends GenApi {

    @Override
    String getGenType() {
        return 'SDK';
    }

}
