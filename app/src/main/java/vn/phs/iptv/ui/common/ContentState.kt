package vn.phs.iptv.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import vn.phs.iptv.ui.theme.TextPrimary
import vn.phs.iptv.ui.theme.TextSecondary

@Composable
fun ContentStateMessage(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = MaterialTheme.typography.displaySmall, color = TextPrimary)
        body?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.titleLarge, color = TextSecondary)
        }
        action?.invoke()
    }
}

@Composable
fun ContentStatePanel(
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.fillMaxWidth(),
    ) {
        ContentStateMessage(
            title = title,
            body = body,
            action = action,
            modifier = Modifier.padding(horizontal = 40.dp, vertical = 36.dp),
        )
    }
}
