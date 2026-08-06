package com.Sunset.REN.GitHub.domain.filemanager

import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.util.Base64

object TextFormatConverter {
    enum class Action {
        LineEndingLf,
        LineEndingCrLf,
        LineEndingCr,
        TrimTrailingWhitespace,
        EnsureFinalNewline,
        RemoveFinalNewline,
        TabsToSpaces,
        SpacesToTabs,
        Uppercase,
        Lowercase,
        JsonPretty,
        JsonCompact,
        UrlEncode,
        UrlDecode,
        Base64Encode,
        Base64Decode
    }

    fun convert(content: String, action: Action): Result<String> {
        return runCatching {
            when (action) {
                Action.LineEndingLf -> FileTextEncodingPolicy.normalizeLineEndings(content, FileTextEncodingPolicy.LineEnding.Lf)
                Action.LineEndingCrLf -> FileTextEncodingPolicy.normalizeLineEndings(content, FileTextEncodingPolicy.LineEnding.CrLf)
                Action.LineEndingCr -> FileTextEncodingPolicy.normalizeLineEndings(content, FileTextEncodingPolicy.LineEnding.Cr)
                Action.TrimTrailingWhitespace -> content.lineSequence().joinToString("\n") { it.trimEnd() }
                Action.EnsureFinalNewline -> if (content.endsWith('\n')) content else "$content\n"
                Action.RemoveFinalNewline -> content.trimEnd('\r', '\n')
                Action.TabsToSpaces -> content.replace("\t", "    ")
                Action.SpacesToTabs -> content.replace(Regex(" {4}"), "\t")
                Action.Uppercase -> content.uppercase()
                Action.Lowercase -> content.lowercase()
                Action.JsonPretty -> prettyJson(content)
                Action.JsonCompact -> compactJson(content)
                Action.UrlEncode -> URLEncoder.encode(content, Charsets.UTF_8.name())
                Action.UrlDecode -> URLDecoder.decode(content, Charsets.UTF_8.name())
                Action.Base64Encode -> Base64.getEncoder().encodeToString(content.toByteArray(Charsets.UTF_8))
                Action.Base64Decode -> Base64.getDecoder().decode(content.trim()).toString(Charsets.UTF_8)
            }
        }
    }

    private fun prettyJson(content: String): String {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
            trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
            else -> throw IllegalArgumentException("Not a JSON object or array")
        }
    }

    private fun compactJson(content: String): String {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("{") -> JSONObject(trimmed).toString()
            trimmed.startsWith("[") -> JSONArray(trimmed).toString()
            else -> throw IllegalArgumentException("Not a JSON object or array")
        }
    }
}
