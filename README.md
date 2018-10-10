# Gradle Repo
Gradle Repo is a Gradle plugin. it helps us manage the many Git repositories, and very convenient and quick to switch to other feature branches.

## Features
* No other tools required.
* Clone the root project and open it by android studio, will automatically clone the code of other projects.
* very convenient and quick to switch to other feature branches.

## Usage
1. Add buildscript dependency in root project **settings.gradle** and apply plugin **gradle-repo-settings**

        buildscript {
            repositories {
                jcenter()
            }
            dependencies {
                classpath 'com.eastwood.tools.plugins:gradle-repo:1.0.0'
            }
        }
         
        apply plugin: 'gradle-repo-settings'

2. Add buildscript dependency in root project **build.gradle** and apply plugin **gradle-repo-build**
    
        buildscript {
            
            repositories {
                ...
                jcenter()
            }
            
            dependencies {
                ...
                classpath 'com.eastwood.tools.plugins:gradle-repo:1.0.0'
            }
        }
        
        apply plugin: 'gradle-repo-build'
        
3. Create repo.xml in root project, and describes the structure and dependency of the repo project.

    <img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/1.png'/>


## Repo Manifest Format
A repo manifest describes the structure and dependency of a repo project; that is
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
            </dependencies>
        </module>
     
        <module name="mylibrary" origin="https://github.com/EastWoodYang/gradle-repo-mylibrary.git"/>
     
        <module name="mylibrary2" origin="./gradle-repo-mylibrary2.git"/>
     
        <module name="mylibrary3" />
     
    </manifest>

##### Element project
At most one project must be specified.
This element describes a single Git repository to be cloned as a root project workspace.

- Attribute `origin`: Specify the URL of a Git repository. 
- Attribute `branch`: Name of the Git branch the manifest wants to track for this module.
  
##### Element module
One or more module elements may be specified.
Each element describes a single Git repository to be cloned into the root project workspace.

- Attribute `name`: A unique name for this module. The module name must match the directory name of this module.
- Attribute `local`: An optional path relative to the top directory of the repo client where the Git working directory for this project should be placed. If not supplied the top directory path is used.
- Attribute `origin`: Specify the URL of a Git repository. Support path relative to the project element origin.
- Attribute `branch`: Name of the Git branch the manifest wants to track for this module. If not supplied the branch given by the project element branch is used if applicable.
    
##### Element include
Zero or more include elements may be specified as children of a project element.
Define which module and the root project are in the same Git repository.

- Attribute `name`: The value must match the element module name.

##### Element dependencies
At most one project may be specified as children of a module element.
Declaring dependencies to a module.

Chile Element Node Name must match the name [Gradle Dependency Configurations](https://docs.gradle.org/current/userguide/managing_dependency_configurations.html) identified.
- Attribute `name`: The value must match the element module name.

## Gradle Repo plugin for Android Studio
Provides an action which allow you sync and bind remote origin repository when you modified repo.xml.

<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/2.png'/>
 
<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/3.png'/>

## Gradle Repo plugin for Jenkins
An SCM provider for Jenkins. Projects can use this plugin to only run builds when changes are detected in any of the git repositories in the repo manifest,
to list the changes between builds, and to re-create the project state across all repositories for any previous build using a static manifest.

<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/4.png'/>
[Plugin detail: https://plugins.jenkins.io/gradle-repo](https://plugins.jenkins.io/gradle-repo)

## QA

**Don't need to include project in settings.gradle any more ?**

    Yes, will automatically add the given module you declared in repo.xml to the build by repo plugin.
    
**How to switch to other feature branches ?**

    You only need to change the name of the project element branch, and then sync it. 
    
    Make sure to commit the code before syncing.

## License
```
   Copyright 2018 EastWood Yang

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
