package com.Sunset.REN.GitHub.domain.filemanager

object FileCompileCapabilityResolver {
    fun resolve(name: String, type: FileEntryType, isDirectory: Boolean = false): FileCompileCapability? {
        if (isDirectory) return null
        val normalizedName = name.substringAfterLast('/').lowercase()
        val extension = normalizedName.substringAfterLast('.', missingDelimiterValue = "")
        return when {
            normalizedName == "build.gradle" || normalizedName == "build.gradle.kts" -> FileCompileCapability(
                language = "Gradle",
                mode = FileCompileMode.BuildScript,
                toolHint = "Gradle Wrapper / Android Gradle Plugin"
            )
            normalizedName == "makefile" || extension == "mk" -> FileCompileCapability("Make", FileCompileMode.BuildScript, "make")
            normalizedName == "dockerfile" -> FileCompileCapability("Dockerfile", FileCompileMode.BuildScript, "docker build")
            extension == "kt" || extension == "kts" -> FileCompileCapability("Kotlin", FileCompileMode.Compile, "kotlinc / Gradle")
            extension == "java" -> FileCompileCapability("Java", FileCompileMode.Compile, "javac / Gradle")
            extension in setOf("c", "h") -> FileCompileCapability("C", FileCompileMode.Compile, "clang / gcc")
            extension in setOf("cc", "cpp", "cxx", "hpp") -> FileCompileCapability("C++", FileCompileMode.Compile, "clang++ / g++")
            extension == "rs" -> FileCompileCapability("Rust", FileCompileMode.Compile, "rustc / cargo")
            extension == "go" -> FileCompileCapability("Go", FileCompileMode.Compile, "go build")
            extension == "swift" -> FileCompileCapability("Swift", FileCompileMode.Compile, "swiftc")
            extension in setOf("py", "rb", "js", "mjs", "cjs", "ts", "tsx", "sh", "bash", "zsh", "lua", "php", "pl") -> FileCompileCapability(
                language = scriptLanguage(extension),
                mode = FileCompileMode.Interpret,
                toolHint = scriptToolHint(extension)
            )
            extension in setOf("html", "htm", "xml", "svg") || type == FileEntryType.Markdown -> FileCompileCapability(
                language = if (type == FileEntryType.Markdown) "Markdown" else extension.uppercase(),
                mode = FileCompileMode.Markup,
                toolHint = "markup renderer / browser"
            )
            extension in setOf("zip", "jar", "aar", "apk") -> FileCompileCapability(
                language = extension.uppercase(),
                mode = FileCompileMode.Package,
                toolHint = "archive/package inspector"
            )
            else -> null
        }
    }

    private fun scriptLanguage(extension: String): String {
        return when (extension) {
            "py" -> "Python"
            "rb" -> "Ruby"
            "js", "mjs", "cjs" -> "JavaScript"
            "ts", "tsx" -> "TypeScript"
            "sh", "bash", "zsh" -> "Shell"
            "lua" -> "Lua"
            "php" -> "PHP"
            "pl" -> "Perl"
            else -> extension.uppercase()
        }
    }

    private fun scriptToolHint(extension: String): String {
        return when (extension) {
            "py" -> "python"
            "rb" -> "ruby"
            "js", "mjs", "cjs" -> "node"
            "ts", "tsx" -> "tsc / node loader"
            "sh", "bash", "zsh" -> "shell"
            "lua" -> "lua"
            "php" -> "php"
            "pl" -> "perl"
            else -> "interpreter"
        }
    }
}
