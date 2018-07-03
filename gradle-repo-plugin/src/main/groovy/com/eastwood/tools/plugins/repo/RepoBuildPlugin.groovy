package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.ModuleInfo
import com.eastwood.tools.plugins.repo.model.RepoInfo
import com.eastwood.tools.plugins.repo.model.RepositoryInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskContainer

class RepoBuildPlugin implements Plugin<Project> {

    Project project
    File projectDir

    void apply(Project project) {
        this.project = project
        projectDir = project.rootProject.projectDir
        def repoXml = new File(projectDir, 'repo.xml')
        if (!repoXml.exists()) {
            createBuildRepoProjectTask()
            println "[repo] - can't find repo.xml under root project."
            return
        }
        project.afterEvaluate {

            RepoInfo repoInfo = RepoUtil.getRepoInfo(repoXml, true)

            if (!GitUtil.isGitDir(projectDir)) {
                createBindRemoteRepositoryTask(project, "project", projectDir, repoInfo.projectInfo.repositoryInfo)
            }

            project.getAllprojects().each {
                def currentProject = it
                ModuleInfo moduleInfo = repoInfo.moduleInfoMap.get(currentProject.name)
                if (moduleInfo == null) return

                if (moduleInfo.dependencyMap.size() > 0) {
                    currentProject.afterEvaluate {
                        moduleInfo.dependencyMap.each {
                            String type = it.key
                            List<String> dependencyList = it.value
                            dependencyList.each {
                                ModuleInfo dependencyModuleInfo = repoInfo.moduleInfoMap.get(it)
                                if(dependencyModuleInfo == null) {
                                    throw new IllegalArgumentException("[repo] - can't find module name [${it}].")
                                }
                                def dependencyModuleDir = RepoUtil.getModuleDir(projectDir, dependencyModuleInfo)
                                def moduleName = RepoUtil.getModuleName(projectDir, dependencyModuleDir)
                                currentProject.dependencies.add(type, currentProject.project(moduleName))
                            }
                        }
                    }
                }

                if (repoInfo.projectInfo.includeModuleList.contains(moduleInfo.name)) return

                def moduleDir = RepoUtil.getModuleDir(projectDir, moduleInfo)
                if (moduleInfo.repositoryInfo != null && !GitUtil.isGitDir(moduleDir)) {
                    createBindRemoteRepositoryTask(currentProject, moduleInfo.name, moduleDir, moduleInfo.repositoryInfo)
                }

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

    void createBuildRepoProjectTask() {
        BuildRepoProjectTask buildRepoProjectTask = project.getTasks().create("createRepo", BuildRepoProjectTask.class)
        buildRepoProjectTask.setDescription("create repo.xml.")
        buildRepoProjectTask.setGroup('repo')
    }
}