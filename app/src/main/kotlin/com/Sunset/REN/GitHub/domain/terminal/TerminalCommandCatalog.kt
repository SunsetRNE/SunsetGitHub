package com.Sunset.REN.GitHub.domain.terminal

import com.Sunset.REN.GitHub.domain.workspace.WorkspaceRemoteBinding
import com.Sunset.REN.GitHub.domain.workspace.WorkspaceScanResult
import java.io.File

/**
 * Registry and built-in command implementations for the app-internal workspace terminal.
 *
 * Keep [WorkspaceCommandRunner] focused on request orchestration; command behavior lives here.
 * This terminal is intentionally sandboxed to the selected workspace and is not a system shell.
 */
internal object TerminalCommandCatalog {
    fun defaultCommands(workspaceScanner: suspend (String) -> WorkspaceScanResult): List<WorkspaceTerminalCommand> = listOf(
        HelpCommand,
        ClearCommand,
        PwdCommand,
        LsCommand,
        CdCommand,
        CatCommand,
        HeadCommand,
        TailCommand,
        PreviewCommand,
        StatCommand,
        WcCommand,
        DuCommand,
        EchoCommand,
        GrepCommand,
        ReplaceCommand,
        DiffCommand,
        ChecksumCommand,
        FindCommand,
        TreeCommand,
        MkdirCommand,
        TouchCommand,
        RmCommand,
        WriteCommand,
        AppendCommand,
        CpCommand,
        MvCommand,
        LnCommand,
        StatusCommand(workspaceScanner),
        WorkspaceCommand,
        RemoteCommand,
        DryRunCommand,
        SyncCommand,
        HistoryCommand,
        RecentCommand
    )
}

private object HelpCommand : WorkspaceTerminalCommand {
    override val name: String = "help"
    override val description: String = "显示可用命令或命令详情"
    override val usage: String = "help [command]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val target = args.firstOrNull()
        if (target == null) {
            return TerminalCommandResult(
                output = buildString {
                    appendLine("SunsetGitHub 工作区终端")
                    appendLine("这是工作区命令终端，不是系统 shell。所有路径都限制在当前工作区内。")
                    appendLine()
                    appendLine("可用命令：")
                    allCommands.sortedBy { it.name }.forEach { command ->
                        appendLine("${command.usage.padEnd(22)} ${command.description}")
                    }
                    appendLine()
                    appendLine("使用 help <命令名> 查看命令详情和示例。")
                }.trimEnd()
            )
        }
        val found = allCommands.firstOrNull { it.name.equals(target, ignoreCase = true) }
            ?: return TerminalCommandResult(error = "未找到命令：$target")
        return TerminalCommandResult(
            output = buildString {
                appendLine("命令：${found.name}")
                appendLine("说明：${found.description}")
                appendLine("用法：${found.usage}")
                when (found.name) {
                    "replace" -> {
                        appendLine()
                        appendLine("示例：")
                        appendLine("  替换预览  replace README.md \"old\" \"new\" --all")
                        appendLine("  确认替换  replace README.md \"old\" \"new\" --all --confirm")
                        appendLine()
                        appendLine("安全提醒：默认只预览不写入，必须加 --confirm 才会真正修改文件。")
                    }
                    "rm" -> {
                        appendLine()
                        appendLine("安全提醒：删除操作必须追加 --confirm。")
                        appendLine("  删除目录需同时加 --recursive --confirm。")
                        appendLine("  根目录受保护，不可删除。")
                    }
                    "sync" -> {
                        appendLine()
                        appendLine("示例：")
                        appendLine("  只上传         sync \"update docs\"")
                        appendLine("  镜像同步预演   sync --mirror \"mirror test\"")
                        appendLine("  镜像同步确认   sync --mirror --confirm-delete \"mirror\"")
                        appendLine("  覆盖远端变化   sync --allow-overwrite-remote \"local wins\"")
                        appendLine()
                        appendLine("安全提醒：--mirror --confirm-delete 会删除远端多余文件，请谨慎。")
                        appendLine("安全提醒：--allow-overwrite-remote 会在远端分支变化后仍以本地同路径内容覆盖远端。")
                        appendLine("  建议先用 dry-run --mirror 或 dry-run --allow-overwrite-remote 预演。")
                    }
                    "dry-run" -> {
                        appendLine()
                        appendLine("示例：")
                        appendLine("  dry-run \"test plan\"")
                        appendLine("  dry-run --mirror")
                        appendLine()
                        appendLine("说明：不会提交，仅生成并展示同步计划。")
                    }
                    "remote" -> {
                        appendLine()
                        appendLine("示例：")
                        appendLine("  查看绑定  remote")
                        appendLine("  绑定远端  remote set myuser myrepo main")
                        appendLine("  清除绑定  remote clear")
                    }
                    "workspace" -> {
                        appendLine()
                        appendLine("示例：")
                        appendLine("  查看当前  workspace")
                        appendLine("  列出所有  workspace list")
                        appendLine("  切换工作区 workspace use <id|name>")
                    }
                    "write" -> {
                        appendLine()
                        appendLine("安全提醒：覆盖已有文件必须加 --overwrite。")
                    }
                    "cp", "mv" -> {
                        appendLine()
                        appendLine("  覆盖已有目标请追加 --overwrite。")
                        appendLine("  复制目录需追加 --recursive。")
                    }
                }
            }.trimEnd()
        )
    }
}

