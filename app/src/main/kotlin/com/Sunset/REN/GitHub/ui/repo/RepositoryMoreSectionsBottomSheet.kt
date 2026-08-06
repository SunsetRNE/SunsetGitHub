package com.Sunset.REN.GitHub.ui.repo

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.ui.compose.SunsetGitHubTheme

/** 仓库详情「更多」分区弹窗。普通模式用于进入/固定分区，排序模式用于调整当前仓库优先级。 */
class RepositoryMoreSectionsBottomSheet : DialogFragment() {

    interface Host {
        /** 当前已钉进中间快捷区的分区（有序）。 */
        fun currentShortcutSections(): List<RepositorySection>

        /** 当前仓库的抽屉分区排序（有序）。 */
        fun currentSectionOrder(): List<RepositorySection>

        /** 该分区是否已在 App 内实现（true 进页内，false 走 GitHub 网页兜底）。 */
        fun isSectionSupportedInApp(section: RepositorySection): Boolean

        /** 选中分区进行切换。 */
        fun onSectionChosen(section: RepositorySection)

        /** 钉选分区，返回是否成功（达到上限会失败）。 */
        fun onSectionPinned(section: RepositorySection): Boolean

        /** 取消钉选分区，返回是否成功（达到下限会失败）。 */
        fun onSectionUnpinned(section: RepositorySection): Boolean

        /** 提交当前仓库抽屉分区排序，返回是否成功。 */
        fun onSectionOrderChanged(sections: List<RepositorySection>): Boolean
    }

    private val host: Host?
        get() = (parentFragment as? Host) ?: (activity as? Host)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return Dialog(requireContext()).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCanceledOnTouchOutside(true)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val activeHost = host
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                SunsetGitHubTheme {
                    RepositoryMoreSectionsSheetContent(
                        isTablet = isTabletLayout(),
                        sections = activeHost?.currentSectionOrder().orEmpty(),
                        pinnedSections = activeHost?.currentShortcutSections().orEmpty(),
                        isSectionSupported = { section -> activeHost?.isSectionSupportedInApp(section) == true },
                        onSectionChosen = { section ->
                            activeHost?.onSectionChosen(section)
                            dismiss()
                        },
                        onSectionPinned = { section -> activeHost?.onSectionPinned(section) == true },
                        onSectionUnpinned = { section -> activeHost?.onSectionUnpinned(section) == true },
                        onSectionOrderChanged = { sections -> activeHost?.onSectionOrderChanged(sections) == true }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isTabletLayout()) configureTabletSideDialog() else configureBottomDialog()
    }

    private fun isTabletLayout(): Boolean {
        return resources.configuration.smallestScreenWidthDp >= TabletMinWidthDp
    }

    private fun configureBottomDialog() {
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.setPadding(0, 0, 0, 0)
            setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            attributes = attributes.apply {
                width = ViewGroup.LayoutParams.MATCH_PARENT
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        }
    }

    private fun configureTabletSideDialog() {
        val targetWidth = minOf(
            resources.displayMetrics.widthPixels - dpToPx(TabletDrawerSideMarginDp) * 2,
            dpToPx(TabletDrawerMaxWidthDp)
        ).coerceAtLeast(dpToPx(TabletDrawerMinWidthDp))
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            decorView.setPadding(0, 0, 0, 0)
            setDimAmount(TabletDialogDimAmount)
            setGravity(Gravity.END or Gravity.TOP)
            setLayout(targetWidth, ViewGroup.LayoutParams.MATCH_PARENT)
            attributes = attributes.apply {
                width = targetWidth
                height = ViewGroup.LayoutParams.MATCH_PARENT
                gravity = Gravity.END or Gravity.TOP
                dimAmount = TabletDialogDimAmount
            }
        }
    }

    private fun dpToPx(valueDp: Int): Int {
        return (valueDp * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val FRAGMENT_TAG = "RepositoryMoreSectionsBottomSheet"

        private const val TabletMinWidthDp = 600
        private const val TabletDrawerMinWidthDp = 360
        private const val TabletDrawerMaxWidthDp = 420
        private const val TabletDrawerSideMarginDp = 24
        private const val TabletDialogDimAmount = 0.32f

        fun newInstance(): RepositoryMoreSectionsBottomSheet {
            return RepositoryMoreSectionsBottomSheet()
        }
    }
}

