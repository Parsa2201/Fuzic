package com.androidprj.fuzic.util

import android.content.Context
import androidx.annotation.StringRes
import com.androidprj.fuzic.R
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

fun Throwable.toUserFriendlyMessage(stringProvider: StringProvider, defaultMessageId: Int): String {
    var isNetwork = false
    var current: Throwable? = this
    while (current != null) {
        val name = current.javaClass.simpleName
        if (current is java.net.UnknownHostException || 
            current is java.net.ConnectException ||
            current is java.io.IOException ||
            name.contains("HttpRequestTimeoutException") ||
            name.contains("HttpRequestException") ||
            name.contains("SocketTimeoutException") ||
            name.contains("ConnectException") ||
            name.contains("UnknownHostException")) {
            isNetwork = true
            break
        }
        current = current.cause
    }

    return if (isNetwork) {
        stringProvider.get(R.string.error_no_internet)
    } else {
        message ?: stringProvider.get(defaultMessageId)
    }
}
