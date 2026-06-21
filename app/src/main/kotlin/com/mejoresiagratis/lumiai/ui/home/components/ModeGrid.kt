package com.mejoresiagratis.lumiai.ui.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mejoresiagratis.lumiai.domain.entitlement.Entitlements
import com.mejoresiagratis.lumiai.domain.entitlement.tier
import com.mejoresiagratis.lumiai.domain.flash.isAvailable
import com.mejoresiagratis.lumiai.domain.model.DeviceCapabilities
import com.mejoresiagratis.lumiai.domain.model.FlashMode
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing

@Composable
fun ModeGrid(
    selected: FlashMode,
    onSelect: (FlashMode) -> Unit,
    onLocked: (FlashMode) -> Unit,
    caps: DeviceCapabilities,
    entitlements: Entitlements,
    modifier: Modifier = Modifier
) {
    val available = MODE_CATALOG.filter { it.mode.isAvailable(caps) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LumiSpacing.sm)) {
        available.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LumiSpacing.sm)
            ) {
                rowItems.forEach { item ->
                    val locked = !entitlements.unlocks(item.mode.tier)
                    ModeCard(
                        item = item,
                        selected = item.mode == selected,
                        locked = locked,
                        onClick = { if (locked) onLocked(item.mode) else onSelect(item.mode) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
