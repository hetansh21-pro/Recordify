package com.audiovault.app.ui.tv

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.ui.theme.SpotifyGreenBright

/**
 * 10-Foot Left Sidebar Navigation Rail for Recordify TV.
 * Remains stationary while main content area scrolls independently.
 * Features stable circular icon buttons in collapsed mode (74dp),
 * expanding into full pills (240dp) ONLY when the top profile button is clicked.
 */
@Composable
fun TvSidebar(
    selectedTab: TvNavTab,
    onTabSelected: (TvNavTab) -> Unit,
    userCountryCode: String,
    primaryLanguage: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sidebarWidth by animateDpAsState(
        targetValue = if (isExpanded) 240.dp else 74.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "tv_sidebar_width"
    )

    Column(
        modifier = modifier
            .width(sidebarWidth)
            .background(Color(0xFF101010))
            .padding(vertical = 16.dp, horizontal = if (isExpanded) 12.dp else 13.dp),
        horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = if (isExpanded) Alignment.Start else Alignment.CenterHorizontally
        ) {
            // Master Expand/Collapse Profile Button (Remote Navigable & Stable)
            if (isExpanded) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1C1C1C),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SpotifyGreenBright),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .tvRemoteFocusable(
                            onClick = onToggleExpand,
                            shape = RoundedCornerShape(24.dp),
                            focusScale = 1.04f
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SpotifyGreenBright,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Recordify TV",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 13.sp
                                ),
                                maxLines = 1
                            )
                            Text(
                                text = "Click to Collapse ◀",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = SpotifyGreenBright,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            } else {
                // Collapsed: Stable 48dp Circular Profile Button
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF1A1A1A),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, SpotifyGreenBright.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .size(48.dp)
                        .tvRemoteFocusable(
                            onClick = onToggleExpand,
                            shape = CircleShape,
                            focusScale = 1.12f,
                            focusBorderColor = SpotifyGreenBright
                        )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profile - Click to expand navigation",
                            tint = SpotifyGreenBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Navigation Tabs (Stable 48dp Circles when collapsed, Full Pill when expanded)
            TvNavTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                TvSidebarItem(
                    tab = tab,
                    isSelected = isSelected,
                    isExpanded = isExpanded,
                    onClick = { onTabSelected(tab) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Profile / Region Badge (Collapsible)
        if (isExpanded) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161616),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF262626)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(
                        text = "Region • $userCountryCode",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = primaryLanguage,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF888888),
                            fontSize = 9.sp
                        )
                    )
                }
            }
        } else {
            // Small circular country / profile badge
            Surface(
                shape = CircleShape,
                color = Color(0xFF181818),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A2A2A)),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = userCountryCode.take(2).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = SpotifyGreenBright,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun TvSidebarItem(
    tab: TvNavTab,
    isSelected: Boolean,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    var hasFocus by remember { mutableStateOf(false) }

    val bgColor by animateColorAsState(
        targetValue = when {
            hasFocus -> SpotifyGreenBright.copy(alpha = 0.25f)
            isSelected -> Color(0xFF262626)
            else -> Color.Transparent
        },
        label = "nav_bg"
    )

    val contentColor by animateColorAsState(
        targetValue = when {
            hasFocus || isSelected -> SpotifyGreenBright
            else -> Color(0xFFB3B3B3)
        },
        label = "nav_color"
    )

    val itemShape = if (isExpanded) RoundedCornerShape(24.dp) else CircleShape

    Box(
        modifier = Modifier
            .then(if (isExpanded) Modifier.fillMaxWidth().height(46.dp) else Modifier.size(48.dp))
            .clip(itemShape)
            .background(bgColor)
            .tvRemoteFocusable(
                onClick = onClick,
                shape = itemShape,
                focusScale = if (isExpanded) 1.04f else 1.12f,
                focusBorderWidth = 2.dp,
                onFocusChanged = { focused ->
                    hasFocus = focused
                }
            ),
        contentAlignment = if (isExpanded) Alignment.CenterStart else Alignment.Center
    ) {
        if (isExpanded) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected || hasFocus) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected || hasFocus) Color.White else Color(0xFFB3B3B3),
                        fontSize = 13.sp
                    ),
                    maxLines = 1
                )
            }
        } else {
            // Compact circle showing only the icon
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.title,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
