package com.eastwood.tools.plugins.repo

import com.eastwood.tools.plugins.repo.model.*
import org.apache.commons.io.FilenameUtils
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

class RepoUtil {

    public static File rootProjectDir

    static RepoInfo getRepoInfo(File repo, boolean dependency) {
        rootProjectDir = repo.getParentFile()
        RepoInfo repoInfo = new RepoInfo()

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = factory.newDocumentBuilder()
        FileInputStream inputStream = new FileInputStream(repo)
        Document doc = builder.parse(inputStream)
        Element rootElement = doc.getDocumentElement()

        NodeList projectNodeList = rootElement.getElementsByTagName("project")
        if (projectNodeList.getLength() > 1) {
            throw new IllegalArgumentException("[repo] - there are multiple project element.")
        }

        if (projectNodeList.getLength() == 1) {
            Element projectElement = (Element) projectNodeList.item(0)
            ProjectInfo projectInfo = new ProjectInfo()
            projectInfo.repositoryInfo = getProjectRepositoryInfo(projectElement)

            // include module
            projectInfo.includeModuleList = new ArrayList<>()
            NodeList includeModuleNodeList = projectElement.getElementsByTagName("include")
            for (int i = 0; i < includeModuleNodeList.getLength(); i++) {
                Element includeModuleElement = (Element) includeModuleNodeList.item(i)
                String moduleName = includeModuleElement.getAttribute("name")
                projectInfo.includeModuleList.add(moduleName.trim())
            }
            repoInfo.projectInfo = projectInfo
        }

        NodeList moduleNodeList = rootElement.getElementsByTagName("module")
        repoInfo.moduleInfoMap = new HashMap<>()
        for (int i = 0; i < moduleNodeList.getLength(); i++) {
            Element moduleElement = (Element) moduleNodeList.item(i)
            ModuleInfo moduleInfo = getModuleRepositoryInfo(repoInfo.projectInfo == null ? null : repoInfo.projectInfo.repositoryInfo, moduleElement)

            if (dependency) {
                Map<String, List<String>> dependencyMap = new HashMap<>()

                NodeList dependenciesNodeList = moduleElement.getElementsByTagName("dependencies")
                for (int j = 0; j < dependenciesNodeList.getLength(); j++) {
                    Element dependencyElement = (Element) dependenciesNodeList.item(j)
                    NodeList childNodeList = dependencyElement.getChildNodes()
                    for (int k = 0; k < childNodeList.length; k++) {
                        Node childNode = childNodeList.item(k)
                        if (childNode instanceof Element) {
                            Element childElement = (Element) childNode
                            String configureName = childElement.getNodeName()
                            if (configureName != null && configureName.trim() != "") {
                                String moduleName = childElement.getAttribute("name")
                                if (moduleName != null && moduleName.trim() != "") {
                                    List<String> dependencies = dependencyMap.get(configureName)
                                    if (dependencies == null) {
                                        dependencies = new ArrayList<>()
                                    }
                                    dependencies.add(moduleName)
                                    dependencyMap.put(configureName, dependencies)
                                }
                            }
                        }
                    }
                }
                moduleInfo.dependencyMap = dependencyMap
            }
            repoInfo.moduleInfoMap.put(moduleInfo.name, moduleInfo)
        }
        return repoInfo
    }

    static RepositoryInfo getProjectRepositoryInfo(Element element) {
        String origin = element.getAttribute("origin")
        if (origin == null || origin.trim() == "") {
            return null
        }

        RepositoryInfo repositoryInfo = new RepositoryInfo()
        if (!(origin.startsWith("http") || origin.startsWith("git@")) || !origin.endsWith(".git")) {
            throw new IllegalArgumentException("[repo] - <project /> element [origin] is not valid.")
        }
        repositoryInfo.originInfo = filterOrigin(origin)
        if (repositoryInfo.originInfo == null) {
            throw new IllegalArgumentException("[repo] - <project /> element [origin] is not valid.")
        }
        String branch = element.getAttribute("branch")
        if (branch == null || branch.trim() == "") {
            branch = "master"
        }
        repositoryInfo.branch = branch
        return repositoryInfo
    }

