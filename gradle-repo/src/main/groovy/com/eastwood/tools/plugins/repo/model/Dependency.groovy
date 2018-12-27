package com.eastwood.tools.plugins.repo.model

class Dependency {

    String name
    String group
    String version

    boolean transitive = true
    List<Exclude> excludes

}