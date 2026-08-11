package com.mejoresiagratis.lumiai.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mejoresiagratis.lumiai.R
import com.mejoresiagratis.lumiai.ui.theme.LumiMotion
import com.mejoresiagratis.lumiai.ui.theme.LumiSpacing
import com.mejoresiagratis.lumiai.ui.util.isCompactHeight

private data class OnboardingPage(
    @DrawableRes val icon: Int,
    @StringRes val title: Int,
    @StringRes val body: Int
)

private val PAGES = listOf(
    OnboardingPage(R.drawable.ic_mode_continuous, R.string.ob1_title, R.string.ob1_body),
    OnboardingPage(R.drawable.ic_mode_strobe, R.string.ob2_title, R.string.ob2_body),
    OnboardingPage(R.drawable.ic_settings, R.string.ob3_title, R.string.ob3_body)
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    var step by rememberSaveable { mutableIntStateOf(0) }
    val isLast = step == PAGES.lastIndex
    val page = PAGES[step]

    fun finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        viewModel.complete()
        onFinished()
    }

    // Con poca altura (apaisado en móvil) el layout vertical no cabe y los botones quedan
    // FUERA de pantalla: esta pantalla no tiene verticalScroll. En vez de añadir scroll —que
    // esconde el botón principal— reorganizamos a DOS PANELES y aprovechamos el ancho.
    // Es la MISMA composición reordenada: no hay dos layouts que mantener en paralelo.
    val compactHeight = isCompactHeight()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = LumiSpacing.lg)
                .padding(bottom = LumiSpacing.lg)
        ) {
            // Saltar (reservamos altura para que el layout no salte en la última página)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isLast) {
                    TextButton(onClick = { finish() }) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }

            if (compactHeight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LumiSpacing.lg)
                ) {
                    Box(modifier = Modifier.weight(0.4f), contentAlignment = Alignment.Center) {
                        ObIllustration(page = page, size = 96.dp)
                    }
                    Column(
                        modifier = Modifier.weight(0.6f),
                        horizontalAlignment = Alignment.Start
                    ) {
                        ObTexts(page = page, align = TextAlign.Start)
                        ObIndicator(step = step, modifier = Modifier.padding(top = LumiSpacing.md))
                        ObPrimaryButton(isLast = isLast, onClick = { if (isLast) finish() else step++ })
                        ObBackRow(step = step, onBack = { step-- })
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ObIllustration(page = page, size = 120.dp)
                    ObTexts(page = page, align = TextAlign.Center)
                }
                ObIndicator(
                    step = step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = LumiSpacing.lg)
                )
                ObPrimaryButton(isLast = isLast, onClick = { if (isLast) finish() else step++ })
                ObBackRow(step = step, onBack = { step-- })
            }
        }
    }
}

@Composable
private fun ObIllustration(page: OnboardingPage, size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3.33f))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(page.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size / 2f)
        )
    }
}

@Composable
private fun ObTexts(page: OnboardingPage, align: TextAlign) {
    Text(
        text = stringResource(page.title),
        style = MaterialTheme.typography.headlineMedium,
        textAlign = align,
        modifier = Modifier.padding(top = LumiSpacing.xl)
    )
    Text(
        text = stringResource(page.body),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = align,
        modifier = Modifier.padding(top = LumiSpacing.md)
    )
}

@Composable
private fun ObIndicator(step: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PAGES.indices.forEach { i ->
            val active = i == step
            val dotWidth by animateDpAsState(
                targetValue = if (active) 22.dp else 8.dp,
                animationSpec = LumiMotion.emphasized(),
                label = "obDot"
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = LumiSpacing.xs)
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant
                    )
            )
        }
    }
}

@Composable
private fun ObPrimaryButton(isLast: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Text(stringResource(if (isLast) R.string.onboarding_start else R.string.onboarding_next))
    }
}

@Composable
private fun ObBackRow(step: Int, onBack: () -> Unit) {
    // Altura reservada para que el layout no salte entre páginas.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (step > 0) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.onboarding_back))
            }
        }
    }
}
