package com.eastwood.tools.plugins.repo

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

class GradleRepoPlugin implements Plugin<Object> {

    void apply(Object object) {
        Plugin plugin = null
        if (object instanceof Project) {
            plugin = new RepoBuildPlugin()
        } else if (object instanceof Settings) {
            plugin = new RepoSettingsPlugin()
        }

        if (plugin != null) {
            plugin.apply(object)
        }
    }

}