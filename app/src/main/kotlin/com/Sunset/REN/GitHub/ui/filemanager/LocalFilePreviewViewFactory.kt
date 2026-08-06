package com.Sunset.REN.GitHub.ui.filemanager

import android.content.Context
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.Sunset.REN.GitHub.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox

object LocalFilePreviewViewFactory {
    fun create(context: Context): View {
        val root = ConstraintLayout(context).apply {
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(R.color.github_canvas)
        }

        val topBar = createTopBar(context)
        val editor = createEditorContainer(context)
        val search = createSearchPanel(context)
        val actions = createActionsBar(context)

        root.addView(topBar)
        root.addView(editor)
        root.addView(search)
        root.addView(actions)

        topBar.layoutParams = ConstraintLayout.LayoutParams(0, dp(context, 30)).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToTop = ConstraintLayout.LayoutParams.PARENT_ID
        }
        editor.layoutParams = ConstraintLayout.LayoutParams(0, 0).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            topToBottom = R.id.container_local_file_preview_top_bar
            bottomToTop = R.id.container_local_file_preview_search
        }
        search.layoutParams = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToTop = R.id.container_local_file_preview_actions
        }
        actions.layoutParams = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT).apply {
            startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
        }
        return root
    }

    private fun createTopBar(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            id = R.id.container_local_file_preview_top_bar
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 8), 0, dp(context, 8), 0)
            setBackgroundResource(R.drawable.bg_file_preview_top_bar)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                )
                addView(TextView(context).apply {
                    id = R.id.text_local_file_preview_name
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    includeFontPadding = false
                    setTextAppearance(R.style.TextAppearance_SunsetGitHub_Material_Body)
                    setTextColor(context.getColor(R.color.github_file_preview_top_bar_text_primary))
                    textSize = 13f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(context).apply {
                    id = R.id.text_local_file_preview_state
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    includeFontPadding = false
                    setTextAppearance(R.style.TextAppearance_SunsetGitHub_Material_Body)
                    setTextColor(context.getColor(R.color.github_file_preview_top_bar_text_secondary))
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                })
            })
            addView(hiddenText(context, R.id.text_local_file_preview_path))
            addView(hiddenText(context, R.id.text_local_file_preview_type_chip))
            addView(hiddenText(context, R.id.text_local_file_preview_access_pill))
        }
    }

    private fun createEditorContainer(context: Context): FrameLayout {
        return FrameLayout(context).apply {
            id = R.id.container_local_file_preview_editor
            setBackgroundResource(R.drawable.bg_file_preview_code)
            addView(ScrollView(context).apply {
                id = R.id.scroll_local_file_preview_markdown
                visibility = View.GONE
                isFillViewport = true
                setPadding(dimen(context, R.dimen.page_horizontal_padding), dimen(context, R.dimen.space_3), dimen(context, R.dimen.page_horizontal_padding), dimen(context, R.dimen.space_3))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                addView(TextView(context).apply {
                    id = R.id.text_local_file_preview_markdown
                    setTextAppearance(R.style.TextAppearance_SunsetGitHub_Material_Body)
                    setTextColor(context.getColor(R.color.github_text_primary))
                    setLineSpacing(dp(context, 4).toFloat(), 1f)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                })
            })
            addView(FrameLayout(context).apply {
                id = R.id.container_local_file_preview_image
                visibility = View.GONE
                setPadding(dimen(context, R.dimen.page_horizontal_padding), dimen(context, R.dimen.page_horizontal_padding), dimen(context, R.dimen.page_horizontal_padding), dimen(context, R.dimen.page_horizontal_padding))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                addView(ImageView(context).apply {
                    id = R.id.image_local_file_preview
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    contentDescription = context.getString(R.string.local_file_preview_image_content_description)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                })
            })
            addView(ScrollView(context).apply {
                id = R.id.scroll_local_file_preview_archive
                visibility = View.GONE
                isFillViewport = true
                setPadding(dimen(context, R.dimen.page_horizontal_padding), dimen(context, R.dimen.space_3), dimen(context, R.dimen.page_horizontal_padding), dimen(context, R.dimen.space_3))
                layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                addView(TextView(context).apply {
                    id = R.id.text_local_file_preview_archive
                    typeface = android.graphics.Typeface.MONOSPACE
                    setTextAppearance(R.style.TextAppearance_SunsetGitHub_Material_Body)
                    setTextColor(context.getColor(R.color.github_text_primary))
                    setLineSpacing(dp(context, 4).toFloat(), 1f)
                    layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT)
                })
            })
        }
    }

    private fun createSearchPanel(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            id = R.id.container_local_file_preview_search
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setBackgroundResource(R.drawable.bg_file_preview_panel)
            setPadding(dimen(context, R.dimen.space_2), dimen(context, R.dimen.space_1), dimen(context, R.dimen.space_2), dimen(context, R.dimen.space_1))
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                addView(searchRow(context))
                addView(replaceRow(context))
                addView(optionsRow(context))
            })
        }
    }

    private fun searchRow(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        addView(searchEditText(context, R.id.edit_local_file_preview_search_query, R.string.local_file_preview_search_hint, android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH).apply {
            layoutParams = LinearLayout.LayoutParams(0, dimen(context, R.dimen.local_file_preview_search_field_height), 1f)
        })
        addView(iconButton(context, R.id.button_local_file_preview_find_previous, R.string.local_file_preview_find_previous_short, R.string.local_file_preview_find_previous))
        addView(iconButton(context, R.id.button_local_file_preview_find_next, R.string.local_file_preview_find_next_short, R.string.local_file_preview_find_next))
        addView(iconButton(context, R.id.button_local_file_preview_regex_help, R.string.local_file_preview_regex_help_short, R.string.local_file_preview_regex_help_title))
    }

    private fun replaceRow(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dimen(context, R.dimen.space_1) }
        addView(searchEditText(context, R.id.edit_local_file_preview_replace_text, R.string.local_file_preview_replace_hint, android.view.inputmethod.EditorInfo.IME_ACTION_DONE).apply {
            layoutParams = LinearLayout.LayoutParams(0, dimen(context, R.dimen.local_file_preview_search_field_height), 1f)
        })
        addView(actionButton(context, R.id.button_local_file_preview_replace_current, R.string.local_file_preview_replace_current).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dimen(context, R.dimen.local_file_preview_search_action_button_height)).apply { marginStart = dimen(context, R.dimen.space_1) }
        })
        addView(actionButton(context, R.id.button_local_file_preview_replace_all, R.string.local_file_preview_replace_all).apply {
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dimen(context, R.dimen.local_file_preview_search_action_button_height)).apply { marginStart = dimen(context, R.dimen.space_1) }
        })
    }

    private fun optionsRow(context: Context): LinearLayout = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = dimen(context, R.dimen.space_1) }
        addView(MaterialCheckBox(context).apply {
            id = R.id.check_local_file_preview_ignore_case
            text = context.getString(R.string.local_file_preview_search_ignore_case)
            setTextColor(context.getColor(R.color.github_text_secondary))
            textSize = 12f
            minHeight = 0
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dimen(context, R.dimen.local_file_preview_search_option_height))
        })
        addView(MaterialCheckBox(context).apply {
            id = R.id.check_local_file_preview_regex
            text = context.getString(R.string.local_file_preview_search_regex)
            setTextColor(context.getColor(R.color.github_text_secondary))
            textSize = 12f
            minHeight = 0
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dimen(context, R.dimen.local_file_preview_search_option_height)).apply { marginStart = dimen(context, R.dimen.space_2) }
        })
        addView(TextView(context).apply {
            id = R.id.text_local_file_preview_search_status
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setTextAppearance(R.style.TextAppearance_SunsetGitHub_Material_Meta)
            setTextColor(context.getColor(R.color.github_text_muted))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dimen(context, R.dimen.space_2) }
        })
    }

    private fun createActionsBar(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            id = R.id.container_local_file_preview_actions
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_file_preview_actions)
            setPadding(dimen(context, R.dimen.space_1), dp(context, 2), dimen(context, R.dimen.space_1), dp(context, 2))
            actionSpecs().forEachIndexed { index, spec ->
                addView(actionButton(context, spec.id, spec.textRes).apply {
                    visibility = if (spec.initiallyVisible) View.VISIBLE else View.GONE
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                        if (index > 0) marginStart = dimen(context, R.dimen.space_2)
                    }
                })
            }
        }
    }

    private fun searchEditText(context: Context, idValue: Int, hintRes: Int, ime: Int): EditText = EditText(context).apply {
        id = idValue
        hint = context.getString(hintRes)
        setSingleLine(true)
        maxLines = 1
        imeOptions = ime
        inputType = InputType.TYPE_CLASS_TEXT
        setBackgroundResource(R.drawable.bg_search_inline)
        setPadding(dp(context, 12), 0, dp(context, 12), 0)
        setTextColor(context.getColor(R.color.github_text_primary))
        setHintTextColor(context.getColor(R.color.github_text_muted))
        textSize = 13f
    }

    private fun iconButton(context: Context, idValue: Int, textRes: Int, contentDescriptionRes: Int): MaterialButton {
        return actionButton(context, idValue, textRes).apply {
            contentDescription = context.getString(contentDescriptionRes)
            layoutParams = LinearLayout.LayoutParams(
                dimen(context, R.dimen.local_file_preview_search_icon_button_size),
                dimen(context, R.dimen.local_file_preview_search_icon_button_size)
            ).apply { marginStart = dimen(context, R.dimen.space_1) }
        }
    }

    private fun actionButton(context: Context, idValue: Int, textRes: Int): MaterialButton = MaterialButton(context).apply {
        id = idValue
        text = context.getString(textRes)
        isAllCaps = false
        minWidth = 0
        minHeight = 0
        insetTop = 0
        insetBottom = 0
        textSize = 12f
        setPadding(dp(context, 8), 0, dp(context, 8), 0)
    }

    private fun hiddenText(context: Context, idValue: Int): TextView = TextView(context).apply {
        id = idValue
        visibility = View.GONE
    }

    private fun actionSpecs(): List<ActionSpec> = listOf(
        ActionSpec(R.id.button_local_file_preview_markdown_toggle, R.string.local_file_preview_markdown_source, false),
        ActionSpec(R.id.button_local_file_preview_search_toggle, R.string.local_file_preview_search, true),
        ActionSpec(R.id.button_local_file_preview_convert, R.string.local_file_preview_convert, true),
        ActionSpec(R.id.button_local_file_preview_save_as, R.string.local_file_preview_save_as, false),
        ActionSpec(R.id.button_local_file_preview_extract, R.string.local_file_preview_archive_extract, false),
        ActionSpec(R.id.button_local_file_preview_edit, R.string.local_file_preview_edit, true),
        ActionSpec(R.id.button_local_file_preview_undo, R.string.local_file_preview_undo, false),
        ActionSpec(R.id.button_local_file_preview_redo, R.string.local_file_preview_redo, false),
        ActionSpec(R.id.button_local_file_preview_cancel, R.string.cancel, false),
        ActionSpec(R.id.button_local_file_preview_save, R.string.local_file_preview_save, false)
    )

    private data class ActionSpec(val id: Int, val textRes: Int, val initiallyVisible: Boolean)

    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
    private fun dimen(context: Context, resId: Int): Int = context.resources.getDimensionPixelSize(resId)
}
