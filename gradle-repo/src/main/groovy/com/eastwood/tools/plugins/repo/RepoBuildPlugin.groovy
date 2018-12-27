package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.tasks.TaskContainer

class RepoBuildPlugin implements Plugin<Project> {

    Project project
    File projectDir

    void apply(Project project) {
        this.project = project
        projectDir = project.rootProject.projectDir

        project.afterEvaluate {
            RepoInfo repoInfo = RepoUtil.getRepoInfo(projectDir, true)

            project.getAllprojects().each {
                def currentProject = it
                ModuleInfo moduleInfo = repoInfo.moduleInfoMap.get(currentProject.name)
                if (moduleInfo == null) return

                if (moduleInfo.dependencyMap.size() > 0) {
                    currentProject.afterEvaluate {
                        repoInfo.configurations.configurationMap.each {
                            Configuration configuration = it.value
                            org.gradle.api.artifacts.Configuration gradleConfiguration = currentProject.configurations.findByName(configuration.name)
                            if (gradleConfiguration == null) {
                                gradleConfiguration = currentProject.configurations.create(configuration.name)
                            }
                            gradleConfiguration.setTransitive(configuration.transitive)
                            configuration.excludeList.each {
                                Map<String, String> map = new HashMap<>()
                                if (!it.group.isEmpty()) {
                                    map.put(ExcludeRule.GROUP_KEY, it.group)
                                }
                                if (!it.name.isEmpty()) {
                                    map.put(ExcludeRule.MODULE_KEY, it.name)
                                }
                                gradleConfiguration.exclude(map)
                            }
                        }

                        currentProject.configurations.all {
                            org.gradle.api.artifacts.Configuration configuration = it

                            repoInfo.configurations.forceList.each {
                                configuration.resolutionStrategy.force(it.toString())
                            }

                            repoInfo.configurations.excludeList.each {
                                Map<String, String> map = new HashMap<>()
                                if (!it.group.isEmpty()) {
                                    map.put(ExcludeRule.GROUP_KEY, it.group)
                                }
                                if (!it.name.isEmpty()) {
                                    map.put(ExcludeRule.MODULE_KEY, it.name)
                                }
                                configuration.exclude(map)
                            }

                            if (!repoInfo.configurations.substituteMap.isEmpty()) {
                                configuration.resolutionStrategy {
                                    repoInfo.configurations.substituteMap.each {
                                        ModuleInfo dependencyModuleInfo = repoInfo.moduleInfoMap.get(it.key)
                                        if (dependencyModuleInfo == null) {
                                            throw new RuntimeException("[repo] - can not find module with name [${it.name}].")
                                        }
                                        def moduleDir = RepoUtil.getModuleDir(projectDir, dependencyModuleInfo)
                                        def moduleName = RepoUtil.getModuleName(projectDir, moduleDir)
                                        dependencySubstitution.substitute(dependencySubstitution.module(it.value)).with(dependencySubstitution.project(moduleName))
                                    }
                                }
                            }
                        }

                        moduleInfo.dependencyMap.each {
                            String type = it.key
                            List<Dependency> dependencyList = it.value
                            dependencyList.each {
                                ModuleDependency moduleDependency
                                if (it.group.isEmpty() && it.version.isEmpty()) {
                                    ModuleInfo dependencyModuleInfo = repoInfo.moduleInfoMap.get(it.name)
                                    if (dependencyModuleInfo == null) {
                                        throw new RuntimeException("[repo] - can not find module with name [${it.name}].")
                                    }
                                    def moduleDir = RepoUtil.getModuleDir(projectDir, dependencyModuleInfo)
                                    def moduleName = RepoUtil.getModuleName(projectDir, moduleDir)
                                    moduleDependency = currentProject.dependencies.add(type, currentProject.project(moduleName))

                                } else if (!it.group.isEmpty() && !it.name.isEmpty() && !it.version.isEmpty()) {
                                    moduleDependency = currentProject.dependencies.add(type, it.group + ':' + it.name + ':' + it.version)
                                }

                                if (moduleDependency != null) {
                                    it.excludes.each {
                                        Map<String, String> map = new HashMap<>()
                                        if (!it.group.isEmpty()) {
                                            map.put(ExcludeRule.GROUP_KEY, it.group)
                                        }
                                        if (!it.name.isEmpty()) {
                                            map.put(ExcludeRule.MODULE_KEY, it.name)
                                        }
                                        moduleDependency.exclude(map)
                                    }
                                    moduleDependency.setTransitive(it.transitive)
                                }
                            }
                        }
                    }
                }

                if (!repoInfo.projectInfo.includeModuleList.contains(moduleInfo.name)) {
                    def moduleDir = RepoUtil.getModuleDir(projectDir, moduleInfo)
                    if (moduleInfo.repositoryInfo != null && !GitUtil.isGitDir(moduleDir)) {
                        createBindRemoteRepositoryTask(currentProject, moduleInfo.name, moduleDir, moduleInfo.repositoryInfo)
                    }
                }
            }

            if (!GitUtil.isGitDir(projectDir)) {
                createBindRemoteRepositoryTask(project, "project", projectDir, repoInfo.projectInfo.repositoryInfo)
            }
        }
    }

    void createBindRemoteRepositoryTask(Project project, String name, File moduleDir, RepositoryInfo repositoryInfo) {
        TaskContainer tasks = project.getTasks()
        BindRemoteRepositoryTask uploadModuleTask
        uploadModuleTask = tasks.create("bindRemoteRepository", BindRemoteRepositoryTask.class)
        uploadModuleTask.setDescription("create remote origin repository by gitlab api and bind it.")
        uploadModuleTask.setGroup('repo')
        uploadModuleTask.moduleDir = moduleDir
        uploadModuleTask.elementName = name
        uploadModuleTask.repositoryInfo = repositoryInfo
    }

}