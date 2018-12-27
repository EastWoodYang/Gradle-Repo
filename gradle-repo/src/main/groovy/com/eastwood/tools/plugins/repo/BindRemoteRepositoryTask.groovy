package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.RepoInfo
import com.eastwood.tools.plugins.repo.model.RepositoryInfo
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class BindRemoteRepositoryTask extends DefaultTask {

    File moduleDir
    String elementName
    RepositoryInfo repositoryInfo

    @TaskAction
    void bind() {
        if (repositoryInfo == null) {
            if (elementName == 'project') {
                throw new IllegalArgumentException("[repo] - <project /> element [origin] is not set.")
            } else {
                throw new IllegalArgumentException("[repo] - <module name=\"$elementName\"/> element [origin] is not set.")
            }
        }

        bindRemoteProject(repositoryInfo.fetchUrl)
    }

    void bindRemoteProject(String repo) {
        GitUtil.init(moduleDir)
        if (elementName == 'project') {
            RepoInfo repoInfo = RepoUtil.getRepoInfo(moduleDir, false)
            RepoUtil.updateExclude(moduleDir, repoInfo)
        }

        def gitignore = new File(moduleDir, '.gitignore')
        def ignore = gitignore.getText("utf-8")
        if(!ignore.contains("*.iml")) {
            ignore += "\n*.iml"
            gitignore.setText(ignore, "utf-8")
        }
        GitUtil.addRemote(moduleDir, repo)
        GitUtil.addFiles(moduleDir, ".")

        if (repositoryInfo.branch != "master") {
            GitUtil.checkoutNewBranch(moduleDir, repositoryInfo.branch)
        }
    }

}