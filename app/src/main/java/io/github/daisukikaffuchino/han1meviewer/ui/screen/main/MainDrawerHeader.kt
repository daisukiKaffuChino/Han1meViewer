package io.github.daisukikaffuchino.han1meviewer.ui.screen.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview

private val robotoFont = FontFamily(
    Font(R.font.roboto)
)

@Composable
private fun Han1meViewerText(
    modifier: Modifier = Modifier,
    fontSize: Int = 24,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant= MaterialTheme.colorScheme.onSurfaceVariant

    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                color = Color(0xFFFA0201), // #FA0201
                fontFamily = robotoFont,
                fontWeight = FontWeight.Bold,
            )
        ) {
            append("H")
        }
        withStyle(
            style = SpanStyle(
                color = onSurface,
                fontFamily = robotoFont,
                fontWeight = FontWeight.Bold,
            )
        ) {
            append("an1me")
        }
        withStyle(
            style = SpanStyle(
                color = onSurfaceVariant,
                fontFamily = robotoFont,
                fontWeight = FontWeight.Normal,
            )
        ) {
            append("Viewer")
        }
    }

    BasicText(
        text = annotatedString,
        modifier = modifier,
        style = androidx.compose.ui.text.TextStyle(
            fontSize = fontSize.sp,
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

@Composable
fun MainDrawerHeader(
    avatarUrl: String?,
    username: String?,
    isLoggedIn: Boolean,
    isLoading: Boolean,
    currentSite: String,
    onAvatarClick: () -> Unit,
    onAvatarLongClick: () -> Unit,
    onSwitchSiteClick: () -> Unit,
) {
val cardShape = RoundedCornerShape(28.dp)
val cardInteractionSource = remember { MutableInteractionSource() }
val isCardPressed = cardInteractionSource.collectIsPressedAsState().value
val cardScale = animateFloatAsState(
    targetValue = if (isCardPressed) 0.98f else 1f,
    label = "drawerHeaderCardScale"
).value
Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp)
        .padding(top = 12.dp),
){
    Han1meViewerText(
        modifier = Modifier.padding(horizontal = 8.dp)
    )
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(cardShape)
            .combinedClickable(
                interactionSource = cardInteractionSource,
                indication = ripple(),
                onClick = onAvatarClick,
            ),
        shape = cardShape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .combinedClickable(
                        onClick = onAvatarClick,
                        onLongClick = onAvatarLongClick
                    ),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.bg_default_header),
                fallback = painterResource(id = R.drawable.bg_default_header),
                error = painterResource(id = R.drawable.bg_default_header),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = when {
                        isLoading -> stringResource(R.string.loading)
                        isLoggedIn -> username
                            ?: stringResource(R.string.refresh_page_or_login_expired)

                        else -> stringResource(R.string.not_logged_in)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentSite,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Min)
                    .align(Alignment.CenterVertically)
                    .padding(top = 6.dp)
                    .clickable(
                        onClick = onSwitchSiteClick,
                        indication = ripple(bounded = false),
                        interactionSource = remember { MutableInteractionSource() }
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_baseline_switch_24),
                        contentDescription = stringResource(R.string.switch_site)
                    )
                }

                Text(
                    text = stringResource(R.string.switch_site),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.alpha(0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
    }
}

@Preview(showBackground = true)
@Composable
fun MainDrawerHeaderPreview() {
    ComponentPreview {
        MainDrawerHeader(
            avatarUrl = "https://www.baidu.com",
            username = "用户名",
            isLoggedIn = true,
            isLoading = false,
            currentSite = "https://www.baidu.com",
            onAvatarClick = {},
            onAvatarLongClick = {},
            onSwitchSiteClick = {}
        )
    }
}
