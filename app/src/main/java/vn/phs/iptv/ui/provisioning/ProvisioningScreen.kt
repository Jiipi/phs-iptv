@file:OptIn(ExperimentalTvMaterial3Api::class)

package vn.phs.iptv.ui.provisioning

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import vn.phs.iptv.BuildConfig
import vn.phs.iptv.ui.theme.ApplePillButton
import vn.phs.iptv.ui.theme.PhsAppTheme
import vn.phs.iptv.ui.theme.SkyLinkBlue
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary

// Provisioning (F0) — auto-register → staff assigns room in PHS console → poll until assigned.
@Composable
fun ProvisioningScreen(viewModel: ProvisioningViewModel, onProvisioned: () -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState) { if (uiState is ProvisioningUiState.Assigned) onProvisioned() }

    if (uiState !is ProvisioningUiState.Assigned) {
        PhsAppTheme {
            ProvisioningContent(
                uiState = uiState,
                onRefresh = viewModel::refresh,
                onDebugBypass = if (BuildConfig.DEBUG) onProvisioned else null,
            )
        }
    }
}

@Composable
private fun ProvisioningContent(
    uiState: ProvisioningUiState,
    onRefresh: () -> Unit,
    onDebugBypass: (() -> Unit)? = null,
) {
    val refresh = remember { FocusRequester() }
    LaunchedEffect(uiState) {
        if (uiState is ProvisioningUiState.WaitingForAssignment || uiState is ProvisioningUiState.Error) {
            refresh.requestFocus()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().padding(80.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.widthIn(max = 900.dp),
        ) {
            Text("PHS", style = MaterialTheme.typography.headlineLarge, color = TextSecondary)
            Spacer(Modifier.height(20.dp))
            Text(
                "Kết nối thiết bị · Connect this device",
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(40.dp))

            when (uiState) {
                is ProvisioningUiState.Registering -> {
                    Text("Đang đăng ký… · Registering…", style = MaterialTheme.typography.titleLarge, color = TextSecondary)
                }
                is ProvisioningUiState.WaitingForAssignment -> {
                    Text("MÃ THIẾT BỊ · DEVICE CODE", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        uiState.displayCode,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = TextUnit(96f, TextUnitType.Sp),
                            letterSpacing = TextUnit(8f, TextUnitType.Sp),
                        ),
                        color = SkyLinkBlue,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Nhờ lễ tân gán phòng cho mã này trên PHS Console.\nAsk reception to assign this code to a room.",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("Đang chờ gán phòng… · Waiting for assignment…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
                is ProvisioningUiState.Error -> {
                    Text(
                        uiState.message,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
                is ProvisioningUiState.Assigned -> {}
            }

            Spacer(Modifier.height(44.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ApplePillButton(
                    text = "Làm mới mã · Refresh code",
                    onClick = onRefresh,
                    filled = false,
                    modifier = Modifier.focusRequester(refresh),
                )
                onDebugBypass?.let { bypass ->
                    ApplePillButton(
                        text = "Debug: Bỏ qua",
                        onClick = bypass,
                        filled = false,
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun ProvisioningWaitingPreview() {
    PhsAppTheme {
        ProvisioningContent(ProvisioningUiState.WaitingForAssignment("B2F113"), onRefresh = {})
    }
}
