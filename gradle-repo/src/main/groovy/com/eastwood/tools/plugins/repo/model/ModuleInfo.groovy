package com.eastwood.tools.plugins.repo.model

class ModuleInfo {
    String name
    String local

    RemoteInfo remoteInfo

    String branch

    boolean hide

    String substitute

    Map<String, List<Dependency>> dependencyMap

}