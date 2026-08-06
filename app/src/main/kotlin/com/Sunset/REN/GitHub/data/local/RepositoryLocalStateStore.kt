package com.Sunset.REN.GitHub.data.local

import android.content.ContentValues
import android.content.Context
import androidx.core.content.edit
import com.Sunset.REN.GitHub.data.local.db.AppDatabase
import com.Sunset.REN.GitHub.data.local.db.AppDatabase.Companion.ColumnAccountId
import com.Sunset.REN.GitHub.data.local.db.AppDatabase.Companion.ColumnIsFavorite
import com.Sunset.REN.GitHub.data.local.db.AppDatabase.Companion.ColumnIsPinned
import com.Sunset.REN.GitHub.data.local.db.AppDatabase.Companion.ColumnRepositoryFullName
import com.Sunset.REN.GitHub.data.local.db.AppDatabase.Companion.TableRepositoryLocalState
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryLocalState

/**
 * 仓库本地标记状态存储。
 *
 * 数据存储于原生 SQLite（[AppDatabase] 的 repository_local_state 表），置顶与收藏合并为一行的两个布尔列。
 * 首次访问会把历史遗留在 SharedPreferences 里的置顶/收藏集合一次性迁移进数据库，迁移完成后清空旧键，
 * 保证用户既有的标记不丢失。公开 API 与迁移前保持一致，调用方（DashboardViewModel）无需改动。
 *
 * 注意：所有方法均为同步阻塞调用，需由调用方放在 IO 线程执行。
 */
class RepositoryLocalStateStore(context: Context) {

    private val appContext = context.applicationContext
    private val databaseHelper = AppDatabase.getInstance(appContext)

    fun getRepositoryStates(
        accountId: Long,
        repositories: List<GitHubRepository>
    ): Map<String, RepositoryLocalState> {
        migrateLegacyPreferencesIfNeeded(accountId)
        val statesByFullName = queryAccountStates(accountId)
        return repositories.associate { repository ->
            val state = statesByFullName[repository.fullName]
            repository.fullName to RepositoryLocalState(
                isPinned = state?.isPinned == true,
                isFavorite = state?.isFavorite == true
            )
        }
    }

    fun getPinnedRepositoryFullNames(accountId: Long): List<String> {
        migrateLegacyPreferencesIfNeeded(accountId)
        return queryAccountStates(accountId)
            .filterValues { state -> state.isPinned }
            .keys
            .toList()
    }

    fun togglePinned(accountId: Long, repositoryFullName: String): RepositoryLocalState {
        if (repositoryFullName.isBlank()) return getRepositoryState(accountId, repositoryFullName)
        migrateLegacyPreferencesIfNeeded(accountId)
        val current = queryState(accountId, repositoryFullName) ?: RepositoryLocalState()
        val updated = current.copy(isPinned = !current.isPinned)
        persistOrDelete(accountId, repositoryFullName, updated)
        return updated
    }

    fun toggleFavorite(accountId: Long, repositoryFullName: String): RepositoryLocalState {
        if (repositoryFullName.isBlank()) return getRepositoryState(accountId, repositoryFullName)
        migrateLegacyPreferencesIfNeeded(accountId)
        val current = queryState(accountId, repositoryFullName) ?: RepositoryLocalState()
        val updated = current.copy(isFavorite = !current.isFavorite)
        persistOrDelete(accountId, repositoryFullName, updated)
        return updated
    }

    private fun getRepositoryState(accountId: Long, repositoryFullName: String): RepositoryLocalState {
        return queryState(accountId, repositoryFullName) ?: RepositoryLocalState()
    }

    private fun queryAccountStates(accountId: Long): Map<String, RepositoryLocalState> {
        val result = LinkedHashMap<String, RepositoryLocalState>()
        databaseHelper.readableDatabase.query(
            TableRepositoryLocalState,
            arrayOf(ColumnRepositoryFullName, ColumnIsPinned, ColumnIsFavorite),
            "$ColumnAccountId = ?",
            arrayOf(accountId.toString()),
            null,
            null,
            null
        ).use { cursor ->
            val fullNameIndex = cursor.getColumnIndexOrThrow(ColumnRepositoryFullName)
            val pinnedIndex = cursor.getColumnIndexOrThrow(ColumnIsPinned)
            val favoriteIndex = cursor.getColumnIndexOrThrow(ColumnIsFavorite)
            while (cursor.moveToNext()) {
                val fullName = cursor.getString(fullNameIndex)
                result[fullName] = RepositoryLocalState(
                    isPinned = cursor.getInt(pinnedIndex) != 0,
                    isFavorite = cursor.getInt(favoriteIndex) != 0
                )
            }
        }
        return result
    }

