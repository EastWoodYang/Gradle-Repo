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

        def repoFile = new File(projectDir, 'repo.xml')
        if (!repoFile.exists()) {
            println "[repo] - can't find file [repo.xml] under root project."
            return
        }

        RepoInfo repoInfo = RepoUtil.getRepoInfo(repoFile, false)
        boolean initialized = GitUtil.isGitDir(projectDir)
        if (initialized) {
            String remoteUrl = GitUtil.getRemoteUrl(projectDir)
            if (repoInfo.projectInfo == null || repoInfo.projectInfo.repositoryInfo == null || remoteUrl != repoInfo.projectInfo.repositoryInfo.originInfo.getOriginUrl()) {
                throw new RuntimeException("[repo] - project git remote origin repository is changed.")
            }
            RepositoryInfo projectRepositoryInfo = repoInfo.projectInfo.repositoryInfo
            if (GitUtil.isBranchChanged(projectDir, projectRepositoryInfo.branch)) {
                isProjectClean(repoInfo)
                if (GitUtil.isLocalBranch(projectDir, projectRepositoryInfo.branch)) {
                    GitUtil.revertRepoFile(projectDir)
                    GitUtil.checkoutBranch(projectDir, projectRepositoryInfo.branch)
                    println "[repo] - git checkout $projectRepositoryInfo.branch"
                } else {
                    if (GitUtil.isRemoteBranch(projectDir, projectRepositoryInfo.branch)) {
                        GitUtil.revertRepoFile(projectDir)
                        GitUtil.checkoutRemoteBranch(projectDir, projectRepositoryInfo.branch)
                        println "[repo] - git checkout -b $projectRepositoryInfo.branch origin/$projectRepositoryInfo.branch"
                    } else {
                        GitUtil.checkoutNewBranch(projectDir, projectRepositoryInfo.branch)
                        GitUtil.commitRepoFile(projectDir)
                        println "[repo] - git checkout -b $projectRepositoryInfo.branch"
                    }
                }
                repoInfo = RepoUtil.getRepoInfo(repoFile, false)
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
                    String remoteUrl = GitUtil.getRemoteUrl(moduleDir)
                    if (moduleInfo.repositoryInfo == null || remoteUrl != moduleInfo.repositoryInfo.originInfo.getOriginUrl()) {
                        throw new RuntimeException("[repo] - module [$moduleName] git remote origin repository is changed.")
                    }

                    String branch = moduleInfo.repositoryInfo.branch
                    def currentBranchName = GitUtil.getBranchName(moduleDir)
                    if (currentBranchName != branch) {
                        boolean isClean = GitUtil.isClean(moduleDir)
                        if (isClean) {
                            if (GitUtil.isLocalBranch(moduleDir, branch)) {
                                GitUtil.checkoutBranch(moduleDir, branch)
                                println "[repo] - git checkout $branch"
                            } else {
                                if (GitUtil.isRemoteBranch(moduleDir, branch)) {
                                    GitUtil.checkoutRemoteBranch(moduleDir, branch)
                                    println "[repo] - git checkout -b $branch origin/$branch"
                                } else {
                                    GitUtil.checkoutNewBranch(moduleDir, branch)
                                    println "[repo] - git checkout -b $branch"
                                }
                            }
                        } else {
                            throw new RuntimeException("[repo] - module [$moduleName] changes not staged for commit.")
                        }
                    }
                } else {
                    if (moduleDir.list().size() > 0) {
                        return
                    }
                    RepositoryInfo repositoryInfo = moduleInfo.repositoryInfo
                    if (repositoryInfo == null) return

                    String originUrl = repositoryInfo.originInfo.getOriginUrl()
                    println "[repo] - git clone $originUrl"
                    GitUtil.clone(moduleDir, originUrl, repositoryInfo.branch)
                }
            } else {
                moduleDir.mkdirs()
                RepositoryInfo repositoryInfo = moduleInfo.repositoryInfo
                if (repositoryInfo == null) return

                String originUrl = repositoryInfo.originInfo.getOriginUrl()
                println "[repo] - git clone $originUrl"
                GitUtil.clone(moduleDir, originUrl, repositoryInfo.branch)
            }
        }

        if (!initialized) return

        RepoUtil.updateExclude(projectDir, repoInfo)
    }

    boolean isProjectClean(RepoInfo repoInfo) {
        // check root project
        def process = ("git status -s").execute(null, projectDir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - git fail to execute [git status -s] under ${projectDir.absolutePath}\n message: ${process.err.text}")
        }
        def info = process.text
        int changeSize = info.split("\n").size()
        boolean isClean = changeSize <= 2 && info.contains(" repo.xml\n")
        if (!isClean) {
            GitUtil.revertRepoFile(projectDir)
            throw new RuntimeException("[repo] - ${settings.rootProject.getName()}: changes not staged for commit.")
        }

        // check modules
        repoInfo.moduleInfoMap.each {
            if (repoInfo.projectInfo.includeModuleList.contains(it.key)) return

            File moduleDir = RepoUtil.getModuleDir(projectDir, it.value)
            if (!GitUtil.isGitDir(moduleDir)) return

            isClean = GitUtil.isClean(moduleDir)
            if (!isClean) {
                GitUtil.revertRepoFile(projectDir)
                throw new RuntimeException("[repo] - ${RepoUtil.getModuleName(projectDir, moduleDir)}: changes not staged for commit.")
            }
        }

    }

}