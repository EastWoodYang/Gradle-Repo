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

        clearSettingsIncludeIfAddNewModule()

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

                def currentProjectBranch = GitUtils.getBranchName(projectDir)
                repoInfo.projectInfo.repositoryInfo.branch = currentProjectBranch
                repoInfo.moduleInfoMap.each {
                    ModuleInfo moduleInfo = it.value
                    if(moduleInfo.repositoryInfo != null) {
                        moduleInfo.repositoryInfo.branch = currentProjectBranch
                    }
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
                GitUtils.removeCachedDir(projectDir, moduleDir.canonicalPath)

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
                        println "[repo] - module '$moduleName': git clone $originUrl --branch $repositoryInfo.branch"
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
                println "[repo] - module '$moduleName': git clone $originUrl --branch $repositoryInfo.branch"
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

    void clearSettingsIncludeIfAddNewModule() {
        def settingsFile = new File(projectDir, 'settings.gradle')
        if (!settingsFile.exists()) return

        def newModuleName = null
        def content = ''
        settingsFile.readLines('utf-8').each {
            def line = it.trim()
            if (line.startsWith("include ") && line.endsWith("'")) {
                newModuleName = line.substring(line.indexOf(":") + 1, line.lastIndexOf("'"))
            } else {
                content += it + '\n'
            }
        }

        if (newModuleName != null) {
            def newModuleDir = new File(projectDir, newModuleName)
            if (newModuleDir.exists()) {
                RepoUtils.addNewModuleElement(projectDir, newModuleName)
                settingsFile.setText(content, 'utf-8')
            }
        }
    }

}