private object ClearCommand : WorkspaceTerminalCommand {
    override val name: String = "clear"
    override val description: String = "清空终端输出"
    override val usage: String = "clear"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult = TerminalCommandResult(clearScreen = true)
}

private object PwdCommand : WorkspaceTerminalCommand {
    override val name: String = "pwd"
    override val description: String = "显示当前工作区目录"
    override val usage: String = "pwd"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val current = context.resolveInsideWorkspace(args.firstOrNull() ?: context.currentDirectory)
        return TerminalCommandResult(output = current.relativePathFor(context.rootDirectory).ifBlank { "/" })
    }
}

private object LsCommand : WorkspaceTerminalCommand {
    override val name: String = "ls"
    override val description: String = "列出工作区目录内容"
    override val usage: String = "ls [path]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val target = context.resolveInsideWorkspace(args.firstOrNull() ?: context.currentDirectory)
        if (!target.exists()) return TerminalCommandResult(error = "路径不存在：${target.relativePathFor(context.rootDirectory)}")
        if (target.isFile) return TerminalCommandResult(output = target.name)
        val children = target.listFiles().orEmpty().sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
        return TerminalCommandResult(
            output = if (children.isEmpty()) "<empty>" else children.joinToString("\n") { file ->
                val suffix = if (file.isDirectory) "/" else ""
                val size = if (file.isFile) "  ${file.length()}B" else ""
                "${file.name}$suffix$size"
            }
        )
    }
}

private object CdCommand : WorkspaceTerminalCommand {
    override val name: String = "cd"
    override val description: String = "切换当前工作区目录"
    override val usage: String = "cd [path]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val target = context.resolveInsideWorkspace(args.firstOrNull() ?: "")
        if (!target.exists()) return TerminalCommandResult(error = "路径不存在：${args.firstOrNull().orEmpty()}")
        if (!target.isDirectory) return TerminalCommandResult(error = "不是目录：${target.relativePathFor(context.rootDirectory)}")
        return TerminalCommandResult(
            output = target.relativePathFor(context.rootDirectory).ifBlank { "/" },
            nextDirectory = target.relativePathFor(context.rootDirectory)
        )
    }
}

private object CatCommand : WorkspaceTerminalCommand {
    override val name: String = "cat"
    override val description: String = "查看文本文件内容"
    override val usage: String = "cat <path>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val target = context.resolveInsideWorkspace(path)
        if (!target.exists()) return TerminalCommandResult(error = "文件不存在：$path")
        if (!target.isFile) return TerminalCommandResult(error = "不是文件：$path")
        if (target.length() > MaxCatBytes) return TerminalCommandResult(error = "文件过大，仅支持查看 ${MaxCatBytes}B 以内的文本文件。")
        val bytes = target.readBytes()
        if (bytes.any { byte -> byte == 0.toByte() }) return TerminalCommandResult(error = "疑似二进制文件，已拒绝 cat。")
        return TerminalCommandResult(output = bytes.toString(Charsets.UTF_8))
    }

    private const val MaxCatBytes = 128 * 1024L
}

private object HeadCommand : WorkspaceTerminalCommand {
    override val name: String = "head"
    override val description: String = "查看文本文件前若干行，默认 20 行"
    override val usage: String = "head <path> [lines]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val lineCount = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, MaxPreviewLines) ?: DefaultPreviewLines
        val target = context.resolveTextFile(path) ?: return TerminalCommandResult(error = "不是可读取文本文件：$path")
        return TerminalCommandResult(output = target.readLines().take(lineCount).joinToString("\n"))
    }
}

private object TailCommand : WorkspaceTerminalCommand {
    override val name: String = "tail"
    override val description: String = "查看文本文件后若干行，默认 20 行"
    override val usage: String = "tail <path> [lines]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val lineCount = args.getOrNull(1)?.toIntOrNull()?.coerceIn(1, MaxPreviewLines) ?: DefaultPreviewLines
        val target = context.resolveTextFile(path) ?: return TerminalCommandResult(error = "不是可读取文本文件：$path")
        val lines = target.readLines()
        return TerminalCommandResult(output = lines.takeLast(lineCount).joinToString("\n"))
    }
}

private object PreviewCommand : WorkspaceTerminalCommand {
    override val name: String = "preview"
    override val description: String = "带行号预览文本文件片段"
    override val usage: String = "preview <path> [start] [lines]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val startLine = args.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val lineCount = args.getOrNull(2)?.toIntOrNull()?.coerceIn(1, MaxPreviewLines) ?: DefaultPreviewLines
        val target = context.resolveTextFile(path) ?: return TerminalCommandResult(error = "不是可读取文本文件：$path")
        val lines = target.readLines()
        if (lines.isEmpty()) return TerminalCommandResult(output = "<empty>")
        if (startLine > lines.size) return TerminalCommandResult(error = "起始行超过文件总行数：$startLine > ${lines.size}")
        val lineNumberWidth = (startLine + lineCount - 1).coerceAtMost(lines.size).toString().length.coerceAtLeast(3)
        val output = lines
            .drop(startLine - 1)
            .take(lineCount)
            .mapIndexed { index, line -> "${(startLine + index).toString().padStart(lineNumberWidth)}| $line" }
            .joinToString("\n")
        return TerminalCommandResult(output = output)
    }
}

