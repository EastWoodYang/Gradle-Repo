package com.eastwood.tools.plugins.repo.utils

import com.eastwood.tools.plugins.repo.model.*
import org.gradle.api.GradleException
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class RepoUtils {

    static RepoInfo getRepoInfo(File projectDir, boolean withDependencies) {
        File repoFile = new File(projectDir, 'repo.xml')
        if (!repoFile.exists()) {
            throw new GradleException("[repo] - repo.xml not found under " + projectDir.absolutePath)
        }

        RepoInfo repoInfo = parseRepo(repoFile, withDependencies)

        File repoLocalFile = new File(projectDir, 'repo-local.xml')
        if (repoLocalFile.exists()) {
            parseRepoLocal(repoInfo, repoLocalFile, withDependencies)
        }
        return repoInfo
    }

    private static RepoInfo parseRepo(File repoFile, boolean withDependencies) {
        RepoInfo repoInfo = new RepoInfo()
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = factory.newDocumentBuilder()
        FileInputStream inputStream = new FileInputStream(repoFile)
        Document doc = builder.parse(inputStream)
        Element rootElement = doc.getDocumentElement()

        RemoteInfo defaultInfo = null
        NodeList defaultNodeList = rootElement.getElementsByTagName("default")
        if (defaultNodeList.getLength() > 1) {
            throw new RuntimeException("[repo] - Make sure there is only one '<default />' element in repo.xml")
        } else if (defaultNodeList.getLength() == 1) {
            defaultInfo = new RemoteInfo()
            Element defaultElement = (Element) defaultNodeList.item(0)
            defaultInfo.fetchUrl = defaultElement.getAttribute('fetch')
            defaultInfo.pushUrl = defaultElement.getAttribute('push')
            if (defaultInfo.pushUrl.trim().isEmpty()) {
                defaultInfo.pushUrl = defaultInfo.fetchUrl
            }
        }

        repoInfo.projectInfo = new ProjectInfo()
        repoInfo.projectInfo.includeModuleList = new ArrayList<>()
        // project remoteInfo info
        NodeList projectNodeList = rootElement.getElementsByTagName("project")
        if (projectNodeList.getLength() > 1) {
            throw new RuntimeException("[repo] - Make sure there is only one '<project />' element in repo.xml")
        } else if (projectNodeList.getLength() == 1) {
            Element projectElement = (Element) projectNodeList.item(0)
            repoInfo.projectInfo.remoteInfo = getProjectRemoteInfo(defaultInfo, projectElement)
            // project include module
            NodeList includeModuleNodeList = projectElement.getElementsByTagName("include")
            for (int i = 0; i < includeModuleNodeList.getLength(); i++) {
                Element includeModuleElement = (Element) includeModuleNodeList.item(i)
                String moduleName = includeModuleElement.getAttribute("name")
                repoInfo.projectInfo.includeModuleList.add(moduleName.trim())
            }
        }

        if (defaultInfo == null) {
            defaultInfo = new RemoteInfo()
            RemoteInfo projectRemoteInfo = repoInfo.projectInfo.remoteInfo
            if (projectRemoteInfo != null) {
                String fetchUrl = projectRemoteInfo.fetchUrl
                if (fetchUrl.startsWith("git@")) {
                    String[] temp = fetchUrl.split(":")
                    defaultInfo.fetchUrl = temp[0] + ':' + temp[1].substring(0, temp[1].lastIndexOf('/'))
                } else {
                    URI uri = new URI(fetchUrl)
                    String path = uri.getPath();
                    String parent = path.substring(0, path.lastIndexOf('/'))
                    defaultInfo.fetchUrl = fetchUrl.replace(uri.getPath(), "") + parent
                }
                defaultInfo.pushUrl = defaultInfo.fetchUrl
            }
        }
        repoInfo.defaultInfo = defaultInfo

        NodeList moduleNodeList = rootElement.getElementsByTagName("module")
        repoInfo.moduleInfoMap = new HashMap<>()
        for (int i = 0; i < moduleNodeList.getLength(); i++) {
            Element moduleElement = (Element) moduleNodeList.item(i)
            ModuleInfo moduleInfo = getModuleInfo(defaultInfo, moduleElement)
            if (withDependencies) {
                moduleInfo.dependencyMap = getModuleDependenciesInfo(moduleElement)
                if (!moduleInfo.substitute.trim().isEmpty()) {
                    if (repoInfo.substituteMap == null) {
                        repoInfo.substituteMap = new HashMap<>()
                    }
                    repoInfo.substituteMap.put(moduleInfo.name, moduleInfo.substitute)
                }
            }
            repoInfo.moduleInfoMap.put(moduleInfo.name, moduleInfo)
        }

        return repoInfo
    }

    private static RepoInfo parseRepoLocal(RepoInfo repoInfo, File repoLocalFile, boolean withDependencies) {
        repoInfo.moduleInfoMap.each {
            ModuleInfo moduleInfo = it.value
            if (repoInfo.projectInfo.includeModuleList.contains(moduleInfo.name)) {
                return
            }
            moduleInfo.hide = true
        }


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = factory.newDocumentBuilder()
        FileInputStream inputStream = new FileInputStream(repoLocalFile)
        Document doc = builder.parse(inputStream)
        Element rootElement = doc.getDocumentElement()

        NodeList moduleNodeList = rootElement.getElementsByTagName("module")
        for (int i = 0; i < moduleNodeList.getLength(); i++) {
            Element moduleElement = (Element) moduleNodeList.item(i)

            String name = moduleElement.getAttribute('name')
            if (repoInfo.moduleInfoMap.containsKey(name)) {
                ModuleInfo moduleInfo = repoInfo.moduleInfoMap.get(name)
                moduleInfo.hide = false
                if (withDependencies) {
                    if (moduleInfo.dependencyMap == null) {
                        moduleInfo.dependencyMap = new HashMap<>()
                    }
                    Map<String, List<Dependency>> dependenciesMap = getModuleDependenciesInfo(moduleElement)
                    dependenciesMap.each {
                        List<Dependency> dependencies = moduleInfo.dependencyMap.get(it.key)
                        if (dependencies == null) {
                            dependencies = new ArrayList<>()
                            moduleInfo.dependencyMap.put(it.key, dependencies)
                        }
                        dependencies.addAll(it.value)
                    }
                }
            } else {
                ModuleInfo moduleInfo = getModuleInfo(repoInfo.defaultInfo, moduleElement)
                moduleInfo.fromLocal = true
                if (withDependencies) {
                    moduleInfo.dependencyMap = getModuleDependenciesInfo(moduleElement)
                    if (!moduleInfo.substitute.trim().isEmpty()) {
                        if (repoInfo.substituteMap == null) {
                            repoInfo.substituteMap = new HashMap<>()
                        }
                        repoInfo.substituteMap.put(moduleInfo.name, moduleInfo.substitute)
                    }
                }
                repoInfo.moduleInfoMap.put(moduleInfo.name, moduleInfo)
            }
        }
    }

    static RemoteInfo getProjectRemoteInfo(RemoteInfo defaultInfo, Element element) {
        String origin = element.getAttribute('origin')
        if (origin.trim().isEmpty()) {
            return null
        }

        RemoteInfo remoteInfo
        if (origin.startsWith('http') || origin.startsWith('git@')) {
            remoteInfo = filterOrigin(origin)
        } else {
            if (defaultInfo != null && defaultInfo.fetchUrl != null) {
                remoteInfo = filterOrigin(defaultInfo, origin)
            } else {
                throw new RuntimeException("[repo] - The 'origin' attribute value of the '<project />' element is invalid.")
            }
        }

        return remoteInfo
    }

    static ModuleInfo getModuleInfo(RemoteInfo defaultInfo, Element element) {
        ModuleInfo moduleInfo = new ModuleInfo()
        String name = element.getAttribute("name")
        if (name.trim().isEmpty()) {
            throw new RuntimeException("[repo] - The 'name' attribute value of the '<module />' element is not configured.")
        }
        moduleInfo.name = name

        String local = element.getAttribute("local")
        if (local.trim().isEmpty()) {
            local = "./"
        }
        moduleInfo.local = local

        String branch = element.getAttribute("branch")
        if (branch.trim().isEmpty()) {
            branch = null
        }
        moduleInfo.branch = branch

        moduleInfo.substitute = element.getAttribute('substitute')

        String origin = element.getAttribute("origin")
        if (!origin.trim().isEmpty()) {
            RemoteInfo remoteInfo
            if (origin.startsWith("http") || origin.startsWith("git@")) {
                remoteInfo = filterOrigin(origin)
            } else {
                if (defaultInfo != null && defaultInfo.fetchUrl != null) {
                    remoteInfo = filterOrigin(defaultInfo, origin)
                } else {
                    throw new RuntimeException("[repo] - The 'origin' attribute value of the '<module />' element is invalid.")
                }
            }
            moduleInfo.remoteInfo = remoteInfo
        }

        return moduleInfo
    }

    static Map<String, List<Dependency>> getModuleDependenciesInfo(Element element) {
        Map<String, List<Dependency>> dependenciesMap = new HashMap<>()
        NodeList dependenciesNodeList = element.getElementsByTagName("dependencies")
        for (int i = 0; i < dependenciesNodeList.getLength(); i++) {
            Element dependenciesElement = (Element) dependenciesNodeList.item(i)
            NodeList dependencyNodeList = dependenciesElement.getChildNodes()
            for (int j = 0; j < dependencyNodeList.length; j++) {
                Node childNode = dependencyNodeList.item(j)
                if (!(childNode instanceof Element)) {
                    continue
                }

                Element dependencyElement = (Element) childNode
                String configurationName = dependencyElement.getNodeName().trim()
                if (configurationName.isEmpty()) {
                    continue
                }

                Dependency dependency = new Dependency()
                dependency.name = dependencyElement.getAttribute("name")
                List<Dependency> dependencies = dependenciesMap.get(configurationName)
                if (dependencies == null) {
                    dependencies = new ArrayList<>()
                    dependenciesMap.put(configurationName, dependencies)
                }
                dependencies.add(dependency)
            }
        }
        return dependenciesMap
    }

    static RemoteInfo filterOrigin(RemoteInfo defaultInfo, String origin) {
        RemoteInfo remoteInfo = new RemoteInfo()
        if (defaultInfo.fetchUrl == defaultInfo.pushUrl) {
            String fetchUrl = defaultInfo.fetchUrl + '/./' + origin
            if (fetchUrl.startsWith("git@")) {
                String[] temp = fetchUrl.split(":")
                remoteInfo.fetchUrl = temp[0] + ':' + PathUtils.normalize(temp[1], true)
                remoteInfo.pushUrl = remoteInfo.fetchUrl
            } else {
                URI uri = new URI(fetchUrl)
                remoteInfo.fetchUrl = fetchUrl.replace(uri.getPath(), "") + PathUtils.normalize(uri.getPath(), true)
                remoteInfo.pushUrl = remoteInfo.fetchUrl
            }
        } else {
            String fetchUrl = defaultInfo.fetchUrl + '/./' + origin
            if (fetchUrl.startsWith("git@")) {
                String[] temp = fetchUrl.split(":")
                remoteInfo.fetchUrl = temp[0] + ':' + PathUtils.normalize(temp[1], true)
            } else {
                URI uri = new URI(fetchUrl)
                remoteInfo.fetchUrl = fetchUrl.replace(uri.getPath(), "") + PathUtils.normalize(uri.getPath(), true)
            }

            String pushUrl = defaultInfo.pushUrl + '/./' + origin
            if (pushUrl.startsWith("git@")) {
                String[] temp = pushUrl.split(":")
                remoteInfo.pushUrl = temp[0] + ':' + PathUtils.normalize(temp[1], true)
            } else {
                URI uri = new URI(pushUrl)
                remoteInfo.pushUrl = pushUrl.replace(uri.getPath(), "") + PathUtils.normalize(uri.getPath(), true)
            }
        }

        if (!remoteInfo.fetchUrl.endsWith('.git')) {
            remoteInfo.fetchUrl += '.git'
        }

        if (!remoteInfo.pushUrl.endsWith('.git')) {
            remoteInfo.pushUrl += '.git'
        }

        return remoteInfo
    }

    static RemoteInfo filterOrigin(String origin) {
        RemoteInfo remoteInfo = new RemoteInfo()
        if (origin.startsWith("git@")) {
            String[] temp = origin.split(":")
            remoteInfo.fetchUrl = temp[0] + ':' + PathUtils.normalize(temp[1], true)
            remoteInfo.pushUrl = remoteInfo.fetchUrl
        } else {
            URI uri = new URI(origin)
            remoteInfo.fetchUrl = origin.replace(uri.getPath(), "") + PathUtils.normalize(uri.getPath(), true)
            remoteInfo.pushUrl = remoteInfo.fetchUrl
        }
        return remoteInfo
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

    static RepoInfo getLastRepoManifest(File rootProjectDir) {
        RepoInfo repoInfo = new RepoInfo()
        repoInfo.moduleInfoMap = new HashMap<>()
        File lastRepoManifest = new File(rootProjectDir, '.repo/lastRepoManifest.xml')
        if (!lastRepoManifest.exists()) {
            return repoInfo
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = factory.newDocumentBuilder()
        FileInputStream inputStream = new FileInputStream(lastRepoManifest)
        Document doc = builder.parse(inputStream)
        Element rootElement = doc.getDocumentElement()

        ProjectInfo projectInfo = new ProjectInfo()
        // project remoteInfo info
        NodeList projectNodeList = rootElement.getElementsByTagName("project")
        Element projectElement = (Element) projectNodeList.item(0)

        if (!projectElement.getAttribute('fetch').trim().isEmpty()) {
            RemoteInfo remoteInfo = new RemoteInfo()
            remoteInfo.fetchUrl = projectElement.getAttribute('fetch')
            remoteInfo.pushUrl = projectElement.getAttribute('push')
            if (remoteInfo.pushUrl.trim().isEmpty()) {
                remoteInfo.pushUrl = remoteInfo.fetchUrl
            }
            projectInfo.remoteInfo = remoteInfo
        }
        projectInfo.branch = projectElement.getAttribute('branch')

        // project include module
        projectInfo.includeModuleList = new ArrayList<>()
        NodeList includeModuleNodeList = projectElement.getElementsByTagName("include")
        for (int i = 0; i < includeModuleNodeList.getLength(); i++) {
            Element includeModuleElement = (Element) includeModuleNodeList.item(i)
            String moduleName = includeModuleElement.getAttribute("name")
            projectInfo.includeModuleList.add(moduleName.trim())
        }
        repoInfo.projectInfo = projectInfo

        NodeList moduleNodeList = rootElement.getElementsByTagName("module")
        for (int i = 0; i < moduleNodeList.getLength(); i++) {
            Element moduleElement = (Element) moduleNodeList.item(i)

            ModuleInfo moduleInfo = new ModuleInfo()
            moduleInfo.name = moduleElement.getAttribute('name')
            moduleInfo.local = moduleElement.getAttribute('local')
            moduleInfo.fromLocal = moduleElement.getAttribute('fromLocal')

            if (!moduleElement.getAttribute('fetch').trim().isEmpty()) {
                RemoteInfo remoteInfo = new RemoteInfo()
                remoteInfo.fetchUrl = moduleElement.getAttribute('fetch')
                remoteInfo.pushUrl = moduleElement.getAttribute('push')
                if (remoteInfo.pushUrl.trim().isEmpty()) {
                    remoteInfo.pushUrl = remoteInfo.fetchUrl
                }
                moduleInfo.remoteInfo = remoteInfo
            }
            moduleInfo.branch = moduleElement.getAttribute('branch')

            repoInfo.moduleInfoMap.put(moduleInfo.name, moduleInfo)
        }

        return repoInfo
    }

    static void saveRepoManifest(File rootProjectDir, RepoInfo repoInfo) {
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance()
        Document document = builderFactory.newDocumentBuilder().newDocument()
        Element manifestElement = document.createElement("manifest")

        Element projectElement = document.createElement("project")
        manifestElement.appendChild(projectElement)

        if (repoInfo.projectInfo.remoteInfo != null) {
            projectElement.setAttribute("fetchUrl", repoInfo.projectInfo.remoteInfo.fetchUrl)
            if (repoInfo.projectInfo.remoteInfo.fetchUrl != repoInfo.projectInfo.remoteInfo.pushUrl) {
                projectElement.setAttribute("pushUrl", repoInfo.projectInfo.remoteInfo.pushUrl)
            }
        }
        projectElement.setAttribute('branch', repoInfo.projectInfo.branch)

        if (repoInfo.projectInfo.includeModuleList != null) {
            repoInfo.projectInfo.includeModuleList.each {
                Element includeElement = document.createElement("include")
                includeElement.setAttribute("name", it)
                projectElement.appendChild(includeElement)
            }
        }

        repoInfo.moduleInfoMap.each {
            Element moduleElement = document.createElement("module")
            manifestElement.appendChild(moduleElement)

            ModuleInfo moduleInfo = it.value
            moduleElement.setAttribute('name', moduleInfo.name)
            if (moduleInfo.local != './') {
                moduleElement.setAttribute('local', moduleInfo.local)
            }
            if (moduleInfo.remoteInfo != null && !repoInfo.projectInfo.includeModuleList.contains(moduleInfo.name)) {
                moduleElement.setAttribute("fetch", moduleInfo.remoteInfo.fetchUrl)
                if (moduleInfo.remoteInfo.fetchUrl != moduleInfo.remoteInfo.pushUrl) {
                    moduleElement.setAttribute("pushUrl", moduleInfo.remoteInfo.pushUrl)
                }
            }
            moduleElement.setAttribute('branch', moduleInfo.branch)

            if (moduleInfo.fromLocal) {
                moduleElement.setAttribute('fromLocal', 'true')
            }
        }

        File repoDir = new File(rootProjectDir, '.repo')
        if (!repoDir.exists()) {
            repoDir.mkdirs()
        }
        File lastRepoManifest = new File(repoDir, 'lastRepoManifest.xml')

        // save
        Transformer transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty(OutputKeys.CDATA_SECTION_ELEMENTS, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        transformer.transform(new DOMSource(manifestElement), new StreamResult(lastRepoManifest))
    }

    static void addNewModuleElement(File projectDir, String moduleName) {
        File repoFile = new File(projectDir, 'repo-local.xml')
        if (!repoFile.exists()) {
            repoFile = new File(projectDir, 'repo.xml')
            if (!repoFile.exists()) {
                return
            }
        }

        def content = ''
        repoFile.readLines('utf-8').each {
            def line = it.trim()
            if (line == '</manifest>') {
                content += "    <module name=\"$moduleName\" />\n"
            }
            content += it + '\n'
        }
        repoFile.setText(content, 'utf-8')
    }
}