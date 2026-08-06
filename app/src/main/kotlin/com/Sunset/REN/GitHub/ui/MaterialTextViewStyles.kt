package com.Sunset.REN.GitHub.ui

import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.Sunset.REN.GitHub.R

fun TextView.applyMaterialRepositoryNameStyle() {
    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_SunsetGitHub_Material_RepositoryName)
}

fun TextView.applyMaterialBodyStyle() {
    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_SunsetGitHub_Material_Body)
}

fun TextView.applyMaterialMetaStyle() {
    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_SunsetGitHub_Material_Meta)
}