private object StatCommand : WorkspaceTerminalCommand {
    override val name: String = "stat"
    override val description: String = "显示文件或目录的大小、类型和修改时间"
    override val usage: String = "stat <path>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val target = context.resolveInsideWorkspace(path)
        if (!target.exists()) return TerminalCommandResult(error = "路径不存在：$path")
        return TerminalCommandResult(
            output = buildString {
                appendLine("path: ${target.relativePathFor(context.rootDirectory).ifBlank { "/" }}")
                appendLine("type: ${if (target.isDirectory) "directory" else "file"}")
                appendLine("size: ${if (target.isDirectory) "${target.directorySizeBytes()}B" else "${target.length()}B"}")
                appendLine("modified: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date(target.lastModified()))}")
                appendLine("readable: ${target.canRead()}")
                appendLine("writable: ${target.canWrite()}")
            }.trimEnd()
        )
    }
}

private object WcCommand : WorkspaceTerminalCommand {
    override val name: String = "wc"
    override val description: String = "统计文本文件的行数、词数和字节数"
    override val usage: String = "wc <path>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val target = context.resolveTextFile(path) ?: return TerminalCommandResult(error = "不是可读取文本文件：$path")
        val text = target.readText()
        val lines = if (text.isEmpty()) 0 else text.lineSequence().count()
        val words = text.split(Regex("\\s+")).count { it.isNotBlank() }
        return TerminalCommandResult(output = "${lines.toString().padStart(7)} ${words.toString().padStart(7)} ${target.length().toString().padStart(7)} ${target.relativePathFor(context.rootDirectory)}")
    }
}

private object DuCommand : WorkspaceTerminalCommand {
    override val name: String = "du"
    override val description: String = "统计文件或目录占用大小"
    override val usage: String = "du [path] [--human]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val human = args.contains("--human") || args.contains("-h")
        val path = args.firstOrNull { !it.startsWith("-") } ?: context.currentDirectory
        val target = context.resolveInsideWorkspace(path)
        if (!target.exists()) return TerminalCommandResult(error = "路径不存在：$path")
        val bytes = if (target.isDirectory) target.directorySizeBytes() else target.length()
        val displaySize = if (human) bytes.toHumanSize() else "${bytes}B"
        return TerminalCommandResult(output = "$displaySize\t${target.relativePathFor(context.rootDirectory).ifBlank { "/" }}")
    }
}

private object EchoCommand : WorkspaceTerminalCommand {
    override val name: String = "echo"
    override val description: String = "输出文本；可用 > 或 >> 写入/追加到工作区文件"
    override val usage: String = "echo <text...> [> path|>> path]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val redirectIndex = args.indexOfFirst { it == ">" || it == ">>" }
        if (redirectIndex < 0) return TerminalCommandResult(output = args.joinToString(" "))
        val operator = args[redirectIndex]
        val targetPath = args.getOrNull(redirectIndex + 1) ?: return TerminalCommandResult(error = "重定向缺少目标路径。用法：$usage")
        val text = args.take(redirectIndex).joinToString(" ")
        val target = context.resolveInsideWorkspace(targetPath)
        if (target.exists() && target.isDirectory) return TerminalCommandResult(error = "目标是目录：$targetPath")
        target.parentFile?.mkdirs()
        if (operator == ">>") target.appendText(text + "\n") else target.writeText(text + "\n")
        return TerminalCommandResult(output = "已${if (operator == ">>") "追加" else "写入"}：${target.relativePathFor(context.rootDirectory)} (${target.length()}B)")
    }
}

private object ReplaceCommand : WorkspaceTerminalCommand {
    override val name: String = "replace"
    override val description: String = "替换文本文件中的内容，默认只预览"
    override val usage: String = "replace <path> <old> <new> [--all] [--confirm]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val positional = args.filterNot { it.startsWith("--") }
        val path = positional.getOrNull(0) ?: return TerminalCommandResult(error = "用法：$usage")
        val oldText = positional.getOrNull(1) ?: return TerminalCommandResult(error = "用法：$usage")
        val newText = positional.getOrNull(2) ?: return TerminalCommandResult(error = "用法：$usage")
        if (oldText.isEmpty()) return TerminalCommandResult(error = "旧文本不能为空。")
        val replaceAll = args.contains("--all")
        val confirmed = args.contains("--confirm") || args.contains("--yes")
        val target = context.resolveTextFile(path) ?: return TerminalCommandResult(error = "不是可读取文本文件：$path")
        val original = target.readText()
        val occurrenceCount = Regex.escape(oldText).toRegex().findAll(original).count()
        if (occurrenceCount == 0) return TerminalCommandResult(output = "未找到匹配内容：$oldText")
        val updated = if (replaceAll) original.replace(oldText, newText) else original.replaceFirst(oldText, newText)
        val changedCount = if (replaceAll) occurrenceCount else 1
        if (!confirmed) {
            return TerminalCommandResult(
                output = buildString {
                    appendLine("将替换 $changedCount/${occurrenceCount} 处匹配。")
                    appendLine("目标：${target.relativePathFor(context.rootDirectory)}")
                    appendLine("旧文本：$oldText")
                    appendLine("新文本：$newText")
                    appendLine("这是预览，确认写入请追加 --confirm。")
                }.trimEnd()
            )
        }
        target.writeText(updated)
        return TerminalCommandResult(output = "已替换 $changedCount 处：${target.relativePathFor(context.rootDirectory)} (${target.length()}B)")
    }
}

