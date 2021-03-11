package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.ModuleInfo
import com.eastwood.tools.plugins.repo.model.RemoteInfo
import com.eastwood.tools.plugins.repo.model.RepoInfo
import com.eastwood.tools.plugins.repo.utils.GitUtils
import com.eastwood.tools.plugins.repo.utils.RepoUtils
import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings

class RepoSettingsPlugin implements Plugin<Settings> {

    Settings settings
    File projectDir

    void apply(Settings settings) {
        def disableLocalRepo = false
        if (settings.ext.has('disableLocalRepo')) {
            disableLocalRepo = settings.ext.disableLocalRepo
        }
        settings.gradle.metaClass.disableLocalRepo = disableLocalRepo

        this.settings = settings
        projectDir = settings.rootProject.projectDir

        def repoModulesDir = new File(projectDir, '.idea/repoModules')
        if (!repoModulesDir.exists()) {
            repoModulesDir.mkdirs()
        }

        RepoInfo repoInfo = RepoUtils.getRepoInfo(projectDir, false, disableLocalRepo)

        boolean initialized = GitUtils.isGitDir(projectDir)
        if (!initialized) {
            GitUtils.init(projectDir)
        }

        RemoteInfo projectRemoteInfo = repoInfo.projectInfo.remoteInfo
        setRemote(projectDir, projectRemoteInfo)

        def currentProjectBranch = GitUtils.getBranchName(projectDir)
        repoInfo.projectInfo.branch = currentProjectBranch

        repoInfo.moduleInfoMap.each {
            ModuleInfo moduleInfo = it.value
            if (moduleInfo.branch == null) {
                moduleInfo.branch = currentProjectBranch
            }

            def moduleDir = RepoUtils.getModuleDir(projectDir, moduleInfo)
            def moduleName = RepoUtils.getModuleName(projectDir, moduleDir)

            if(!moduleName.equals(":buildSrc")) {
                // include
                settings.include moduleName
            }

            if (repoInfo.projectInfo.includeModuleList.contains(moduleInfo.name)) {
                GitUtils.clearGitDir(moduleDir)
                return
            }

            if (moduleInfo.hide) {
                if (moduleDir.exists()) {
                    if (GitUtils.isGitDir(moduleDir)) {
                        if (!GitUtils.isClean(moduleDir)) {
                            throw new RuntimeException("[repo] - module '$moduleName': please commit or revert changes before it been removed to .repo/modules.")
                        }
                        moduleDir.eachFile {
                            if (it.name == 'build') {
                                it.deleteDir()
                            } else if (it.name.endsWith('.iml')) {
                                it.delete()
                            }
                        }

                        def targetDir = RepoUtils.getModuleDir(repoModulesDir, moduleInfo)
                        if (targetDir.exists()) {
                            if (targetDir.list().size() == 0) {
                                def result = targetDir.deleteDir()
                                if (!result) {
                                    throw new RuntimeException("[repo] - module '$moduleName': failure to delete $targetDir.absolutePath.")
                                }
                            } else {
                                throw new RuntimeException("[repo] - module '$moduleName': unable to move module to $targetDir.absolutePath, because it is not empty.")
                            }
                        }

                        def result = moduleDir.renameTo(targetDir)
                        if (!result) {
                            throw new RuntimeException("[repo] - module '$moduleName': failure to rename $moduleDir.absolutePath to $targetDir.absolutePath.")
                        }
                    }
                }

                moduleDir = RepoUtils.getModuleDir(repoModulesDir, moduleInfo)
                if(!moduleName.equals(":buildSrc")) {
                    settings.project(moduleName).projectDir = moduleDir
                }
            } else {
                def targetDir = RepoUtils.getModuleDir(repoModulesDir, moduleInfo)
                if (targetDir.exists() && targetDir.list().size() > 0) {
                    targetDir.eachFile {
                        if (it.name == 'build') {
                            it.deleteDir()
                        } else if (it.name.endsWith('.iml')) {
                            it.delete()
                        }
                    }

                    if (moduleDir.exists()) {
                        if (moduleDir.list().size() == 0) {
                            def result = moduleDir.deleteDir()
                            if (!result) {
                                throw new RuntimeException("[repo] - module '$moduleName': failure to delete $moduleDir.absolutePath.")
                            }
                        } else {
                            throw new RuntimeException("[repo] - module '$moduleName': unable to move module to $moduleDir.absolutePath, because it is not empty.")
                        }
                    }

                    def result = targetDir.renameTo(moduleDir)
                    if (!result) {
                        throw new RuntimeException("[repo] - module '$moduleName': failure to rename $targetDir.absolutePath to $moduleDir.absolutePath.")
                    }
                }
            }

            // module
            if (moduleDir.exists()) {
                GitUtils.removeCachedDir(projectDir, moduleDir.canonicalPath)

                boolean moduleInitialized = GitUtils.isGitDir(moduleDir)
                if (moduleInitialized) {
                    setRemote(moduleDir, moduleInfo.remoteInfo)

                    String branch = moduleInfo.branch
                    def currentBranchName = GitUtils.getBranchName(moduleDir)
                    if (currentBranchName != branch) {
                        boolean isClean = GitUtils.isClean(moduleDir)
                        if (isClean) {
                            if (moduleInfo.remoteInfo == null) {
                                if (GitUtils.isLocalBranch(moduleDir, branch)) {
                                    GitUtils.checkoutBranch(moduleDir, branch)
                                    println "[repo] - module '$moduleName': git checkout $branch"
                                } else {
                                    GitUtils.checkoutNewBranch(moduleDir, branch)
                                    println "[repo] - module '$moduleName': git checkout -b $branch"
                                }
                            } else {
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
                            }
                        } else {
                            throw new RuntimeException("[repo] - module '$moduleName': please commit or revert changes before checkout branch '$branch', current branch is '$currentBranchName'.")
                        }
                    }
                } else {
                    RemoteInfo remoteInfo = moduleInfo.remoteInfo
                    if (moduleDir.list().size() > 0) {
                        GitUtils.init(moduleDir)
                        if (remoteInfo != null) {
                            GitUtils.addRemote(moduleDir, remoteInfo.fetchUrl)
                        }
                    } else {
                        if (remoteInfo != null) {
                            String originUrl = remoteInfo.fetchUrl
                            println "[repo] - module '$moduleName': git clone $originUrl --branch $moduleInfo.branch"
                            GitUtils.clone(moduleDir, originUrl, moduleInfo.branch)
                            if (remoteInfo.pushUrl != remoteInfo.fetchUrl) {
                                GitUtils.setOriginRemotePushUrl(moduleDir, remoteInfo.pushUrl)
                            }
                        }
                    }
                    GitUtils.addExclude(moduleDir)
                }
            } else {
                moduleDir.mkdirs()
                RemoteInfo remoteInfo = moduleInfo.remoteInfo
                if (remoteInfo == null) return

                String originUrl = remoteInfo.fetchUrl
                println "[repo] - module '$moduleName': git clone $originUrl --branch $moduleInfo.branch"
                GitUtils.clone(moduleDir, originUrl, moduleInfo.branch)
                if (remoteInfo.pushUrl != remoteInfo.fetchUrl) {
                    GitUtils.setOriginRemotePushUrl(moduleDir, remoteInfo.pushUrl)
                }
                GitUtils.addExclude(moduleDir)
            }
        }

        if (initialized) {
            GitUtils.updateExclude(projectDir, repoInfo)
        }
    }

    void setRemote(File dir, RemoteInfo remoteInfo) {
        if (remoteInfo == null) {
            GitUtils.removeRemote(dir)
            return
        }

        String fetchUrl = GitUtils.getOriginRemoteFetchUrl(dir)
        if (fetchUrl == null) {
            GitUtils.addRemote(dir, remoteInfo.fetchUrl)
        } else if (remoteInfo.fetchUrl != null && remoteInfo.fetchUrl != fetchUrl) {
            GitUtils.setOriginRemoteUrl(dir, remoteInfo.fetchUrl)
        }

        String pushUrl = GitUtils.getOriginRemotePushUrl(dir)
        if (pushUrl != remoteInfo.pushUrl) {
            GitUtils.setOriginRemotePushUrl(dir, remoteInfo.pushUrl)
        }
    }

}