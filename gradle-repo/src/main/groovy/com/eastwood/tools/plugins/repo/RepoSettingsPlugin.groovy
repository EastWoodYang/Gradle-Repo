package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.ModuleInfo
import com.eastwood.tools.plugins.repo.model.RepoInfo
import com.eastwood.tools.plugins.repo.model.RepositoryInfo
import com.eastwood.tools.plugins.repo.utils.GitUtils
import com.eastwood.tools.plugins.repo.utils.RepoUtils
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class RepoSettingsPlugin implements Plugin<Settings> {

    Settings settings
    File projectDir

    void apply(Settings settings) {
        this.settings = settings
        projectDir = settings.rootProject.projectDir

        RepoInfo lastRepoInfo = RepoUtils.getLastRepoManifest(projectDir)
        RepoInfo repoInfo = RepoUtils.getRepoInfo(projectDir, false)

        boolean initialized = GitUtils.isGitDir(projectDir)
        if (initialized) {
            RepositoryInfo projectRepositoryInfo = repoInfo.projectInfo.repositoryInfo
            if (projectRepositoryInfo != null) {
                String fetchUrl = GitUtils.getOriginRemoteFetchUrl(projectDir)
                if (fetchUrl != projectRepositoryInfo.fetchUrl) {
                    GitUtils.setOriginRemoteUrl(projectDir, projectRepositoryInfo.fetchUrl)
                }

                String pushUrl = GitUtils.getOriginRemotePushUrl(projectDir)
                if (pushUrl != projectRepositoryInfo.pushUrl) {
                    GitUtils.setOriginRemotePushUrl(projectDir, projectRepositoryInfo.pushUrl)
                }

                if (GitUtils.isBranchChanged(projectDir, projectRepositoryInfo.branch)) {
                    isProjectClean()
                    if (GitUtils.isLocalBranch(projectDir, projectRepositoryInfo.branch)) {
                        GitUtils.revertRepoFile(projectDir)
                        GitUtils.checkoutBranch(projectDir, projectRepositoryInfo.branch)
                        println "[repo] - project '${settings.rootProject.getName()}': git checkout $projectRepositoryInfo.branch"
                    } else {
                        if (GitUtils.isRemoteBranch(projectDir, projectRepositoryInfo.branch)) {
                            GitUtils.revertRepoFile(projectDir)
                            GitUtils.checkoutRemoteBranch(projectDir, projectRepositoryInfo.branch)
                            println "[repo] - project '${settings.rootProject.getName()}': git checkout -b $projectRepositoryInfo.branch origin/$projectRepositoryInfo.branch"
                        } else {
                            GitUtils.checkoutNewBranch(projectDir, projectRepositoryInfo.branch)
                            GitUtils.commitRepoFile(projectDir)
                            println "[repo] - project '${settings.rootProject.getName()}': git checkout -b $projectRepositoryInfo.branch"
                        }
                    }
                    repoInfo = RepoUtils.getRepoInfo(projectDir, false)
                }
            } else {
                initialized = false
                GitUtils.clearGitDir(projectDir)
            }
        } else {
            if (repoInfo.projectInfo.repositoryInfo != null) {
                GitUtils.init(projectDir)
                GitUtils.commitRepoFile(projectDir)
                GitUtils.addRemote(projectDir, repoInfo.projectInfo.repositoryInfo.fetchUrl)
            }
        }

        repoInfo.moduleInfoMap.each {
            ModuleInfo moduleInfo = it.value

            def moduleDir = RepoUtils.getModuleDir(projectDir, moduleInfo)
            def moduleName = RepoUtils.getModuleName(projectDir, moduleDir)

            // include
            settings.include moduleName

            if (repoInfo.projectInfo.includeModuleList.contains(moduleInfo.name)) {
                GitUtils.clearGitDir(moduleDir)
                return
            }

            // module
            if (moduleDir.exists()) {
                boolean moduleInitialized = GitUtils.isGitDir(moduleDir)
                if (moduleInitialized) {
                    if (moduleInfo.repositoryInfo != null) {
                        String remoteUrl = GitUtils.getOriginRemoteFetchUrl(moduleDir)
                        if (remoteUrl != moduleInfo.repositoryInfo.fetchUrl) {
                            GitUtils.setOriginRemoteUrl(moduleDir, moduleInfo.repositoryInfo.fetchUrl)
                        }

                        String pushUrl = GitUtils.getOriginRemotePushUrl(moduleDir)
                        if (pushUrl != moduleInfo.repositoryInfo.pushUrl) {
                            GitUtils.setOriginRemotePushUrl(moduleDir, moduleInfo.repositoryInfo.pushUrl)
                        }
                    } else {
                        GitUtils.clearGitDir(moduleDir)
                        return
                    }

                    String branch = moduleInfo.repositoryInfo.branch
                    def currentBranchName = GitUtils.getBranchName(moduleDir)
                    if (currentBranchName != branch) {
                        boolean isClean = GitUtils.isClean(moduleDir)
                        if (isClean) {
                            if (GitUtils.isLocalBranch(moduleDir, branch)) {
                                GitUtils.checkoutBranch(moduleDir, branch)
                                println "[repo] - module '$moduleName': git checkout $branch"
                            } else {
                                if (GitUtils.isRemoteBranch(moduleDir, branch)) {
                                    GitUtils.checkoutRemoteBranch(moduleDir, branch)
                                    println "[repo] - module '$moduleName': git checkout -b $branch origin/$branch"
                                } else {
                                    GitUtils.checkoutNewBranch(moduleDir, branch)
                                    println "[repo] - module '$moduleName': git checkout -b $branch"
                                }
                            }
                        } else {
                            throw new RuntimeException("[repo] - module '$moduleName': please commit or revert changes before checkout other branch.")
                        }
                    }
                } else {
                    RepositoryInfo repositoryInfo = moduleInfo.repositoryInfo
                    if (repositoryInfo == null) return

                    ModuleInfo lastModuleInfo = lastRepoInfo.moduleInfoMap.get(moduleInfo.name)
                    if (lastModuleInfo == null || lastModuleInfo.repositoryInfo == null) {
                        GitUtils.init(moduleDir)
                        GitUtils.addRemote(moduleDir, repositoryInfo.fetchUrl)
                        GitUtils.addExclude(moduleDir)
                    } else {
                        if (moduleDir.list().size() > 0) {
                            throw new RuntimeException("[repo] - module '$moduleName': failure to clone beacause [$moduleDir.absolutePath] is not an empty directory.")
                        }

                        String originUrl = repositoryInfo.fetchUrl
                        println "[repo] - module '$moduleName': git clone $originUrl -b $repositoryInfo.branch"
                        GitUtils.clone(moduleDir, originUrl, repositoryInfo.branch)
                        if (repositoryInfo.pushUrl != repositoryInfo.fetchUrl) {
                            GitUtils.setOriginRemotePushUrl(moduleDir, repositoryInfo.pushUrl)
                        }
                        GitUtils.addExclude(moduleDir)
                    }
                }
            } else {
                moduleDir.mkdirs()
                RepositoryInfo repositoryInfo = moduleInfo.repositoryInfo
                if (repositoryInfo == null) return

                String originUrl = repositoryInfo.fetchUrl
                println "[repo] - module '$moduleName': git clone $originUrl -b $repositoryInfo.branch"
                GitUtils.clone(moduleDir, originUrl, repositoryInfo.branch)
                if (repositoryInfo.pushUrl != repositoryInfo.fetchUrl) {
                    GitUtils.setOriginRemotePushUrl(moduleDir, repositoryInfo.pushUrl)
                }
                GitUtils.addExclude(moduleDir)
            }
        }

        if (initialized) {
            GitUtils.updateExclude(projectDir, repoInfo)
        }

        RepoUtils.saveRepoManifest(projectDir, repoInfo)
    }

    boolean isProjectClean() {
        RepoInfo repoInfo = RepoUtils.getLastRepoManifest(projectDir)
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
            GitUtils.revertRepoFile(projectDir)
            throw new RuntimeException("[repo] - project '${settings.rootProject.getName()}': please commit or revert changes before checkout other branch.")
        }

        // check modules
        repoInfo.moduleInfoMap.each {
            if (repoInfo.projectInfo.includeModuleList.contains(it.key)) return

            File moduleDir = RepoUtils.getModuleDir(projectDir, it.value)
            if (!GitUtils.isGitDir(moduleDir)) return

            isClean = GitUtils.isClean(moduleDir)
            if (!isClean) {
                GitUtils.revertRepoFile(projectDir)
                throw new RuntimeException("[repo] - module '${RepoUtils.getModuleName(projectDir, moduleDir)}': please commit or revert changes before checkout other branch.")
            }
        }
    }

}