private object DiffCommand : WorkspaceTerminalCommand {
    override val name: String = "diff"
    override val description: String = "比较两个文本文件的逐行差异"
    override val usage: String = "diff <left> <right>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val leftPath = args.getOrNull(0) ?: return TerminalCommandResult(error = "用法：$usage")
        val rightPath = args.getOrNull(1) ?: return TerminalCommandResult(error = "用法：$usage")
        val left = context.resolveTextFile(leftPath) ?: return TerminalCommandResult(error = "不是可读取文本文件：$leftPath")
        val right = context.resolveTextFile(rightPath) ?: return TerminalCommandResult(error = "不是可读取文本文件：$rightPath")
        val leftLines = left.readLines()
        val rightLines = right.readLines()
        val max = maxOf(leftLines.size, rightLines.size)
        val diffs = mutableListOf<String>()
        for (index in 0 until max) {
            val leftLine = leftLines.getOrNull(index)
            val rightLine = rightLines.getOrNull(index)
            if (leftLine != rightLine) {
                diffs += "@@ line ${index + 1} @@"
                leftLine?.let { diffs += "- $it" } ?: run { diffs += "- <missing>" }
                rightLine?.let { diffs += "+ $it" } ?: run { diffs += "+ <missing>" }
                if (diffs.size >= MaxDiffLines) break
            }
        }
        if (diffs.isEmpty()) return TerminalCommandResult(output = "文件内容一致。")
        if (diffs.size >= MaxDiffLines) diffs += "... 已截断，最多显示 $MaxDiffLines 行差异输出"
        return TerminalCommandResult(output = diffs.joinToString("\n"))
    }

    private const val MaxDiffLines = 120
}

private object ChecksumCommand : WorkspaceTerminalCommand {
    override val name: String = "checksum"
    override val description: String = "计算文件 SHA-256 摘要"
    override val usage: String = "checksum <path>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val target = context.resolveInsideWorkspace(path)
        if (!target.exists() || !target.isFile) return TerminalCommandResult(error = "不是文件：$path")
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        target.inputStream().use { input ->
            val buffer = ByteArray(8 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        val hex = digest.digest().joinToString("") { byte -> "%02x".format(byte) }
        return TerminalCommandResult(output = "$hex  ${target.relativePathFor(context.rootDirectory)}")
    }
}

private object FindCommand : WorkspaceTerminalCommand {
    override val name: String = "find"
    override val description: String = "按名称查找工作区文件，最多 80 项"
    override val usage: String = "find <keyword> [path]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val keyword = args.firstOrNull()?.lowercase() ?: return TerminalCommandResult(error = "用法：$usage")
        val start = context.resolveInsideWorkspace(args.getOrNull(1) ?: context.currentDirectory)
        if (!start.exists() || !start.isDirectory) return TerminalCommandResult(error = "搜索路径不是目录。")
        val matches = start.walkTopDown()
            .filter { it != start && it.name.lowercase().contains(keyword) }
            .take(MaxSearchResults)
            .map { it.relativePathFor(context.rootDirectory) + if (it.isDirectory) "/" else "" }
            .toList()
        return TerminalCommandResult(output = matches.ifEmpty { listOf("<no matches>") }.joinToString("\n"))
    }
}

private object GrepCommand : WorkspaceTerminalCommand {
    override val name: String = "grep"
    override val description: String = "在文本文件中搜索关键词，最多 80 项"
    override val usage: String = "grep <keyword> [path]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val keyword = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val start = context.resolveInsideWorkspace(args.getOrNull(1) ?: context.currentDirectory)
        if (!start.exists()) return TerminalCommandResult(error = "搜索路径不存在。")
        val files = if (start.isFile) sequenceOf(start) else start.walkTopDown().filter { it.isFile }
        val matches = mutableListOf<String>()
        files.forEach { file ->
            if (matches.size >= MaxSearchResults) return@forEach
            if (!file.isLikelyTextFile()) return@forEach
            runCatching { file.readLines() }.getOrNull()?.forEachIndexed { index, line ->
                if (matches.size < MaxSearchResults && line.contains(keyword, ignoreCase = true)) {
                    matches += "${file.relativePathFor(context.rootDirectory)}:${index + 1}: $line"
                }
            }
        }
        return TerminalCommandResult(output = matches.ifEmpty { listOf("<no matches>") }.joinToString("\n"))
    }
}

