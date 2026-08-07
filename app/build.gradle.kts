import java.util.Properties
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { stream: java.io.InputStream ->
        localProperties.load(stream)
    }
}
// 优先级：CI 环境变量（GitHub Actions secret）> local.properties > 空。
// CI runner 上没有 local.properties，必须经 OAUTH_CLIENT_ID secret 注入，
// 否则产物 APK 的设备码登录（Device Flow）会因缺少 client_id 无法工作。
val githubOAuthClientId = (System.getenv("OAUTH_CLIENT_ID")
    ?: localProperties.getProperty("github.oauth.client.id", "")).trim()

fun localProperty(name: String): String = localProperties.getProperty(name, "").trim()

val releaseStoreFilePath = localProperty("release.store.file")
val releaseStorePassword = localProperty("release.store.password")
val releaseKeyAlias = localProperty("release.key.alias")
val releaseKeyPassword = localProperty("release.key.password")
val releaseSigningValues = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
)
val hasCompleteReleaseSigningConfig = releaseSigningValues.all { it.isNotBlank() }
val hasPartialReleaseSigningConfig = releaseSigningValues.any { it.isNotBlank() } && !hasCompleteReleaseSigningConfig

if (hasPartialReleaseSigningConfig) {
    throw GradleException(
        "Incomplete release signing configuration. Please set release.store.file, " +
            "release.store.password, release.key.alias, and release.key.password in local.properties."
    )
}

