package com.eastwood.tools.plugins.repo.model

class RepoInfo {

    RepositoryInfo defaultInfo
    ProjectInfo projectInfo
    Map<String, ModuleInfo> moduleInfoMap

    Map<String, String> substituteMap

}