private object TreeCommand : WorkspaceTerminalCommand {
    override val name: String = "tree"
    override val description: String = "树状展示工作区目录，默认最多 80 项"
    override val usage: String = "tree [path]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val target = context.resolveInsideWorkspace(args.firstOrNull() ?: context.currentDirectory)
        if (!target.exists()) return TerminalCommandResult(error = "路径不存在。")
        val lines = mutableListOf(target.name.ifBlank { "/" })
        var count = 0
        fun visit(directory: File, prefix: String) {
            if (count >= MaxTreeEntries) return
            val children = directory.listFiles().orEmpty().sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            children.forEachIndexed { index, child ->
                if (count >= MaxTreeEntries) return
                val last = index == children.lastIndex
                val branch = if (last) "└── " else "├── "
                lines += prefix + branch + child.name + if (child.isDirectory) "/" else ""
                count++
                if (child.isDirectory) {
                    visit(child, prefix + if (last) "    " else "│   ")
                }
            }
        }
        if (target.isDirectory) visit(target, "")
        if (count >= MaxTreeEntries) lines += "... 已截断，最多显示 $MaxTreeEntries 项"
        return TerminalCommandResult(output = lines.joinToString("\n"))
    }

    private const val MaxTreeEntries = 80
}

private object MkdirCommand : WorkspaceTerminalCommand {
    override val name: String = "mkdir"
    override val description: String = "在工作区内创建目录"
    override val usage: String = "mkdir <path>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val target = context.resolveInsideWorkspace(path)
        if (target.exists() && !target.isDirectory) return TerminalCommandResult(error = "目标已存在且不是目录：$path")
        target.mkdirs()
        return TerminalCommandResult(output = "已创建目录：${target.relativePathFor(context.rootDirectory).ifBlank { "/" }}")
    }
}

private object TouchCommand : WorkspaceTerminalCommand {
    override val name: String = "touch"
    override val description: String = "创建空文件或更新时间戳"
    override val usage: String = "touch <path>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val target = context.resolveInsideWorkspace(path)
        if (target.exists() && target.isDirectory) return TerminalCommandResult(error = "目标是目录：$path")
        target.parentFile?.mkdirs()
        if (!target.exists()) target.writeText("") else target.setLastModified(System.currentTimeMillis())
        return TerminalCommandResult(output = "已 touch：${target.relativePathFor(context.rootDirectory)}")
    }
}

private object RmCommand : WorkspaceTerminalCommand {
    override val name: String = "rm"
    override val description: String = "删除工作区内文件；删除目录必须带 --recursive --confirm"
    override val usage: String = "rm <path> [--recursive] [--confirm]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull { !it.startsWith("--") } ?: return TerminalCommandResult(error = "用法：$usage")
        val target = context.resolveInsideWorkspace(path)
        if (target.canonicalFile == context.rootDirectory.canonicalFile) return TerminalCommandResult(error = "拒绝删除工作区根目录。")
        if (!target.exists()) return TerminalCommandResult(error = "路径不存在：$path")
        val recursive = args.contains("--recursive") || args.contains("-r")
        val confirmed = args.contains("--confirm") || args.contains("--yes")
        if (target.isDirectory && !recursive) return TerminalCommandResult(error = "目标是目录，必须追加 --recursive。")
        if (!confirmed) return TerminalCommandResult(error = "删除操作必须追加 --confirm。")
        val deleted = if (target.isDirectory) target.deleteRecursively() else target.delete()
        return if (deleted) {
            TerminalCommandResult(output = "已删除：$path")
        } else {
            TerminalCommandResult(error = "删除失败：$path")
        }
    }
}

private object WriteCommand : WorkspaceTerminalCommand {
    override val name: String = "write"
    override val description: String = "写入文本文件；覆盖已有文件必须带 --overwrite"
    override val usage: String = "write <path> <text...> [--overwrite]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val overwrite = args.contains("--overwrite")
        val text = args.drop(1).filterNot { it == "--overwrite" }.joinToString(" ")
        val target = context.resolveInsideWorkspace(path)
        if (target.exists() && target.isDirectory) return TerminalCommandResult(error = "目标是目录：$path")
        if (target.exists() && !overwrite) return TerminalCommandResult(error = "文件已存在，覆盖请追加 --overwrite。")
        target.parentFile?.mkdirs()
        target.writeText(text)
        return TerminalCommandResult(output = "已写入：${target.relativePathFor(context.rootDirectory)} (${target.length()}B)")
    }
}

private object AppendCommand : WorkspaceTerminalCommand {
    override val name: String = "append"
    override val description: String = "向文本文件追加内容"
    override val usage: String = "append <path> <text...>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val path = args.firstOrNull() ?: return TerminalCommandResult(error = "用法：$usage")
        val text = args.drop(1).joinToString(" ")
        val target = context.resolveInsideWorkspace(path)
        if (target.exists() && target.isDirectory) return TerminalCommandResult(error = "目标是目录：$path")
        target.parentFile?.mkdirs()
        target.appendText(text)
        return TerminalCommandResult(output = "已追加：${target.relativePathFor(context.rootDirectory)} (${target.length()}B)")
    }
}

