# Gradle Repo
Gradle Repo is a tool that we built on top of Git. it helps us manage the many Git repositories, very convenient to switch to other feature branches.


## Repo Manifest
A repo manifest describes the structure and dependency tree of a repo project; that is
the directories that are visible and where they should be obtained from with git. 
##### repo manifest sample

    <?xml version='1.0' encoding='UTF-8'?>
    <manifest>
     
        <project origin="https://github.com/EastWoodYang/gradle-repo.git"
            branch="master" >
     
            <include name="mylibrary3"/>
     
        </project>
     
        <module name="app" origin="./gradle-repo-app.git">
            <dependencies>
                <api name="mylibrary2"/>
                <implementation name="mylibrary"/>
                <apiDebug name="mylibrary"/>
            </dependencies>
        </module>
     
        <module name="mylibrary" origin="https://github.com/EastWoodYang/gradle-repo-mylibrary.git"/>
     
        <module name="mylibrary2" origin="./gradle-repo-mylibrary2.git"/>
     
        <module name="mylibrary3" />
     
    </manifest>

##### Element project
describe root project.

- Attribute `origin`: A git url of this project obtain from with git. 
- Attribute `branch`: Name of the Git branch the manifest wants to track for this module. If not supplied the branch given by the project element is used if applicable.
  
##### Element module
describe modules for this project.

- Attribute `name`: A unique name for this module. The module name must match the directory name of this module.
- Attribute `local`: An optional path relative to the top directory of the repo client where the Git working directory for this project should be placed. If not supplied the top directory path is used.
- Attribute `origin`: A git url of this module obtain from with git.
- Attribute `branch`: Name of the Git branch the manifest wants to track for this module. If not supplied the branch given by the project element is used if applicable.
    
##### Element include
Define which module and the project are in the same repository.

- Attribute `name`: The value must match the element module name.

##### Element dependencies
Declaring dependencies to a module

Chile Element Node Name must match the name [Gradle Dependency Configurations](https://docs.gradle.org/current/userguide/managing_dependency_configurations.html).
- Attribute `name`: The value must match the element module name.

## Gradle Repo plugin for Android Studio
Provides an action which allow you sync and bind remote origin repository when you modified repo.xml.

<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/1.png'/>
