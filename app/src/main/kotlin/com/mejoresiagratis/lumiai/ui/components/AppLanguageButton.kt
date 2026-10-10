package com.mejoresiagratis.lumiai.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mejoresiagratis.lumiai.R

/** Opens Android's per-app language picker on Android 13 and newer. */
@Composable
fun AppLanguageButton() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val context = LocalContext.current
        TextButton(
            onClick = {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            }
        ) {
            Text(stringResource(R.string.language_row_title))
        }
    }
}
