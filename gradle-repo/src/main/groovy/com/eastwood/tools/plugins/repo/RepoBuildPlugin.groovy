package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.Dependency
import com.eastwood.tools.plugins.repo.model.ModuleInfo
import com.eastwood.tools.plugins.repo.model.RepoInfo
import com.eastwood.tools.plugins.repo.utils.RepoUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class RepoBuildPlugin implements Plugin<Project> {

    Project project
    File projectDir

    void apply(Project project) {
        this.project = project
        projectDir = project.rootProject.projectDir

        project.afterEvaluate {
            def disableLocalRepo = project.gradle.hasProperty('disableLocalRepo') ? project.gradle.disableLocalRepo : false
            RepoInfo repoInfo = RepoUtils.getRepoInfo(projectDir, true, disableLocalRepo)

            project.getAllprojects().each {
                def currentProject = it
                ModuleInfo moduleInfo = repoInfo.moduleInfoMap.get(currentProject.name)
                if (moduleInfo == null) return

                if (moduleInfo.dependencyMap.size() > 0) {
                    currentProject.afterEvaluate {
                        if (repoInfo.substituteMap != null && !repoInfo.substituteMap.isEmpty()) {
                            currentProject.configurations.all {
                                it.resolutionStrategy {
                                    repoInfo.substituteMap.each {
                                        ModuleInfo dependencyModuleInfo = repoInfo.moduleInfoMap.get(it.key)
                                        def moduleDir = RepoUtils.getModuleDir(projectDir, dependencyModuleInfo)
                                        def moduleName = RepoUtils.getModuleName(projectDir, moduleDir)
                                        dependencySubstitution.substitute(dependencySubstitution.module(it.value)).with(dependencySubstitution.project(moduleName))
                                    }
                                }
                            }
                        }

                        moduleInfo.dependencyMap.each {
                            String type = it.key
                            List<Dependency> dependencyList = it.value
                            dependencyList.each {
                                ModuleInfo dependencyModuleInfo = repoInfo.moduleInfoMap.get(it.name)
                                if (dependencyModuleInfo == null) return

                                def moduleDir = RepoUtils.getModuleDir(projectDir, dependencyModuleInfo)
                                def moduleName = RepoUtils.getModuleName(projectDir, moduleDir)
                                currentProject.dependencies.add(type, currentProject.project(moduleName))
                            }
                        }

                    }
                }
            }
        }
    }

}