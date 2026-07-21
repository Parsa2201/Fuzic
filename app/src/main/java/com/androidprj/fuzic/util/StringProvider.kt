package com.androidprj.fuzic.util

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

interface StringProvider {
    fun get(@StringRes resourceId: Int): String
}

class AndroidStringProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : StringProvider {
    override fun get(resourceId: Int): String = context.getString(resourceId)
}
