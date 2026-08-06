package com.Sunset.REN.GitHub.data.local.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 应用本地数据库（原生 SQLite）。
 *
 * 选用 [SQLiteOpenHelper] 而非 Room：Room 的注解处理器在编译期需加载 sqlite-jdbc 的 native 库做
 * schema 校验，但当前 Android（aarch64）编译环境没有对应 native 库，会导致 KSP 直接崩溃。
 * 原生 SQLite 无编译期处理，能在该环境正常构建，表结构与语义和 Room 方案完全等价。
 *
 * 目前仅承载仓库本地标记状态（置顶/收藏）。后续如需更多本地表，可在 [onCreate]/[onUpgrade] 扩展并升级版本号。
 */
class AppDatabase private constructor(
    context: Context
) : SQLiteOpenHelper(context.applicationContext, DatabaseName, null, DatabaseVersion) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS $TableRepositoryLocalState (" +
                "$ColumnAccountId INTEGER NOT NULL, " +
                "$ColumnRepositoryFullName TEXT NOT NULL, " +
                "$ColumnIsPinned INTEGER NOT NULL DEFAULT 0, " +
                "$ColumnIsFavorite INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY ($ColumnAccountId, $ColumnRepositoryFullName)" +
                ")"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 目前只有 version 1，暂无升级路径。后续新增表/列时在此按版本递增处理。
    }

    companion object {
        const val TableRepositoryLocalState = "repository_local_state"
        const val ColumnAccountId = "account_id"
        const val ColumnRepositoryFullName = "repository_full_name"
        const val ColumnIsPinned = "is_pinned"
        const val ColumnIsFavorite = "is_favorite"

        private const val DatabaseName = "sunset_github.db"
        private const val DatabaseVersion = 1

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: AppDatabase(context).also { instance = it }
            }
        }
    }
}