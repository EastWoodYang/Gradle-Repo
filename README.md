
# Gradle Repo  
Gradle Repo is a Gradle plugin. it helps us manage the many Git repositories, and very convenient and quick to switch to other feature branches.  
  
## Features  
* No other tools required.  
* Clone the root project and open it by android studio, will automatically clone the code of other projects.  
* very convenient and quick to switch to other feature branches.  
  
## Usage  
1. Add buildscript dependency in root project **settings.gradle** and apply plugin **gradle-repo-settings**  

    ```groovy
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.eastwood.tools.plugins:gradle-repo:1.0.0'
        }
    }

    apply plugin: 'gradle-repo-settings'
    ```
  
2. Add buildscript dependency in root project **build.gradle** and apply plugin **gradle-repo-build**  

    ```
    buildscript {
        repositories {
            jcenter()
        }
        dependencies {
            classpath 'com.eastwood.tools.plugins:gradle-repo:1.0.0'
        }
    }

    apply plugin: 'gradle-repo-build'
    ```
          
3. Create repo.xml in root project, and describes the structure and dependency of the repo project.  
  
    <img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/1.png'/>  
  
  
## Repo Manifest Format

  
## Gradle Repo plugin for Android Studio  


The following features are available:  
  
* Provides an action which allow you sync and remote origin repository when you modified repo.xml.  
* Support create Repo Tag, could be find in [VCS] -> [Git] -> [Create Repo Tag...].  
  
<img src='https://github.com/EastWoodYang/gradle-repo-idea-plugin/blob/master/pictures/1.png'/>  
  
<img src='https://github.com/EastWoodYang/gradle-repo-idea-plugin/blob/master/pictures/2.png'/>  
  
**Install Step**:  
1. open [File] -> [Settings...] -> [plugins] -> [Browse repositories...]  
2. and search name **Gradle Repo**  
  
**Plugin detail**:  
  
[https://plugins.jetbrains.com/plugin/10876-gradle-repo](https://plugins.jetbrains.com/plugin/10876-gradle-repo)  
  
## Gradle Repo plugin for Jenkins  
An SCM provider for Jenkins. Projects can use this plugin to only run builds when changes are detected in any of the git repositories in the repo manifest,  
to list the changes between builds, and to re-create the project state across all repositories for any previous build using a static manifest.  
  
<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/4.png'/>  
Plugin detail: https://plugins.jenkins.io/gradle-repo  
  
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