private object CpCommand : WorkspaceTerminalCommand {
    override val name: String = "cp"
    override val description: String = "复制工作区内文件；目录复制必须带 --recursive"
    override val usage: String = "cp <from> <to> [--recursive] [--overwrite]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val positional = args.filterNot { it.startsWith("--") }
        val from = positional.getOrNull(0) ?: return TerminalCommandResult(error = "用法：$usage")
        val to = positional.getOrNull(1) ?: return TerminalCommandResult(error = "用法：$usage")
        val source = context.resolveInsideWorkspace(from)
        val target = context.resolveInsideWorkspace(to)
        val recursive = args.contains("--recursive") || args.contains("-r")
        val overwrite = args.contains("--overwrite")
        if (!source.exists()) return TerminalCommandResult(error = "来源不存在：$from")
        if (source.canonicalFile == target.canonicalFile) return TerminalCommandResult(error = "来源和目标相同。")
        if (source.isDirectory && !recursive) return TerminalCommandResult(error = "复制目录必须追加 --recursive。")
        if (target.exists() && !overwrite) return TerminalCommandResult(error = "目标已存在，覆盖请追加 --overwrite。")
        if (source.isDirectory) {
            if (target.exists()) target.deleteRecursively()
            source.copyRecursively(target, overwrite = true)
        } else {
            target.parentFile?.mkdirs()
            source.copyTo(target, overwrite = overwrite)
        }
        return TerminalCommandResult(output = "已复制：$from -> $to")
    }
}

private object MvCommand : WorkspaceTerminalCommand {
    override val name: String = "mv"
    override val description: String = "移动或重命名工作区内文件/目录"
    override val usage: String = "mv <from> <to> [--overwrite]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val positional = args.filterNot { it.startsWith("--") }
        val from = positional.getOrNull(0) ?: return TerminalCommandResult(error = "用法：$usage")
        val to = positional.getOrNull(1) ?: return TerminalCommandResult(error = "用法：$usage")
        val source = context.resolveInsideWorkspace(from)
        val target = context.resolveInsideWorkspace(to)
        val overwrite = args.contains("--overwrite")
        if (!source.exists()) return TerminalCommandResult(error = "来源不存在：$from")
        if (source.canonicalFile == context.rootDirectory.canonicalFile) return TerminalCommandResult(error = "拒绝移动工作区根目录。")
        if (source.canonicalFile == target.canonicalFile) return TerminalCommandResult(error = "来源和目标相同。")
        if (target.exists()) {
            if (!overwrite) return TerminalCommandResult(error = "目标已存在，覆盖请追加 --overwrite。")
            if (target.isDirectory) target.deleteRecursively() else target.delete()
        }
        target.parentFile?.mkdirs()
        val renamed = source.renameTo(target)
        if (!renamed) {
            if (source.isDirectory) {
                source.copyRecursively(target, overwrite = true)
                source.deleteRecursively()
            } else {
                source.copyTo(target, overwrite = true)
                source.delete()
            }
        }
        return TerminalCommandResult(output = "已移动：$from -> $to")
    }
}

private object LnCommand : WorkspaceTerminalCommand {
    override val name: String = "ln"
    override val description: String = "创建工作区内文件的副本链接（安全降级为复制）"
    override val usage: String = "ln <from> <to> [--overwrite]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val positional = args.filterNot { it.startsWith("--") }
        val from = positional.getOrNull(0) ?: return TerminalCommandResult(error = "用法：$usage")
        val to = positional.getOrNull(1) ?: return TerminalCommandResult(error = "用法：$usage")
        val source = context.resolveInsideWorkspace(from)
        val target = context.resolveInsideWorkspace(to)
        val overwrite = args.contains("--overwrite")
        if (!source.exists() || !source.isFile) return TerminalCommandResult(error = "来源不是文件：$from")
        if (target.exists() && !overwrite) return TerminalCommandResult(error = "目标已存在，覆盖请追加 --overwrite。")
        if (target.exists() && target.isDirectory) return TerminalCommandResult(error = "目标是目录：$to")
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
        return TerminalCommandResult(output = "已创建文件副本：$from -> $to")
    }
}

private class StatusCommand(
    private val workspaceScanner: suspend (String) -> WorkspaceScanResult
) : WorkspaceTerminalCommand {
    override val name: String = "status"
    override val description: String = "扫描工作区文件、忽略项和敏感文件"
    override val usage: String = "status"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val result = workspaceScanner(context.workspace.id)
        return TerminalCommandResult(
            output = buildString {
                appendLine("工作区：${context.workspace.name}")
                appendLine("文件：${result.files.size}")
                appendLine("忽略：${result.ignoredPaths.size}")
                appendLine("敏感：${result.sensitivePaths.size}")
                if (result.sensitivePaths.isNotEmpty()) {
                    appendLine()
                    appendLine("敏感文件：")
                    result.sensitivePaths.take(10).forEach { sensitive ->
                        appendLine("- ${sensitive.relativePath}: ${sensitive.reason}")
                    }
                }
                if (result.files.isNotEmpty()) {
                    appendLine()
                    appendLine("前 20 个文件：")
                    result.files.take(20).forEach { file -> appendLine("- ${file.relativePath} (${file.sizeBytes}B)") }
                }
            }.trimEnd()
        )
    }
}

