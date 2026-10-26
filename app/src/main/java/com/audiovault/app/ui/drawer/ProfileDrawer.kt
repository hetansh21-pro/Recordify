package com.audiovault.app.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.R
import com.audiovault.app.ui.theme.SpotifyDarkSurface

@Composable
fun ProfileDrawerSheet(
    userName: String,
    onEditProfile: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecord: () -> Unit = {},
    onNavigateToRecents: () -> Unit = {},
    onNavigateToUpdates: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hasCustomName = userName.isNotBlank()
    val initialLetter = if (hasCustomName) userName.trim().take(1).uppercase() else ""

    ModalDrawerSheet(
        drawerContainerColor = SpotifyDarkSurface,
        modifier = modifier
            .fillMaxHeight()
            .width(310.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(horizontal = 20.dp)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // User Profile Header (Spotify Style)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .clickable { onEditProfile() }
                    .padding(vertical = 10.dp)
            ) {
                if (hasCustomName) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF00C9FF), Color(0xFF92FE9D))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initialLetter,
                            color = Color.Black,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp
                        )
                    }
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_recordify_logo),
                        contentDescription = "Recordify",
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = if (hasCustomName) userName else "Recordify",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = if (hasCustomName) "View profile" else "Tap to set your name",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFFB3B3B3)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Drawer Action Items
            DrawerMenuItem(
                icon = Icons.Default.Mic,
                title = "Song Recorder / Capture",
                onClick = onNavigateToRecord
            )

            DrawerMenuItem(
                icon = Icons.Default.ElectricBolt,
                title = "What's new",
                onClick = onNavigateToUpdates
            )

            DrawerMenuItem(
                icon = Icons.Default.History,
                title = "Listening history",
                onClick = onNavigateToRecents
            )

            DrawerMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings and privacy",
                onClick = onNavigateToSettings
            )

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(color = Color(0xFF282828), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Brand / Version
            Text(
                text = "Recordify v1.0.0",
                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF666666)),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

@Composable
private fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color(0xFFCCCCCC),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        )
    }
}
