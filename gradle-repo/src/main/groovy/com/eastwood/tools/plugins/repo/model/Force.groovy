package com.eastwood.tools.plugins.repo.model

class Force {

    String name
    String group
    String version

    @Override
    String toString() {
        return group + ':' + name + ':' + version
    }
}