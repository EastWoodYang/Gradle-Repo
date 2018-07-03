# Gradle Repo

## repo.xml

    <?xml version='1.0' encoding='UTF-8'?>
    <manifest>
     
        <project origin="https://github.com/EastWoodYang/gradle-repo.git"
            branch="master" >
     
            <include name="mylibrary3"/>
     
        </project>
     
        <module name="app" origin="./gradle-repo-app.git">
            <dependencies>
                <api name="mylibrary"/>
                <api name="mylibrary2"/>
            </dependencies>
        </module>
     
        <module name="mylibrary" origin="./gradle-repo-mylibrary.git"/>
     
        <module name="mylibrary2" origin="./gradle-repo-mylibrary2.git"/>
     
        <module name="mylibrary3" />
     
    </manifest>

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

- **name** 模块名称
    
    该值取自 \<module /> 元素的name值

#### \<dependencies />元素
用于描述该模块依赖与哪些模块。

- **name** 模块名称

    该值取自 \<module /> 元素的name值


## Gradle Repo plugin for Android Studio
Provides an action which allow you sync and remote origin repository when you modified repo.xml.

<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/1.png'/>
