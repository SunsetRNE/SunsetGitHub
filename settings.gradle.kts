// 仓库策略：GitHub runner 在美国，官方源快且稳 → CI 环境官方源优先；
// 本地（中国网络）官方源慢/不稳 → 镜像优先，官方源兜底。
// 镜像（尤其阿里云）偶发 5xx，若镜像排在官方源之前会在 CI 上造成解析失败。
val isCi = System.getenv("CI") == "true"

pluginManagement {
    repositories {
        if (isCi) {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://repo.huaweicloud.com/repository/gradle-plugin/")
        maven("https://repo.huaweicloud.com/repository/maven/")
        if (!isCi) {
            google {
                content {
                    includeGroupByRegex("com\\.android.*")
                    includeGroupByRegex("com\\.google.*")
                    includeGroupByRegex("androidx.*")
                }
            }
            mavenCentral()
            gradlePluginPortal()
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (isCi) {
            google()
            mavenCentral()
        }
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/central")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://repo.huaweicloud.com/repository/maven/")
        if (!isCi) {
            google()
            mavenCentral()
        }
    }
}

rootProject.name = "SunsetGitHub"
include(":app")

