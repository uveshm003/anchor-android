package io.github.uveshm003.anchor.core.data.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Telephony
import android.telecom.TelecomManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Apps that are never blocked: dialer, emergency, launcher, system UI, Anchor, and the SMS app if allowed. */
fun interface EssentialApps {
    suspend fun packages(includeDefaultSms: Boolean): Set<String>
}

internal class AndroidEssentialApps @Inject constructor(@ApplicationContext private val context: Context) :
    EssentialApps {
    override suspend fun packages(includeDefaultSms: Boolean): Set<String> = buildSet {
        addAll(ALWAYS)
        add(context.packageName)
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage?.let(::add)
        homePackage()?.let(::add)
        if (includeDefaultSms) Telephony.Sms.getDefaultSmsPackage(context)?.let(::add)
    }

    private fun homePackage(): String? {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        return context.packageManager.resolveActivity(
            home,
            PackageManager.MATCH_DEFAULT_ONLY,
        )?.activityInfo?.packageName
    }

    private companion object {
        /** System UI, telephony and emergency packages present on stock Android. */
        val ALWAYS = setOf(
            "android",
            "com.android.systemui",
            "com.android.phone",
            "com.android.server.telecom",
            "com.android.emergency",
            "com.google.android.apps.safetyhub",
            "com.android.settings",
        )
    }
}
