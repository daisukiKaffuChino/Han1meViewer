package io.github.daisukikaffuchino.han1meviewer.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.daisukikaffuchino.han1meviewer.R

/**
 * 可复用的分页导航，支持省略号和输入页码快速跳转。
 */
@Composable
fun PaginationPager(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    maxVisiblePages: Int = 5,
) {
    require(maxVisiblePages >= 5 && maxVisiblePages % 2 != 0) {
        "maxVisiblePages must be an odd number greater than or equal to 5"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { if (currentPage > 1) onPageSelected(currentPage - 1) },
            enabled = currentPage > 1,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_left),
                contentDescription = "Previous page",
            )
        }

        val pages = remember(currentPage, totalPages, maxVisiblePages) {
            calculatePagination(currentPage, totalPages, maxVisiblePages)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.forEach { page ->
                when (page) {
                    LEFT_ELLIPSIS, RIGHT_ELLIPSIS -> JumpEllipsisItem(
                        totalPages = totalPages,
                        onJump = onPageSelected,
                    )

                    else -> PageItem(
                        page = page,
                        isSelected = page == currentPage,
                        onClick = { onPageSelected(page) },
                    )
                }
            }
        }

        IconButton(
            onClick = { if (currentPage < totalPages) onPageSelected(currentPage + 1) },
            enabled = currentPage < totalPages,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = "Next page",
            )
        }
    }
}

@Composable
private fun JumpEllipsisItem(
    totalPages: Int,
    onJump: (Int) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    var jumpText by remember { mutableStateOf("") }
    var hasFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    if (isEditing) {
        Surface(
            shape = CircleShape,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .width(56.dp)
                .height(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                BasicTextField(
                    value = jumpText,
                    onValueChange = { input ->
                        if (input.length <= totalPages.toString().length) {
                            jumpText = input.filter(Char::isDigit)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                hasFocused = true
                            } else if (hasFocused) {
                                isEditing = false
                                hasFocused = false
                                jumpText = ""
                            }
                        },
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go,
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            jumpText.toIntOrNull()
                                ?.takeIf { it in 1..totalPages }
                                ?.let(onJump)
                            isEditing = false
                            hasFocused = false
                            jumpText = ""
                            focusManager.clearFocus()
                        },
                    ),
                )
            }
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    } else {
        Surface(
            onClick = { isEditing = true },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun PageItem(
    page: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = containerColor,
        modifier = Modifier.size(40.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = page.toString(),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

private fun calculatePagination(current: Int, total: Int, maxVisible: Int): List<Int> {
    if (total <= maxVisible) return (1..total).toList()
    if (total <= 0) return emptyList()

    val safeCurrent = current.coerceIn(1, total)
    val result = mutableListOf<Int>()
    val showLeftEllipsis = safeCurrent > (maxVisible / 2) + 1
    val showRightEllipsis = safeCurrent < total - (maxVisible / 2)

    when {
        !showLeftEllipsis && showRightEllipsis -> {
            for (page in 1..(maxVisible - 2)) result.add(page)
            result.add(RIGHT_ELLIPSIS)
            result.add(total)
        }

        showLeftEllipsis && !showRightEllipsis -> {
            result.add(1)
            result.add(LEFT_ELLIPSIS)
            for (page in (total - maxVisible + 3)..total) result.add(page)
        }

        else -> {
            result.add(1)
            result.add(LEFT_ELLIPSIS)
            val middleRadius = (maxVisible - 4) / 2
            for (page in (safeCurrent - middleRadius)..(safeCurrent + middleRadius)) {
                result.add(page)
            }
            result.add(RIGHT_ELLIPSIS)
            result.add(total)
        }
    }

    return result
}

private const val LEFT_ELLIPSIS = -1
private const val RIGHT_ELLIPSIS = -2

@Preview(showBackground = true, widthDp = 420)
@Composable
private fun PaginationPagerPreview() {
    PaginationPager(
        currentPage = 2,
        totalPages = 10,
        onPageSelected = {},
    )
}