    static ModuleInfo getModuleRepositoryInfo(RepositoryInfo project, Element element) {
        ModuleInfo moduleInfo = new ModuleInfo()
        String name = element.getAttribute("name")
        if (name == null || name.trim() == "") {
            throw new IllegalArgumentException("[repo] - <module /> element [name] is not set.")
        }
        moduleInfo.name = name

        String local = element.getAttribute("local")
        if (local == null || local.trim() == "") {
            local = "./"
        }
        moduleInfo.local = local

        String origin = element.getAttribute("origin")
        if (origin == null || origin.trim() == "") {
            return moduleInfo
        }

        RepositoryInfo repositoryInfo = new RepositoryInfo()
        OriginInfo originInfo
        if ((origin.startsWith("http") || origin.startsWith("git@")) && origin.endsWith(".git")) {
            originInfo = filterOrigin(origin)
            if (originInfo == null) {
                throw new IllegalArgumentException("[repo] - <module name=\"$name\"/> element [origin] is not valid.")
            }
        } else {
            if (!origin.startsWith(".")) {
                throw new IllegalArgumentException("[repo] - if <module /> element [origin] is relative path, must start with './' or '../'.")
            }
            if (project == null) {
                throw new IllegalArgumentException("[repo] - if <module /> element [origin] is relative path, must set <project /> element [origin].")
            }
            originInfo = filterRelativeOrigin(project.originInfo, origin)
        }
        repositoryInfo.originInfo = originInfo

        String branch = element.getAttribute("branch")
        if (branch == null || branch.trim() == "") {
            if (project == null) {
                branch = "master"
            } else {
                branch = project.branch
            }
        }
        repositoryInfo.branch = branch
        moduleInfo.repositoryInfo = repositoryInfo
        return moduleInfo
    }

    static OriginInfo filterOrigin(String origin) {
        if (origin == null || origin.trim() == "") return null

        OriginInfo uriInfo = new OriginInfo()
        if (origin.startsWith("git@")) {
            uriInfo.isSSH = true
            String[] temp = origin.split(":")
            uriInfo.url = temp[0]
            uriInfo.ssh_url_to_repo = FilenameUtils.normalize(temp[1], true)
            uriInfo.path = uriInfo.ssh_url_to_repo
        } else {
            URI uri = new URI(origin)
            uriInfo.path = uri.getPath()
            uriInfo.url = origin.replace(uriInfo.path, "")
            uriInfo.http_url_to_repo = FilenameUtils.normalize(uriInfo.path, true)
            uriInfo.path = uriInfo.http_url_to_repo
        }
        if (uriInfo.path == null) {
            return null
        }
        return uriInfo
    }

    static OriginInfo filterRelativeOrigin(OriginInfo rootUriInfo, String origin) {
        if (origin == null || origin.trim() == "") return null
        String parentPath = new File(rootUriInfo.path).getParent()
        parentPath = FilenameUtils.normalize(parentPath, true)
        if (parentPath == null) {
            parentPath = "/"
        }
        String path
        if (parentPath == "/") {
            path = FilenameUtils.normalize(origin, true)
        } else {
            path = FilenameUtils.normalize(parentPath + "/" + origin, true)
        }
        OriginInfo uriInfo = new OriginInfo()
        uriInfo.isSSH = rootUriInfo.isSSH
        uriInfo.url = rootUriInfo.url
        if (uriInfo.isSSH) {
            if (path.startsWith("/")) {
                path = path.replaceFirst("/", "")
            }
        } else {
            if (!path.startsWith("/")) {
                path = "/" + path
            }
        }
        uriInfo.path = path
        return uriInfo
    }

    static File getModuleDir(File projectDir, ModuleInfo moduleInfo) {
        def moduleParentDir = new File(projectDir, moduleInfo.local)
        return new File(moduleParentDir, moduleInfo.name)
    }

    static String getModuleName(File projectDir, File moduleDir) {
        String rootPath = projectDir.absolutePath
        String modulePath = moduleDir.absolutePath
        if (modulePath.contains(rootPath)) {
            return modulePath
                    .replace(rootPath, "")
                    .replace("\\", "/")
                    .replace("./", "")
                    .replace("/", ":")
        }
        return ""
    }

    static updateExclude(File projectDir, RepoInfo repoInfo) {
        List<String> ignoreModules = new ArrayList<>()
        List<String> includeModules = new ArrayList<>()

        repoInfo.moduleInfoMap.each {
            def moduleDir = getModuleDir(projectDir, it.value)
            def moduleName = getModuleName(projectDir, moduleDir)
            String ignoreModule = moduleName.replace(":", "/") + "/"
            if (ignoreModule.startsWith("/")) {
                ignoreModule = ignoreModule.substring(1)
            }

            if (repoInfo.projectInfo.includeModuleList.contains(it.key)) {
                includeModules.add(ignoreModule)
            } else {
                ignoreModules.add(ignoreModule)
            }
        }

        File excludeFile = new File(projectDir, '.git/info/exclude')
        String exclude = ""
        excludeFile.eachLine {
            def item = it.trim()
            if (includeModules.contains(item)) {
                return
            }
            ignoreModules.remove(item)
            exclude += item + "\n"
        }
        ignoreModules.each {
            exclude += it + "\n"
        }
        excludeFile.write(exclude)
    }

}