@Composable
private fun RepositoryMoreSectionsSheetContent(
    isTablet: Boolean,
    sections: List<RepositorySection>,
    pinnedSections: List<RepositorySection>,
    isSectionSupported: (RepositorySection) -> Boolean,
    onSectionChosen: (RepositorySection) -> Unit,
    onSectionPinned: (RepositorySection) -> Boolean,
    onSectionUnpinned: (RepositorySection) -> Boolean,
    onSectionOrderChanged: (List<RepositorySection>) -> Boolean
) {
    var isSorting by remember { mutableStateOf(false) }
    val orderedSections = remember(sections) {
        mutableStateListOf<RepositorySection>().apply { addAll(sections) }
    }
    var pinnedSet by remember(pinnedSections) { mutableStateOf(pinnedSections.toSet()) }

    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isTablet) Modifier.fillMaxHeight() else Modifier)
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (isTablet) 24.dp else 20.dp,
                vertical = if (isTablet) 24.dp else 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.repository_section_more),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(
                    if (isSorting) R.string.repository_section_more_sheet_sort_description
                    else R.string.repository_section_more_sheet_description
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = { isSorting = !isSorting }) {
                Text(
                    text = stringResource(
                        if (isSorting) R.string.repository_section_sort_done
                        else R.string.repository_section_sort_action
                    )
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = if (isTablet) 720.dp else 520.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                itemsIndexed(orderedSections, key = { _, section -> section.storageKey }) { index, section ->
                    RepositoryMoreSectionRow(
                        section = section,
                        index = index,
                        isPinned = section in pinnedSet,
                        isSupported = isSectionSupported(section),
                        isSorting = isSorting,
                        canMoveUp = index > 0,
                        canMoveDown = index < orderedSections.lastIndex,
                        onClick = { if (!isSorting) onSectionChosen(section) },
                        onTogglePinned = {
                            val changed = if (section in pinnedSet) {
                                onSectionUnpinned(section)
                            } else {
                                onSectionPinned(section)
                            }
                            if (changed) {
                                pinnedSet = if (section in pinnedSet) pinnedSet - section else pinnedSet + section
                            }
                        },
                        onMoveUp = {
                            if (moveSection(orderedSections, index, index - 1)) {
                                onSectionOrderChanged(orderedSections.toList())
                            }
                        },
                        onMoveDown = {
                            if (moveSection(orderedSections, index, index + 1)) {
                                onSectionOrderChanged(orderedSections.toList())
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryMoreSectionRow(
    section: RepositorySection,
    index: Int,
    isPinned: Boolean,
    isSupported: Boolean,
    isSorting: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onTogglePinned: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isSorting, onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(section.navigationIconResId),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(section.titleResId),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (isSorting) {
                    stringResource(R.string.repository_section_drag_status, stringResource(section.titleResId), index + 1)
                } else if (isSupported) {
                    stringResource(R.string.repository_section_status_supported)
                } else {
                    stringResource(R.string.repository_section_status_web_fallback)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSorting) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Button(enabled = canMoveUp, onClick = onMoveUp) { Text(text = "上移") }
                Button(enabled = canMoveDown, onClick = onMoveDown) { Text(text = "下移") }
            }
        } else {
            IconButton(onClick = onTogglePinned) {
                Icon(
                    painter = painterResource(if (isPinned) R.drawable.ic_pin_filled_24 else R.drawable.ic_pin_outline_24),
                    contentDescription = stringResource(
                        if (isPinned) R.string.repository_section_unpin_content_description
                        else R.string.repository_section_pin_content_description
                    ),
                    tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun moveSection(
    sections: MutableList<RepositorySection>,
    fromPosition: Int,
    toPosition: Int
): Boolean {
    if (fromPosition !in sections.indices || toPosition !in sections.indices || fromPosition == toPosition) return false
    val section = sections.removeAt(fromPosition)
    sections.add(toPosition, section)
    return true
}