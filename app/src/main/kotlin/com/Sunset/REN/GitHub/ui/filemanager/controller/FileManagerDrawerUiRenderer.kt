package com.Sunset.REN.GitHub.ui.filemanager.controller

import android.content.Context
import android.text.TextUtils
import android.widget.LinearLayout
import com.Sunset.REN.GitHub.ui.filemanager.DrawerTab
import android.widget.TextView
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.data.filemanager.FavoriteDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.RecentDirectoryRecord
import com.Sunset.REN.GitHub.data.filemanager.SafDirectoryRecord

/** Renders the compact file-manager drawer rows without owning navigation or dialog behavior. */
class FileManagerDrawerUiRenderer(
    private val context: Context,
    private val container: LinearLayout,
    private val density: Float
) {
    fun render(
        tab: DrawerTab,
        recentDirectories: List<RecentDirectoryRecord>,
        favoriteDirectories: List<FavoriteDirectoryRecord>,
        authorizedDirectories: List<SafDirectoryRecord>,
        onOpenRecent: (RecentDirectoryRecord) -> Unit,
        onRecentLongClick: (RecentDirectoryRecord) -> Unit,
        onOpenFavorite: (FavoriteDirectoryRecord) -> Unit,
        onFavoriteLongClick: (FavoriteDirectoryRecord) -> Unit,
        onOpenAuthorized: (SafDirectoryRecord) -> Unit,
        onAuthorizedLongClick: (SafDirectoryRecord) -> Unit
    ) {
        container.removeAllViews()
        when (tab) {
            DrawerTab.Recent -> renderRecent(recentDirectories, onOpenRecent, onRecentLongClick)
            DrawerTab.Favorite -> renderFavorites(
                favoriteDirectories = favoriteDirectories,
                authorizedDirectories = authorizedDirectories,
                onOpenFavorite = onOpenFavorite,
                onFavoriteLongClick = onFavoriteLongClick,
                onOpenAuthorized = onOpenAuthorized,
                onAuthorizedLongClick = onAuthorizedLongClick
            )
        }
    }

    private fun renderRecent(
        records: List<RecentDirectoryRecord>,
        onOpen: (RecentDirectoryRecord) -> Unit,
        onLongClick: (RecentDirectoryRecord) -> Unit
    ) {
        if (records.isEmpty()) {
            addEmptyRow(context.getString(R.string.local_file_manager_recent_empty))
            return
        }
        records.forEach { record ->
            addRecordRow(
                title = record.directory.label.ifBlank { record.directory.value.substringAfterLast('/') },
                path = record.directory.value,
                onClick = { onOpen(record) },
                onLongClick = { onLongClick(record) }
            )
        }
    }

    private fun renderFavorites(
        favoriteDirectories: List<FavoriteDirectoryRecord>,
        authorizedDirectories: List<SafDirectoryRecord>,
        onOpenFavorite: (FavoriteDirectoryRecord) -> Unit,
        onFavoriteLongClick: (FavoriteDirectoryRecord) -> Unit,
        onOpenAuthorized: (SafDirectoryRecord) -> Unit,
        onAuthorizedLongClick: (SafDirectoryRecord) -> Unit
    ) {
        if (favoriteDirectories.isEmpty() && authorizedDirectories.isEmpty()) {
            addEmptyRow(context.getString(R.string.local_file_manager_favorite_empty_hint))
            return
        }
        if (favoriteDirectories.isNotEmpty()) addSectionTitle(context.getString(R.string.local_file_manager_favorites_title))
        favoriteDirectories.forEach { record ->
            addRecordRow(
                title = record.label.ifBlank { record.value.substringAfterLast('/') },
                path = record.value,
                onClick = { onOpenFavorite(record) },
                onLongClick = { onFavoriteLongClick(record) }
            )
        }
        if (authorizedDirectories.isNotEmpty()) addSectionTitle(context.getString(R.string.local_file_manager_authorized_directory))
        authorizedDirectories.forEach { record ->
            addRecordRow(
                title = record.label.ifBlank { context.getString(R.string.local_file_manager_authorized_directory) },
                path = record.uri.toString(),
                onClick = { onOpenAuthorized(record) },
                onLongClick = { onAuthorizedLongClick(record) }
            )
        }
    }

    private fun addSectionTitle(title: String) {
        container.addView(TextView(context).apply {
            text = title
            setTextColor(0xFF1F6FEB.toInt())
            textSize = 11f
            setPadding(14.dp(), 5.dp(), 10.dp(), 1.dp())
        })
    }

    private fun addEmptyRow(message: String) {
        container.addView(TextView(context).apply {
            text = message
            setTextColor(0xFF777777.toInt())
            textSize = 13f
            setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
        })
    }

    private fun addRecordRow(
        title: String,
        path: String,
        onClick: () -> Unit,
        onLongClick: (() -> Unit)? = null
    ) {
        container.addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14.dp(), 3.dp(), 12.dp(), 3.dp())
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            if (onLongClick != null) {
                setOnLongClickListener {
                    onLongClick()
                    true
                }
            }
            addView(TextView(context).apply {
                text = title
                setTextColor(0xFF111111.toInt())
                textSize = 14f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
            })
            addView(TextView(context).apply {
                text = path
                setTextColor(0xFF777777.toInt())
                textSize = 11f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.MIDDLE
            })
        })
    }

    private fun Int.dp(): Int = (this * density).toInt()
}
