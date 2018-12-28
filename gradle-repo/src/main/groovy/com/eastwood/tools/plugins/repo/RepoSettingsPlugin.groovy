package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.ModuleInfo
import com.eastwood.tools.plugins.repo.model.RepoInfo
import com.eastwood.tools.plugins.repo.model.RepositoryInfo
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class RepoSettingsPlugin implements Plugin<Settings> {

    Settings settings
    File projectDir

    void apply(Settings settings) {
        this.settings = settings
        projectDir = settings.rootProject.projectDir

        RepoInfo repoInfo = RepoUtil.getRepoInfo(projectDir, false)

        boolean initialized = GitUtil.isGitDir(projectDir)
        if (initialized) {
            String fetchUrl = GitUtil.getOriginRemoteFetchUrl(projectDir)
            if (repoInfo.projectInfo == null || repoInfo.projectInfo.repositoryInfo == null || fetchUrl != repoInfo.projectInfo.repositoryInfo.fetchUrl) {
                throw new RuntimeException("[repo] - project '${settings.rootProject.getName()}': git remote origin fetch url is changed.")
            }

            RepositoryInfo projectRepositoryInfo = repoInfo.projectInfo.repositoryInfo
            if (GitUtil.isBranchChanged(projectDir, projectRepositoryInfo.branch)) {
                isProjectClean()
                if (GitUtil.isLocalBranch(projectDir, projectRepositoryInfo.branch)) {
                    GitUtil.revertRepoFile(projectDir)
                    GitUtil.checkoutBranch(projectDir, projectRepositoryInfo.branch)
                    println "[repo] - project '${settings.rootProject.getName()}': git checkout $projectRepositoryInfo.branch"
                } else {
                    if (GitUtil.isRemoteBranch(projectDir, projectRepositoryInfo.branch)) {
                        GitUtil.revertRepoFile(projectDir)
                        GitUtil.checkoutRemoteBranch(projectDir, projectRepositoryInfo.branch)
                        println "[repo] - project '${settings.rootProject.getName()}': git checkout -b $projectRepositoryInfo.branch origin/$projectRepositoryInfo.branch"
                    } else {
                        GitUtil.checkoutNewBranch(projectDir, projectRepositoryInfo.branch)
                        GitUtil.commitRepoFile(projectDir)
                        println "[repo] - project '${settings.rootProject.getName()}': git checkout -b $projectRepositoryInfo.branch"
                    }
                }
                repoInfo = RepoUtil.getRepoInfo(projectDir, false)
            }
        }

        repoInfo.moduleInfoMap.each {
            ModuleInfo moduleInfo = it.value

            def moduleDir = RepoUtil.getModuleDir(projectDir, moduleInfo)
            def moduleName = RepoUtil.getModuleName(projectDir, moduleDir)

            // include
            settings.include moduleName

            if (repoInfo.projectInfo != null && repoInfo.projectInfo.includeModuleList.contains(moduleInfo.name))
                return

            // module
            if (moduleDir.exists()) {
                boolean moduleInitialized = GitUtil.isGitDir(moduleDir)
                if (moduleInitialized) {
                    String remoteUrl = GitUtil.getOriginRemoteFetchUrl(moduleDir)
                    if (moduleInfo.repositoryInfo == null || remoteUrl != moduleInfo.repositoryInfo.fetchUrl) {
                        throw new RuntimeException("[repo] - module '$moduleName': git remote origin fetch url is changed.")
                    }

                    String branch = moduleInfo.repositoryInfo.branch
                    def currentBranchName = GitUtil.getBranchName(moduleDir)
                    if (currentBranchName != branch) {
                        boolean isClean = GitUtil.isClean(moduleDir)
                        if (isClean) {
                            if (GitUtil.isLocalBranch(moduleDir, branch)) {
                                GitUtil.checkoutBranch(moduleDir, branch)
                                println "[repo] - module '$moduleName': git checkout $branch"
                            } else {
                                if (GitUtil.isRemoteBranch(moduleDir, branch)) {
                                    GitUtil.checkoutRemoteBranch(moduleDir, branch)
                                    println "[repo] - module '$moduleName': git checkout -b $branch origin/$branch"
                                } else {
                                    GitUtil.checkoutNewBranch(moduleDir, branch)
                                    println "[repo] - module '$moduleName': git checkout -b $branch"
                                }
                            }
                        } else {
                            throw new RuntimeException("[repo] - module '$moduleName': please commit or revert changes before checkout other branch.")
                        }
                    }
                } else {
                    if (moduleDir.list().size() > 0) {
                        return
                    }
                    RepositoryInfo repositoryInfo = moduleInfo.repositoryInfo
                    if (repositoryInfo == null) return

                    String originUrl = repositoryInfo.fetchUrl
                    println "[repo] - module '$moduleName': git clone $originUrl -b $repositoryInfo.branch"
                    GitUtil.clone(moduleDir, originUrl, repositoryInfo.branch)
                }
            } else {
                moduleDir.mkdirs()
                RepositoryInfo repositoryInfo = moduleInfo.repositoryInfo
                if (repositoryInfo == null) return

                String originUrl = repositoryInfo.fetchUrl
                println "[repo] - module '$moduleName': git clone $originUrl -b $repositoryInfo.branch"
                GitUtil.clone(moduleDir, originUrl, repositoryInfo.branch)
            }
        }

        if (initialized) {
            RepoUtil.updateExclude(projectDir, repoInfo)
        }

        RepoUtil.saveRepoManifest(projectDir, repoInfo)
    }

    boolean isProjectClean() {
        RepoInfo repoInfo = RepoUtil.getLastRepoManifest(projectDir)
        // check root project
        def process = ("git status -s").execute(null, projectDir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git status -s] under ${projectDir.absolutePath}\n message: ${process.err.text}")
        }
        def info = process.text.readLines()
        int changeSize = info.size()
        boolean isClean = changeSize == 1 && info.get(0).endsWith('repo.xml')
        if (!isClean) {
            GitUtil.revertRepoFile(projectDir)
            throw new RuntimeException("[repo] - project '${settings.rootProject.getName()}': please commit or revert changes before checkout other branch.")
        }

        // check modules
        repoInfo.moduleInfoMap.each {
            if (repoInfo.projectInfo.includeModuleList.contains(it.key)) return

            File moduleDir = RepoUtil.getModuleDir(projectDir, it.value)
            if (!GitUtil.isGitDir(moduleDir)) return

            isClean = GitUtil.isClean(moduleDir)
            if (!isClean) {
                GitUtil.revertRepoFile(projectDir)
                throw new RuntimeException("[repo] - module '${RepoUtil.getModuleName(projectDir, moduleDir)}': please commit or revert changes before checkout other branch.")
            }
        }
    }

}