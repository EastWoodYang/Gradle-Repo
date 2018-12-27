package com.eastwood.tools.plugins.repo.model

import org.gradle.api.artifacts.ExcludeRule

class Exclude implements ExcludeRule {

    String group
    String name

    @Override
    String getModule() {
        return name
    }

    @Override
    String getGroup() {
        return group
    }
}