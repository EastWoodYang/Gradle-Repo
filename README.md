
# Gradle Repo
用于统一管理Git多仓库及模块间的依赖关系。

## Usage

1. 分别在`settings.gradle`和`build.gradle`中添加**gradle-repo**插件。

```
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'com.eastwood.tools.plugins:gradle-repo:1.2.0'
    }
}
```

apply plugin: 'gradle-repo'
  
2. 创建repo.xml，并根据项目结构及依赖关系转换成xml格式。

## Take Look

<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/1.png' />
  
## Repo Manifest Format

[Repo清单格式](https://github.com/EastWoodYang/Gradle-Repo/wiki/Repo%E6%B8%85%E5%8D%95%E6%A0%BC%E5%BC%8F) / [Repo Manifest Format](https://github.com/EastWoodYang/Gradle-Repo/wiki/Repo-Manifest-Format)

## About repo-local.xml
repo-local.xml可以理解为`本地模式`。该模式下repo.xml声明的module将不会直接出现项目工程中，而是直接被移到`.idea/module`中。也就是说只有repo-local.xml声明的module才会出现在项目工程中。

另外，你也可以通过设置`disableLocalRepo`来禁用该模式，比如：

```
setting.gradle

...

ext.disableLocalRepo = true

apply plugin: 'gradle-repo'
```

## Be Careful

* **尽量清理掉settings.gradle中的`include`。如果一些模块不想被Gradle Repo管理，当然可以继续使用。**

* **分支切换尽量在根项目上操作，同步的时候，其他模块会自动跟随切换过去。如果有些模块是使用固定的分支，可以在<module />声明中指定`branch`**

## Gradle Repo plugin for Android Studio  

The following features are available:  
  
* Provides an action which allow you sync and remote origin repository when you modified repo.xml.  
* Support create Repo Tag, could be find in [VCS] -> [Git] -> [Create Repo Tag...].
  
<img src='https://github.com/EastWoodYang/gradle-repo-idea-plugin/blob/master/pictures/2.png'/>  
  
**Install Step**:  
1. open [File] -> [Settings...] -> [plugins] -> [Browse repositories...]  
2. and search name `Gradle Repo`
  
**Plugin detail**:  
  
[https://plugins.jetbrains.com/plugin/10876-gradle-repo](https://plugins.jetbrains.com/plugin/10876-gradle-repo)  
  
## Gradle Repo plugin for Jenkins  
An SCM provider for Jenkins. Projects can use this plugin to only run builds when changes are detected in any of the git repositories in the repo manifest,  
to list the changes between builds, and to re-create the project state across all repositories for any previous build using a static manifest.  
  
<img src='https://github.com/EastWoodYang/gradle-repo/blob/master/picture/4.png'/>  
Plugin detail: https://plugins.jenkins.io/gradle-repo
  
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
