package com.eastwood.tools.plugins.repo

class GitUtil {

    static void clone(File dir, String url, String branchName) {
        def process = ("git clone -b $branchName $url -l $dir.name").execute(null, dir.parentFile)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git clone -b $branchName $url -l $dir.name] under ${dir.absolutePath}\n message: ${process.err.text}")
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
        if (url == null) {
            throw new RuntimeException("[repo] - failure to get origin remote fetch fetchUrl.")
        }
        return url
    }

    static String getBranchName(File dir) {
        def process = ("git branch").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git branch] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
        String branchName = null
        process.text.readLines().each {
            if (it.startsWith('*')) {
                branchName = it.replace('*', '').trim()
            }
        }

        if (branchName == null) {
            throw new RuntimeException("[repo] - failure to get git branch name under ${dir.absolutePath}")
        }
        return branchName
    }

    static boolean isBranchChanged(File dir, String branch) {
        def currentBranchName = getBranchName(dir)
        return currentBranchName != branch
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
        return new File(dir, ".git/refs/remotes/origin/$branchName").exists()
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

    static void commitRepoFile(File dir) {
        def process = ("git add repo.xml").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git add repo.xml] under ${dir.absolutePath}\n message: ${process.err.text}")
        }

        commit(dir, "keep repo.xml")
    }

    static void revertRepoFile(File dir) {
        def process = ("git checkout repo.xml").execute(null, dir)
        def result = process.waitFor()
        if (result != 0) {
            throw new RuntimeException("[repo] - failure to execute git command [git checkout repo.xml] under ${dir.absolutePath}\n message: ${process.err.text}")
        }
    }

}