android {
    namespace = "com.Sunset.REN.GitHub"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.Sunset.REN.GitHub"
        minSdk = 28
        targetSdk = 35
        versionCode = 308
        versionName = "1.0.0-2026-7-29（1）"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GITHUB_OAUTH_CLIENT_ID", "\"$githubOAuthClientId\"")
    }

    signingConfigs {
        if (hasCompleteReleaseSigningConfig) {
            create("release") {
                val releaseStoreFile = rootProject.file(releaseStoreFilePath)
                if (!releaseStoreFile.exists()) {
                    throw GradleException("Release signing store file not found: ${releaseStoreFile.absolutePath}")
                }
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = false
            }
        }
    }

    buildTypes {
        release {
            if (hasCompleteReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.register("checkStringResources") {
    group = "verification"
    description = "Verify string resources follow the shared-main plus small variant-overrides policy."

    doLast {
        val mainResDir = file("src/main/res")
        val debugResDir = file("src/debug/res")
        val failures = mutableListOf<String>()

        val forbiddenAutoMissingFiles = listOf(mainResDir, debugResDir)
            .filter { it.exists() }
            .flatMap { resDir ->
                resDir.walkTopDown()
                    .filter { it.isFile && it.name == "strings_auto_missing.xml" }
                    .toList()
            }
        forbiddenAutoMissingFiles.forEach { file ->
            failures += "Auto-generated placeholder string file is not allowed: ${file.relativeTo(projectDir)}"
        }

        val suspiciousTokenRegex = Regex(
            pattern = """(^|\s)(标题|说明|操作|暂无内容|count|desc|loading|retry|section|with|unknown|missing|native|stub|generic|placeholder|button|toast|content|description)(\s|$)""",
            option = RegexOption.IGNORE_CASE
        )
        val suspiciousChinesePlaceholderRegex = Regex(
            pattern = """(检查检查|标题GitHub|暂无(认证令牌检查|\S*数量|\S*权限范围)|账号(分组|管理|条目|标题|登录信息)\S*(说明|状态|类型|数量)|认证令牌\S*(说明|缺少|等待|风险|保存中|重新生成)|通知(原因|类型|详情)\S+|\blinks\b)""",
            option = RegexOption.IGNORE_CASE
        )
        val stringEntryRegex = Regex(
            pattern = """<string\b[^>]*\bname=\"([^\"]+)\"[^>]*>(.*?)</string>""",
            options = setOf(RegexOption.DOT_MATCHES_ALL)
        )
        val tagRegex = Regex("<[^>]+>")

        fun stringEntriesUnder(resDir: File): Map<String, List<Pair<File, String>>> {
            if (!resDir.exists()) return emptyMap()
            return resDir.walkTopDown()
                .filter { file ->
                    file.isFile &&
                        file.extension == "xml" &&
                        file.parentFile?.name?.startsWith("values") == true
                }
                .flatMap { file ->
                    val content = file.readText()
                    stringEntryRegex.findAll(content).map { match ->
                        val name = match.groupValues[1]
                        val value = match.groupValues[2]
                            .replace(tagRegex, "")
                            .replace(Regex("\\s+"), " ")
                            .trim()
                        name to (file to value)
                    }
                }
                .groupBy(keySelector = { it.first }, valueTransform = { it.second })
        }

        val mainStringEntries = stringEntriesUnder(mainResDir)
        val debugStringEntries = stringEntriesUnder(debugResDir)

        mainStringEntries.values.flatten().forEach { (file, value) ->
            val content = file.readText()
            if (content.contains("兜底字符串") || content.contains("strings_auto_missing")) {
                failures += "Auto-missing marker found in main resource: ${file.relativeTo(projectDir)}"
            }
            if (suspiciousTokenRegex.containsMatchIn(value) || suspiciousChinesePlaceholderRegex.containsMatchIn(value)) {
                val name = mainStringEntries.entries.firstOrNull { entry -> entry.value.any { it.first == file && it.second == value } }?.key.orEmpty()
                failures += "Placeholder-like string in ${file.relativeTo(projectDir)}: $name=\"$value\""
            }
        }

        val allowedDebugOverrides = emptySet<String>()
        val illegalDebugOverrides = debugStringEntries.keys
            .intersect(mainStringEntries.keys)
            .filterNot { name ->
                name in allowedDebugOverrides ||
                    name.startsWith("debug_") ||
                    name.startsWith("dev_")
            }
            .sorted()
        illegalDebugOverrides.forEach { name ->
            val locations = debugStringEntries.getValue(name)
                .joinToString { (file, _) -> file.relativeTo(projectDir).path }
            failures += "Debug string overrides shared UI string '$name' in $locations. Move common copy to src/main or add an explicit whitelist entry."
        }

        if (failures.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("String resource check failed.")
                    appendLine("A-scheme policy: src/main holds shared formal copy; src/debug only holds small intentional dev-only overrides.")
                    failures.forEach { appendLine(" - $it") }
                }
            )
        }
    }
}

tasks.register("makeReleaseApkShareable") {
    group = "build"
    description = "Make the release APK readable by workspace sharing tools."

    doLast {
        val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk").get().asFile
        if (releaseApk.exists()) {
            releaseApk.setReadable(true, false)
            releaseApk.setWritable(true, true)
            logger.lifecycle("Release APK is shareable: ${releaseApk.absolutePath}")
        } else {
            logger.warn("Release APK not found, cannot update permissions: ${releaseApk.absolutePath}")
        }
    }
}

afterEvaluate {
    tasks.named("assembleRelease") {
        dependsOn("checkStringResources")
        finalizedBy("makeReleaseApkShareable")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.vectordrawable)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.markwon.core)
    implementation("net.java.dev.jna:jna:${libs.versions.jna.get()}@aar") // UniFFI 绑定 JNA 运行时（Android AAR）
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.tasklist)
    implementation(libs.markwon.ext.strikethrough)
    implementation(libs.markwon.html)
    implementation(libs.markwon.image)
    implementation(libs.markwon.image.glide)
    implementation(libs.markwon.linkify)
    implementation(libs.glide)
    implementation(libs.androidsvg)
    implementation(libs.commons.compress)
    implementation(libs.pdfbox.android)
    annotationProcessor(libs.glide.compiler)
    implementation(platform(libs.sora.editor.bom))
    implementation(libs.sora.editor)
    implementation(libs.sora.language.java)
    implementation(libs.sora.language.textmate)

    testImplementation(libs.junit)
}