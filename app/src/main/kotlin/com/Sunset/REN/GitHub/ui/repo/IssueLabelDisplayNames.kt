package com.Sunset.REN.GitHub.ui.repo

object IssueLabelDisplayNames {
    private val translations = mapOf(
        "bug" to "缺陷",
        "documentation" to "文档",
        "duplicate" to "重复",
        "enhancement" to "功能增强",
        "good first issue" to "新手友好",
        "help wanted" to "需要帮助",
        "invalid" to "无效",
        "question" to "问题",
        "wontfix" to "不修复",
        "dependencies" to "依赖",
        "security" to "安全",
        "performance" to "性能",
        "refactor" to "重构",
        "feature" to "功能",
        "release" to "发布",
        "testing" to "测试",
        "ci" to "持续集成",
        "build" to "构建",
        "android" to "Android",
        "ios" to "iOS",
        "api" to "接口",
        "ui" to "界面",
        "ux" to "体验",
        "backend" to "后端",
        "frontend" to "前端",
        "database" to "数据库",
        "cleanup" to "清理",
        "documentation-needed" to "需要文档",
        "bugfix" to "缺陷修复"
    )

    fun displayName(name: String): String {
        return translations[name.trim().lowercase()] ?: name
    }
}
