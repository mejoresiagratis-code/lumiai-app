package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mejoresiagratis.lumiai.R

@Composable
fun ScreenLight(argb: Int, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val cd = stringResource(R.string.screen_exit_cd)
    Box(
        modifier
            .fillMaxSize()
            .background(Color(argb))
            .clickable(onClick = onTap)
            .semantics { contentDescription = cd }
    )
}
