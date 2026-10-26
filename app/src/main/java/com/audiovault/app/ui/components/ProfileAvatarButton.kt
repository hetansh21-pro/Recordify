package com.audiovault.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiovault.app.R

@Composable
fun ProfileAvatarButton(
    userName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp
) {
    val hasCustomName = userName.isNotBlank()
    val initialLetter = if (hasCustomName) userName.trim().take(1).uppercase() else ""

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (hasCustomName) {
            Box(
                modifier = Modifier
                    .size(size)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = (size.value * 0.44f).sp
                )
            }
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_recordify_logo),
                contentDescription = "Recordify",
                modifier = Modifier.size(size).clip(CircleShape)
            )
        }
    }
}
