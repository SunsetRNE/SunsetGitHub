package com.Sunset.REN.GitHub.domain.filemanager

object FileEntryTypeResolver {
    @Deprecated(
        message = "Use FileContentAccessPolicy.inlineTextTypes instead.",
        replaceWith = ReplaceWith("FileContentAccessPolicy.inlineTextTypes")
    )
    val editableTypes = FileContentAccessPolicy.inlineTextTypes

    fun resolve(
        name: String,
        mimeType: String? = null,
        isDirectory: Boolean = false,
        sampleBytes: ByteArray? = null
    ): FileEntryType {
        if (isDirectory) return FileEntryType.Directory

        val normalizedName = name.substringAfterLast('/').lowercase()
        val extension = normalizedName.substringAfterLast('.', missingDelimiterValue = "")
        val normalizedMimeType = mimeType.orEmpty().lowercase()

        val byMimeType = resolveMimeType(normalizedMimeType, normalizedName)
        if (byMimeType != null && byMimeType != FileEntryType.Unknown) return byMimeType

        val byName = resolveName(normalizedName, extension)
        if (byName != null && byName != FileEntryType.Unknown) return byName

        val bySignature = sampleBytes?.let { FileSignatureSniffer.sniff(it, name, mimeType) }
        return bySignature ?: FileEntryType.Unknown
    }

    fun resolveVerified(
        name: String,
        mimeType: String? = null,
        isDirectory: Boolean = false,
        sampleBytes: ByteArray? = null
    ): FileEntryType {
        val declaredType = resolve(
            name = name,
            mimeType = mimeType,
            isDirectory = isDirectory,
            sampleBytes = null
        )
        if (isDirectory || sampleBytes == null || sampleBytes.isEmpty()) return declaredType
        val sniffedType = FileSignatureSniffer.sniff(sampleBytes, name, mimeType) ?: return declaredType
        return when {
            declaredType == FileEntryType.Unknown -> sniffedType
            declaredType in FileContentAccessPolicy.inlineTextTypes && sniffedType !in FileContentAccessPolicy.inlineTextTypes -> sniffedType
            else -> declaredType
        }
    }

    private fun resolveMimeType(mimeType: String, name: String): FileEntryType? {
        if (mimeType.isBlank()) return null
        return when {
            mimeType.startsWith("text/") -> textLikeTypeForName(name)
            mimeType.startsWith("image/") -> FileEntryType.Image
            mimeType == "application/json" || mimeType.endsWith("+json") -> FileEntryType.Text
            mimeType == "application/xml" || mimeType.endsWith("+xml") -> FileEntryType.Code
            mimeType == "application/pdf" -> FileEntryType.Binary
            mimeType == "application/vnd.android.package-archive" -> FileEntryType.Apk
            ArchiveFormatResolver.resolve(name, mimeType) != null -> if (name.endsWith(".apk")) FileEntryType.Apk else FileEntryType.Archive
            mimeType in binaryMimeTypes -> FileEntryType.Binary
            else -> null
        }
    }

    private fun resolveName(name: String, extension: String): FileEntryType? {
        return when {
            name in markdownFileNames -> FileEntryType.Markdown
            name in codeFileNames -> FileEntryType.Code
            name in textFileNames -> FileEntryType.Text
            extension in markdownExtensions -> FileEntryType.Markdown
            extension in codeExtensions -> FileEntryType.Code
            extension in textExtensions -> FileEntryType.Text
            extension in imageExtensions -> FileEntryType.Image
            extension == "apk" -> FileEntryType.Apk
            ArchiveFormatResolver.resolve(name) != null -> FileEntryType.Archive
            extension in binaryExtensions -> FileEntryType.Binary
            else -> null
        }
    }

    private fun textLikeTypeForName(name: String): FileEntryType {
        val extension = name.substringAfterLast('.', missingDelimiterValue = "")
        return when {
            name in markdownFileNames || extension in markdownExtensions -> FileEntryType.Markdown
            name in codeFileNames || extension in codeExtensions -> FileEntryType.Code
            else -> FileEntryType.Text
        }
    }

