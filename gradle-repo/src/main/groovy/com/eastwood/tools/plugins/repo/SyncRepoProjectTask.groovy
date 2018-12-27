package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.ModuleInfo
import com.eastwood.tools.plugins.repo.model.RepoInfo
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class SyncRepoProjectTask extends DefaultTask {

    @TaskAction
    void build() {
        Project rootProject = project.getRootProject()
        RepoInfo repoInfo = RepoUtil.getRepoInfo(rootProject.projectDir, true)

        boolean initialized = GitUtil.isGitDir(rootProject.projectDir)
        String repoXml = ""
        rootProject.subprojects.each {
            if (!new File(it.projectDir, "build.gradle").exists()) return

            // it is a gradle module.

            ModuleInfo moduleInfo = repoInfo.moduleInfoMap.get(it.name)
            if (moduleInfo != null) return

            // it is new module.

            // add it to repo.xml
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
        if (repoXml != "") {
            // write to repo.xml
            repoXml += "</manifest>"
            repoFile.write(repoFile.text.replace("</manifest>", repoXml))
        }

        if (!initialized) return

        repoInfo = RepoUtil.getRepoInfo(rootProject.projectDir, false)
        RepoUtil.updateExclude(rootProject.projectDir, repoInfo)
    }

}