private object WorkspaceCommand : WorkspaceTerminalCommand {
    override val name: String = "workspace"
    override val description: String = "显示、列出或切换工作区"
    override val usage: String = "workspace | workspace list | workspace use <id|name>"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        if (args.firstOrNull() == "list") {
            val workspaces = context.workspaceLister()
            return TerminalCommandResult(
                output = if (workspaces.isEmpty()) {
                    "暂无工作区。"
                } else {
                    workspaces.joinToString("\n") { workspace ->
                        val current = if (workspace.id == context.workspace.id) "*" else " "
                        val remote = workspace.remoteBinding?.repositoryFullName ?: "<no remote>"
                        "$current ${workspace.name}  ${workspace.id.take(8)}  $remote"
                    }
                }
            )
        }
        if (args.firstOrNull() == "use") {
            val selector = args.getOrNull(1)?.trim().orEmpty()
            if (selector.isBlank()) return TerminalCommandResult(error = "用法：$usage")
            val workspaces = context.workspaceLister()
            val matches = workspaces.filter { workspace ->
                workspace.id == selector ||
                    workspace.id.startsWith(selector, ignoreCase = true) ||
                    workspace.name.equals(selector, ignoreCase = true)
            }
            val selected = when {
                matches.isEmpty() -> return TerminalCommandResult(error = "未找到工作区：$selector")
                matches.size > 1 -> return TerminalCommandResult(
                    error = "匹配到多个工作区，请使用更完整的 id：\n" +
                        matches.joinToString("\n") { "- ${it.name}  ${it.id}" }
                )
                else -> matches.first()
            }
            return TerminalCommandResult(
                updatedWorkspace = selected,
                nextDirectory = "",
                output = "已切换工作区：${selected.name}\n${selected.rootPath}"
            )
        }
        val remote = context.workspace.remoteBinding
        return TerminalCommandResult(
            output = buildString {
                appendLine("name: ${context.workspace.name}")
                appendLine("id: ${context.workspace.id}")
                appendLine("root: ${context.workspace.rootPath}")
                appendLine("remote: ${remote?.repositoryFullName ?: "<none>"}")
            }.trimEnd()
        )
    }
}

private object RemoteCommand : WorkspaceTerminalCommand {
    override val name: String = "remote"
    override val description: String = "查看或设置工作区远端绑定"
    override val usage: String = "remote | remote set <owner> <repo> <branch> [path] | remote clear"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        if (args.isEmpty()) {
            val remote = context.workspace.remoteBinding
                ?: return TerminalCommandResult(output = "当前工作区尚未绑定远端。用法：$usage")
            return TerminalCommandResult(
                output = buildString {
                    appendLine("owner: ${remote.owner}")
                    appendLine("repo: ${remote.repo}")
                    appendLine("branch: ${remote.branch}")
                    appendLine("path: ${remote.normalizedRemotePath.ifBlank { "/" }}")
                }.trimEnd()
            )
        }
        if (args.firstOrNull() == "clear") {
            val updated = context.remoteClearer(context.workspace.id)
            return TerminalCommandResult(updatedWorkspace = updated, output = "已清除当前工作区远端绑定。")
        }
        if (args.firstOrNull() != "set" || args.size < 4) {
            return TerminalCommandResult(error = "用法：$usage")
        }
        val owner = args[1]
        val repo = args[2]
        val branch = args[3].ifBlank { "main" }
        val remotePath = args.getOrNull(4).orEmpty()
        if (owner.isBlank() || repo.isBlank()) return TerminalCommandResult(error = "owner/repo 不能为空。")
        val updated = context.remoteBinder(
            context.workspace.id,
            WorkspaceRemoteBinding(owner = owner, repo = repo, branch = branch, remotePath = remotePath)
        )
        val remote = updated.remoteBinding
        return TerminalCommandResult(
            updatedWorkspace = updated,
            output = "已绑定远端：${remote?.repositoryFullName}:${remote?.branch}/${remote?.normalizedRemotePath.orEmpty()}"
        )
    }
}

private object DryRunCommand : WorkspaceTerminalCommand {
    override val name: String = "dry-run"
    override val description: String = "基于当前远端绑定生成同步计划但不提交"
    override val usage: String = "dry-run [message...] [--mirror] [--confirm-delete] [--allow-overwrite-remote]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult = executeSyncLike(context, args, dryRun = true)
}

private object SyncCommand : WorkspaceTerminalCommand {
    override val name: String = "sync"
    override val description: String = "基于当前远端绑定执行工作区同步"
    override val usage: String = "sync [message...] [--mirror] [--confirm-delete] [--allow-overwrite-remote]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult = executeSyncLike(context, args, dryRun = false)
}

private suspend fun executeSyncLike(
    context: WorkspaceTerminalContext,
    args: List<String>,
    dryRun: Boolean
): TerminalCommandResult {
    if (context.workspace.remoteBinding == null) {
        return TerminalCommandResult(error = "当前工作区尚未绑定远端。请先执行 remote set <owner> <repo> <branch> [path]")
    }
    val mirrorMode = args.contains("--mirror")
    val destructiveConfirmed = args.contains("--confirm-delete") || args.contains("--yes")
    val allowOverwriteRemoteChanges = args.contains("--allow-overwrite-remote")
    val message = args
        .filterNot { it == "--mirror" || it == "--confirm-delete" || it == "--yes" || it == "--allow-overwrite-remote" }
        .joinToString(" ")
        .ifBlank { if (dryRun) "Dry run workspace sync from SunsetGitHub" else "Sync workspace from SunsetGitHub" }
    if (mirrorMode && !dryRun && !destructiveConfirmed) {
        return TerminalCommandResult(error = "sync --mirror 可能删除远端文件，必须追加 --confirm-delete。建议先运行 dry-run --mirror。")
    }
    val output = context.syncExecutor(
        TerminalSyncCommandRequest(
            workspace = context.workspace,
            dryRun = dryRun,
            mirrorMode = mirrorMode,
            destructiveConfirmed = destructiveConfirmed,
            allowOverwriteRemoteChanges = allowOverwriteRemoteChanges,
            commitMessage = message,
            progress = context.progressReporter
        )
    )
    return TerminalCommandResult(output = output)
}