    private val markdownFileNames = setOf("readme", "readme.md", "readme.markdown", "changelog", "changelog.md", "changes", "news")
    private val codeFileNames = setOf(
        "dockerfile", "containerfile", "makefile", "gnumakefile", "rakefile", "gemfile", "podfile",
        "justfile", "vagrantfile", "brewfile", "procfile", "earthfile", "fastfile", "appfile", "matchfile"
    )
    private val textFileNames = setOf(
        "license", "licence", "notice", "authors", "contributors", "copying", "install", "thanks",
        ".gitignore", ".gitattributes", ".gitmodules", ".editorconfig", ".env", ".envrc", ".npmrc",
        ".yarnrc", ".dockerignore", ".eslintignore", ".prettierignore", ".prettierrc", ".babelrc",
        ".watchmanconfig", ".buckconfig", ".curlrc", ".wgetrc", ".netrc"
    )

    private val markdownExtensions = setOf("md", "markdown", "mdown", "mkdn", "mkd", "mdwn", "mdtxt", "mdtext", "rst", "adoc", "asciidoc", "asc", "org", "texi", "textile")
    private val codeExtensions = setOf(
        "kt", "kts", "java", "xml", "html", "htm", "xhtml", "css", "scss", "sass", "less",
        "js", "mjs", "cjs", "ts", "tsx", "jsx", "mts", "cts", "vue", "svelte", "astro",
        "sh", "bash", "zsh", "fish", "ps1", "psm1", "bat", "cmd", "awk", "sed",
        "c", "cc", "cpp", "cxx", "h", "hh", "hpp", "hxx", "m", "mm",
        "py", "pyw", "pyi", "rb", "rake", "gemspec", "go", "rs", "swift", "sql",
        "cs", "fs", "fsx", "fsi", "vb", "scala", "sc", "groovy", "gvy", "gradle",
        "lua", "php", "phtml", "pl", "pm", "r", "R", "jl", "dart", "ex", "exs", "erl", "hrl",
        "clj", "cljs", "cljc", "edn", "hs", "lhs", "ml", "mli", "nim", "zig", "v", "sv", "svh",
        "proto", "graphql", "gql", "prisma", "twig", "liquid", "mustache", "hbs", "handlebars", "ejs",
        "jinja", "j2", "ftl", "vm", "cmake", "mk", "make", "ninja", "dockerfile"
    )
    private val textExtensions = setOf(
        "txt", "text", "log", "out", "err", "trace", "dump", "stacktrace",
        "json", "jsonl", "ndjson", "geojson", "topojson", "json5", "hjson",
        "yaml", "yml", "toml", "ini", "cfg", "conf", "config", "cnf", "properties", "props",
        "env", "dotenv", "example", "sample", "template", "tmpl", "tpl", "dist",
        "csv", "tsv", "psv", "ssv", "dsv", "tab", "list", "lst", "manifest", "mf",
        "lock", "sum", "mod", "work", "patch", "diff", "rej",
        "pem", "crt", "cer", "csr", "key", "pub", "asc", "sig", "sha1", "sha256", "sha512", "md5",
        "url", "uri", "ics", "vcf", "srt", "vtt", "ass", "ssa", "po", "pot", "strings"
    )
    private val imageExtensions = setOf("png", "jpg", "jpeg", "gif", "webp", "bmp", "svg", "heic", "heif", "avif", "ico", "tif", "tiff")
    private val binaryExtensions = setOf(
        "bin", "so", "dex", "class", "exe", "dll", "o", "a", "pdf", "wasm", "db", "sqlite", "sqlite3",
        "doc", "xls", "ppt", "mp3", "m4a", "aac", "flac", "ogg", "wav", "mp4", "m4v", "mkv", "webm", "avi", "mov",
        "ttf", "otf", "woff", "woff2"
    )
    private val archiveMimeTypes = setOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/java-archive",
        "application/x-tar",
        "application/gzip",
        "application/x-gzip",
        "application/x-7z-compressed",
        "application/vnd.rar"
    )
    private val binaryMimeTypes = setOf("application/octet-stream", "application/pdf")
}