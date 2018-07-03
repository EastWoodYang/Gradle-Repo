package com.eastwood.tools.plugins.repo.model

class ModuleInfo {
    String name
    String local
    RepositoryInfo repositoryInfo
    Map<String, List<String>> dependencyMap
}