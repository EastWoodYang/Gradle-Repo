package com.eastwood.tools.plugins.repo.model

class Configurations {

    List<Exclude> excludeList
    List<Force> forceList

    Map<String, Configuration> configurationMap

    Map<String, String> substituteMap
}