    private fun queryState(accountId: Long, repositoryFullName: String): RepositoryLocalState? {
        databaseHelper.readableDatabase.query(
            TableRepositoryLocalState,
            arrayOf(ColumnIsPinned, ColumnIsFavorite),
            "$ColumnAccountId = ? AND $ColumnRepositoryFullName = ?",
            arrayOf(accountId.toString(), repositoryFullName),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            if (!cursor.moveToFirst()) return null
            return RepositoryLocalState(
                isPinned = cursor.getInt(cursor.getColumnIndexOrThrow(ColumnIsPinned)) != 0,
                isFavorite = cursor.getInt(cursor.getColumnIndexOrThrow(ColumnIsFavorite)) != 0
            )
        }
    }

    /**
     * 当一行的置顶与收藏都为 false 时删除该行，避免数据库里堆积无意义的空状态记录；否则插入或覆盖。
     */
    private fun persistOrDelete(
        accountId: Long,
        repositoryFullName: String,
        state: RepositoryLocalState
    ) {
        val database = databaseHelper.writableDatabase
        if (!state.isPinned && !state.isFavorite) {
            database.delete(
                TableRepositoryLocalState,
                "$ColumnAccountId = ? AND $ColumnRepositoryFullName = ?",
                arrayOf(accountId.toString(), repositoryFullName)
            )
            return
        }
        database.insertWithOnConflict(
            TableRepositoryLocalState,
            null,
            buildContentValues(accountId, repositoryFullName, state),
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    private fun buildContentValues(
        accountId: Long,
        repositoryFullName: String,
        state: RepositoryLocalState
    ): ContentValues {
        return ContentValues().apply {
            put(ColumnAccountId, accountId)
            put(ColumnRepositoryFullName, repositoryFullName)
            put(ColumnIsPinned, if (state.isPinned) 1 else 0)
            put(ColumnIsFavorite, if (state.isFavorite) 1 else 0)
        }
    }

    /**
     * 一次性迁移：把旧 SharedPreferences 里该账号的置顶/收藏集合写入数据库，然后清掉旧键。
     * 旧键被清空后该方法会快速返回（读到的集合为空），不会重复迁移。
     */
    private fun migrateLegacyPreferencesIfNeeded(accountId: Long) {
        val legacyPreferences = appContext.getSharedPreferences(
            LegacyPreferencesName,
            Context.MODE_PRIVATE
        )
        val pinnedKey = buildAccountKey(accountId, LegacyKeyPinnedRepositories)
        val favoriteKey = buildAccountKey(accountId, LegacyKeyFavoriteRepositories)
        val pinned = legacyPreferences.getStringSet(pinnedKey, emptySet()).orEmpty()
        val favorite = legacyPreferences.getStringSet(favoriteKey, emptySet()).orEmpty()
        if (pinned.isEmpty() && favorite.isEmpty()) return

        val database = databaseHelper.writableDatabase
        database.beginTransaction()
        try {
            (pinned + favorite).forEach { fullName ->
                database.insertWithOnConflict(
                    TableRepositoryLocalState,
                    null,
                    buildContentValues(
                        accountId,
                        fullName,
                        RepositoryLocalState(
                            isPinned = fullName in pinned,
                            isFavorite = fullName in favorite
                        )
                    ),
                    android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
        legacyPreferences.edit {
            remove(pinnedKey)
            remove(favoriteKey)
        }
    }

    private fun buildAccountKey(accountId: Long, stateKey: String): String {
        return "$accountId:$stateKey"
    }

    private companion object {
        const val LegacyPreferencesName = "repository_local_state_preferences"
        const val LegacyKeyPinnedRepositories = "pinned_repositories"
        const val LegacyKeyFavoriteRepositories = "favorite_repositories"
    }
}