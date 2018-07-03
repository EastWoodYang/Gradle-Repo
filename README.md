# Gradle Repo

## 打开Repo工程
凡支持Repo的工程，clone主工程之后，直接用Android Studio打开即可。


## 创建Repo工程 
1. 先在root project build.gradle中添加依赖。
    > classpath 'com.winwin.common:gradle-repo-plugin:1.0.0-SNAPSHOT'
    
2. 并添加插件**gradle-repo-plugin**。
    > apply plugin: 'gradle-repo-build'

3. **Sync**之后，执行**buildRepoProject**。
    > 打开Gradle Tasks View，在root中，双击 Tasks -> repo -> buildRepoProject

4. **buildRepoProject**执行结束后，在项目的根目录会生成一个**repo.xml**文件。


## 新建模块并添加到Repo
1. 通过Android Studio新建一个Android Module。

2. 执行**syncRepo**。
   > 打开Gradle Tasks View，在root中，双击 Tasks -> repo -> syncRepo
   
3. **syncRepo**执行结束后，在**repo.xml**中会新增一个\<module />。

## 绑定远程仓库
还未绑定远程仓库的模块，在Gradle Tasks View中的Tasks -> repo下都要对应的绑定远程仓库任务。

> **root**工程对应的任务名称为**bindRemoteProject**，其他模块格式为**bindRemoteProject_[模块名称]**

执行绑定远程仓库任务前，需设置GitLab的url和privateToken，以及模块对应的origin地址。
##### GitLab的url和privateToken
在root project build.gradle中添加
```
repo {
    gitLab {
        url 'http://git.yingyinglicai.net'
        privateToken 'fvjcBCz7*****mU4samJ'
    }
}
```

## 切换工程分支
两种方式：
1. 修改repo.xml
    
    修改\<project />的branch值，执行执行**syncRepo**即可。
    * 若修改后的branch，存在本地分支，则直接切换。
    * 若修改后的branch，不存在本地分支，但远程仓库存在，则拉取远程仓库中的分支到本地后再切换。
    * 若修改后的branch，不存在本地分支，远程仓库也不存在，则新建一个本地分支。
    
    **如果其他模块的分支是跟随root project分支，则相应也会做上述同步操作。**
    
2. 通过git切换
    
    此方式只能在root project的已有分支上进行切换，然后执行执行**syncRepo**即可。
    
    **其他模块的分支会根据切换后的root project分支中的repo.xml进行需要变动。**

## 切换模块分支
修改\<module />的branch值，执行执行**syncRepo**即可。

## repo 文件格式
```
<?xml version='1.0' encoding='UTF-8'?>
<manifest>
 
    <project
        origin="git@git.yingyinglicai.net:mobile/android/common/simple.git"
        branch="master" >
 
        <include module="app" />
 
    </project>
 
    <module name="app" >

        <compile name="module"/>

    </module>
    
    <module name="module"
        local="common"
        origin="git@git.yingyinglicai.net:mobile/android/common/module.git"
        branch="master" />
 
</manifest>
```

#### \<project /> 元素
用于描述**root**工程的相关配置。

- **origin** 远程仓库地址
    
    未绑定远程仓库前，该值可缺省。绑定远程仓库时，根据该值信息自动调用GitLab API创建远程仓库。
    
- **branch** 当前分支
    
    可缺省，默认为 *master*。
    
#### \<module /> 元素
用于描述各个模块的相关配置。

- **name** 模块名称
    
    不可缺省，需与模块文件夹名称保存一致。
    
- **local** 本地路径
    
    所有模块必须位于工程根目录下。可缺省，默认为根目录。该值须为根目录的相对路径。

- **origin** 远程仓库地址
    
    未绑定远程仓库前，该值可缺省。绑定远程仓库时，根据该值信息自动调用GitLab API创建远程仓库。
    
- **branch** 当前分支
    
    可缺省，**默认同 \<project /> 元素的branch**。
    
#### \<include /> 元素
用于描述哪些模块与**root**工程同一个代码仓库。

- **module** 模块名称
    
    该值取自 \<module /> 元素的name值

#### \<compile />、\<api />、\<implementation />元素
用于描述该模块依赖与哪些模块。

- **name** 模块名称

    该值取自 \<module /> 元素的name值
