package com.eastwood.tools.plugins.repo

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class BuildRepoProjectTask extends DefaultTask {

    @TaskAction
    void build() {
        Project rootProject = project.getRootProject()
        def repoFile = new File(rootProject.projectDir, 'repo.xml')
        if (repoFile.exists()) {
            println "[repo] - repo.xml already exist."
            return
        }

        String repoXml = "<?xml version='1.0' encoding='UTF-8'?>\n<manifest>\n\n"
        repoXml += "    <project />\n\n"

        rootProject.subprojects.each {
            if (!new File(it.projectDir, "build.gradle").exists()) {
                return
            }

            repoXml += "    <module name=\"$it.name\""

            String local = it.projectDir.parentFile.absolutePath.replace(rootProject.projectDir.absolutePath, "")
            if (local.startsWith("\\") || local.startsWith("/")) {
                local = local.substring(1)
            }
            if (local.trim() != "") {
                repoXml += "\n        local=\"$local\" />\n\n"
            } else {
                repoXml += " />\n\n"
            }
        }
        repoXml += "</manifest>"
        repoFile.write(repoXml)

        def settings = new File(rootProject.projectDir, 'settings.gradle')
        settings.write("buildscript {\n" +
                "    repositories {\n" +
                "        jcenter()\n" +
                "        google()\n" +
                "    }\n" +
                "    dependencies {\n" +
                "        classpath 'com.winwin.common:gradle-repo-plugin:1.0.0-SNAPSHOT'\n" +
                "    }\n" +
                "}\n" +
                "\n" +
                "apply plugin: 'gradle-repo-settings'")
    }

}