package com.eastwood.tools.plugins.repo.model

class ModuleInfo {
    String name
    String local
    RepositoryInfo repositoryInfo
    String substitute

    Map<String, List<Dependency>> dependencyMap

    boolean fromLocal

}