private object HistoryCommand : WorkspaceTerminalCommand {
    override val name: String = "history"
    override val description: String = "显示终端命令历史"
    override val usage: String = "history [limit]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val history = context.history
        if (history.isEmpty()) return TerminalCommandResult(output = "暂无命令历史。")
        val limit = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, history.size) ?: history.size.coerceAtMost(40)
        val entries = history.takeLast(limit)
        val indexWidth = (history.size).toString().length.coerceAtLeast(3)
        val output = entries.mapIndexed { i, entry ->
            val num = history.size - limit + i + 1
            "${num.toString().padStart(indexWidth)}  $entry"
        }.joinToString("\n")
        return TerminalCommandResult(
            output = buildString {
                appendLine("共 ${history.size} 条历史，显示最近 $limit 条：")
                appendLine()
                appendLine(output)
            }.trimEnd()
        )
    }
}

private object RecentCommand : WorkspaceTerminalCommand {
    override val name: String = "recent"
    override val description: String = "显示工作区内最近修改的文件"
    override val usage: String = "recent [limit]"

    override suspend fun execute(
        context: WorkspaceTerminalContext,
        args: List<String>,
        allCommands: List<WorkspaceTerminalCommand>
    ): TerminalCommandResult {
        val limit = args.firstOrNull()?.toIntOrNull()?.coerceIn(1, 80) ?: 20
        val recentFiles = context.rootDirectory.walkTopDown()
            .filter { it.isFile && it.name.isNotBlank() }
            .map { file -> file to file.lastModified() }
            .sortedByDescending { it.second }
            .take(limit)
            .toList()
        if (recentFiles.isEmpty()) return TerminalCommandResult(output = "工作区内暂无文件。")
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val output = recentFiles.joinToString("\n") { (file, modTime) ->
            val relative = file.relativePathFor(context.rootDirectory)
            val modified = dateFormat.format(java.util.Date(modTime))
            "${file.length()}B".padStart(8) + "  " + modified + "  " + relative
        }
        return TerminalCommandResult(
            output = buildString {
                appendLine("最近修改的 ${recentFiles.size} 个文件：")
                appendLine()
                appendLine(output)
            }.trimEnd()
        )
    }
}


private fun WorkspaceTerminalContext.resolveInsideWorkspace(path: String): File {
    val normalized = when {
        path.isBlank() -> currentDirectory
        path.startsWith("/") -> path.removePrefix("/")
        currentDirectory.isBlank() -> path
        else -> "$currentDirectory/$path"
    }
    val target = normalized.split('/')
        .fold(rootDirectory.canonicalFile) { current, segment ->
            when (segment) {
                "", "." -> current
                ".." -> current.parentFile ?: current
                else -> current.resolve(segment)
            }
        }
        .canonicalFile
    require(target.path == rootDirectory.path || target.path.startsWith(rootDirectory.path + File.separator)) {
        "路径越过工作区边界：$path"
    }
    return target
}

private fun WorkspaceTerminalContext.resolveTextFile(path: String): File? {
    val target = resolveInsideWorkspace(path)
    if (!target.exists() || !target.isFile) return null
    if (!target.isLikelyTextFile()) return null
    return target
}

private fun File.isLikelyTextFile(): Boolean {
    if (!exists() || !isFile) return false
    if (length() > MaxTextPreviewBytes) return false
    val sample = inputStream().use { input -> input.readBytes().take(MaxBinaryProbeBytes) }
    return sample.none { byte -> byte == 0.toByte() }
}

private fun File.directorySizeBytes(): Long {
    if (!exists()) return 0L
    if (isFile) return length()
    return walkTopDown()
        .filter { it.isFile }
        .fold(0L) { total, file -> total + file.length() }
}

private fun Long.toHumanSize(): String {
    val units = listOf("B", "KB", "MB", "GB")
    var value = toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (index == 0) "${this}B" else "%.1f%s".format(java.util.Locale.US, value, units[index])
}

private fun File.relativePathFor(root: File): String {
    val canonicalRoot = root.canonicalFile
    val canonicalFile = canonicalFile
    return if (canonicalFile.path == canonicalRoot.path) {
        ""
    } else {
        canonicalRoot.toPath().relativize(canonicalFile.toPath()).toString().replace(File.separatorChar, '/')
    }
}

private const val DefaultPreviewLines = 20
private const val MaxPreviewLines = 200
private const val MaxSearchResults = 80
private const val MaxTextPreviewBytes = 512 * 1024L
private const val MaxBinaryProbeBytes = 4096