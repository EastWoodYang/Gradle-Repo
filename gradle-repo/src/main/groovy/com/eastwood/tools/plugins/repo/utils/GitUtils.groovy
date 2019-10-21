package com.eastwood.tools.plugins.repo.utils

import com.eastwood.tools.plugins.repo.model.RepoInfo

class GitUtils {

    static void init(File dir) {
        def process = ("git init").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git init] under ${dir.absolutePath}\nmessage: ${process.err.text}")
        }
    }

    static void addRemote(File dir, String url) {
        def process = ("git remote add origin $url").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git remote add origin $url] under ${dir.absolutePath}\nmessage: ${process.err.text}")
        }
    }

    static void removeRemote(File dir) {
        def fetchUrl = getOriginRemoteFetchUrl(dir)
        if (fetchUrl == null) return

        def process = ("git remote remove origin").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git remote remove origin] under ${dir.absolutePath}\nmessage: ${process.err.text}")
        }
    }

    static void clone(File dir, String url, String branchName) {
        def process = ("git clone --branch $branchName $url -l $dir.name").execute(null, dir.parentFile)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git clone --branch $branchName $url -l $dir.name] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
    }

    static void commit(File dir, String message) {
        def process = ("git commit -m \"$message\"").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git commit -m \"$message\"] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
    }

    static boolean isGitDir(File dir) {
        return new File(dir, ".git").exists()
    }

    static void clearGitDir(File dir) {
        File git = new File(dir, ".git")
        if (git.exists()) {
            git.deleteDir()
        }
    }

    static void removeCachedDir(File dir, String moduleDir) {
        def process = ("git rm -r --cached $moduleDir").execute(null, dir)
        process.waitFor()
    }

    static String getOriginRemoteFetchUrl(File dir) {
        def process = ("git remote -v").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git remote -v] under ${dir.absolutePath}\n message: ${process.err.text}")
        }

        def url = null
        process.getText().readLines().each {
            if (it.startsWith('origin') && it.endsWith('(fetch)')) {
                url = it.replace('origin', '').replace('(fetch)', '').trim()
            }
        }
        return url
    }

    static String getOriginRemotePushUrl(File dir) {
        def process = ("git remote -v").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git remote -v] under ${dir.absolutePath}\n message: ${process.err.text}")
        }

        def url = null
        process.getText().readLines().each {
            if (it.startsWith('origin') && it.endsWith('(push)')) {
                url = it.replace('origin', '').replace('(push)', '').trim()
            }
        }
        return url
    }

    static String setOriginRemoteUrl(File dir, String url) {
        def process = ("git remote set-url origin $url").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git remote set-url origin $url] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
        return process.text.trim() == ""
    }

    static String setOriginRemotePushUrl(File dir, String url) {
        def process = ("git remote set-url --push origin $url").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git remote set-url --push origin $url] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
        return process.text.trim() == ""
    }

    static String getBranchName(File dir) {
        def process = ("git branch").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git branch] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
        List<String> lines = process.text.readLines()
        String branchName = null
        if (lines.isEmpty()) {
            branchName = 'master'
        } else {
            lines.each {
                if (it.startsWith('*')) {
                    branchName = it.replace('*', '').trim()
                }
            }
        }
        if (branchName == null) {
            throw new RuntimeException("[repo] - failure to get git branch name under ${dir.absolutePath}")
        }
        return branchName
    }

    static boolean isClean(File dir) {
        def process = ("git status -s").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git status -s] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
        return process.text.trim() == ""
    }

    static boolean isLocalBranch(File dir, String branchName) {
        return new File(dir, ".git/refs/heads/$branchName").exists()
    }

    static boolean isRemoteBranch(File dir, String branchName) {
        def process = ("git fetch").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git fetch] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
        if (branchName == "master") {
            branchName = "HEAD"
        }

        process = ("git branch -r").execute(null, dir)
        result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git branch -r] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
        return process.text.contains("origin/$branchName")
    }

    static void checkoutBranch(File dir, String branchName) {
        def process = ("git checkout $branchName").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git checkout $branchName] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
    }

    static void checkoutRemoteBranch(File dir, String branchName) {
        def process = ("git checkout -b $branchName origin/$branchName").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git checkout -b $branchName origin/$branchName] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
    }

    static void checkoutNewBranch(File dir, String branchName) {
        def process = ("git checkout -b $branchName").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git checkout -b $branchName] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
    }

    static void updateExclude(File projectDir, RepoInfo repoInfo) {
        List<String> ignoreModules = new ArrayList<>()
        List<String> includeModules = new ArrayList<>()

        ignoreModules.add('.repo')
        ignoreModules.add('.idea/')
        ignoreModules.add('.iml')
        ignoreModules.add('*.iml')

        repoInfo.moduleInfoMap.each {
            def moduleDir = RepoUtils.getModuleDir(projectDir, it.value)
            def moduleName = RepoUtils.getModuleName(projectDir, moduleDir)
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
        if (excludeFile.exists()) {
            excludeFile.eachLine {
                def item = it.trim()
                if (includeModules.contains(item)) {
                    return
                }
                ignoreModules.remove(item)
                exclude += item + "\n"
            }
        }
        ignoreModules.each {
            exclude += it + "\n"
        }
        excludeFile.write(exclude)
    }

    static void addExclude(File dir) {
        List<String> ignoreList = new ArrayList<>()
        ignoreList.add('build/')
        ignoreList.add('.iml')
        ignoreList.add('*.iml')

        File excludeFile = new File(dir, '.git/info/exclude')
        String exclude = ""
        if (excludeFile.exists()) {
            excludeFile.eachLine {
                def item = it.trim()
                ignoreList.remove(item)
                exclude += item + "\n"
            }
        }
        ignoreList.each {
            exclude += it + "\n"
        }
        excludeFile.write(exclude)
    }

}