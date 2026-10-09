package io.github.daisukikaffuchino.han1meviewer.ui.screen.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import io.github.daisukikaffuchino.han1meviewer.R
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccount
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccountAction
import io.github.daisukikaffuchino.han1meviewer.logic.model.UserAccountSubmittingState
import io.github.daisukikaffuchino.han1meviewer.logic.state.UiState
import io.github.daisukikaffuchino.han1meviewer.ui.component.IconButton
import io.github.daisukikaffuchino.han1meviewer.ui.component.PageContent
import io.github.daisukikaffuchino.han1meviewer.ui.component.appbar.HanimeScaffold
import io.github.daisukikaffuchino.han1meviewer.ui.component.content.ErrorContent
import io.github.daisukikaffuchino.han1meviewer.util.toUiMessage
import io.github.daisukikaffuchino.han1meviewer.ui.preview.ComponentPreview
import io.github.daisukikaffuchino.han1meviewer.ui.screen.rememberRandomLoadingHint
import io.github.daisukikaffuchino.han1meviewer.ui.theme.HanimeDefaults
import io.github.daisukikaffuchino.han1meviewer.ui.viewmodel.UserAccountViewModel
import io.github.daisukikaffuchino.utils.SonnerToast
import io.github.daisukikaffuchino.utils.VibrationUtil
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticButton as Button
import io.github.daisukikaffuchino.han1meviewer.ui.component.HapticTextButton as TextButton

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AccountScreen(
    viewModel: UserAccountViewModel,
    onBack: () -> Unit,
    onOpenAvatarCrop: (String) -> Unit,
    pendingAvatarCropResult: String?,
    onAvatarCropResultConsumed: () -> Unit,
    onRefreshHome: () -> Unit,
    onLogout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val avatarPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onOpenAvatarCrop(it.toString()) }
    }
    val state by viewModel.accountState.collectAsStateWithLifecycle()
    val submittingState by viewModel.submittingState.collectAsStateWithLifecycle()
    val modifySuccess = stringResource(R.string.modify_success)
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadAccount()
    }

    LaunchedEffect(pendingAvatarCropResult) {
        val filePath = pendingAvatarCropResult ?: return@LaunchedEffect
        viewModel.updateAvatar(java.io.File(filePath))
        onAvatarCropResultConsumed()
    }

    LaunchedEffect(viewModel) {

        viewModel.actionFlow.collect { event ->
            when (event.state) {
                UiState.Empty -> Unit
                is UiState.Error -> {
                    SonnerToast.error(event.state.throwable.toUiMessage(context))
                }

                is UiState.Success -> {
                    when (event.action) {
                        UserAccountAction.ProfileUpdated,
                        UserAccountAction.AvatarUpdated -> onRefreshHome()

                        UserAccountAction.PasswordUpdated -> Unit
                    }
                    val message = when (event.action) {
                        UserAccountAction.ProfileUpdated -> modifySuccess
                        UserAccountAction.PasswordUpdated -> modifySuccess
                        UserAccountAction.AvatarUpdated -> modifySuccess
                    }
                    SonnerToast.error(message)
                }

                UiState.Loading -> Unit
            }
        }
    }

    HanimeScaffold(
        title = stringResource(R.string.my_account),
        onBack = onBack,
    ) { paddingValues ->
        val loadingHint = rememberRandomLoadingHint()
        PageContent(
            isLoading = state is UiState.Loading,
            isError = state is UiState.Error,
            isEmpty = state !is UiState.Success,
            onRetry = { viewModel.loadAccount(forceReload = true) },
            loadingMessage = loadingHint,
            error = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    ErrorContent(
                        title = stringResource(R.string.load_failed_retry),
                        onRetry = { viewModel.loadAccount(forceReload = true) },
                    )
                }
            },
        ) {
            val account = (state as? UiState.Success)?.data ?: return@PageContent
            AccountContent(
                account = account,
                submittingState = submittingState,
                contentPadding = paddingValues,
                onUpdateProfile = viewModel::updateProfile,
                onUpdatePassword = viewModel::updatePassword,
                onPickAvatar = {
                    avatarPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onLogout = onLogout,
                onOpenPasswordReset = {
                    uriHandler.openUri("${io.github.daisukikaffuchino.han1meviewer.HANIME_BASE_URL}password/reset")
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AccountContent(
    account: UserAccount,
    submittingState: UserAccountSubmittingState,
    contentPadding: PaddingValues,
    onUpdateProfile: (String, String) -> Unit,
    onUpdatePassword: (String, String, String) -> Unit,
    onPickAvatar: () -> Unit,
    onLogout: () -> Unit,
    onOpenPasswordReset: () -> Unit,
) {
    val view = LocalView.current
    val scrollState = rememberScrollState()

    var name by rememberSaveable(account.username) { mutableStateOf(account.username) }
    var email by rememberSaveable(account.email) { mutableStateOf(account.email) }

    var oldPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var newPasswordConfirm by rememberSaveable { mutableStateOf("") }

    var oldPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var newPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    val isUpdatingProfile = submittingState == UserAccountSubmittingState.UpdatingProfile
    val isUpdatingPassword = submittingState == UserAccountSubmittingState.UpdatingPassword
    val isUpdatingAvatar = submittingState == UserAccountSubmittingState.UpdatingAvatar
    val isSubmitting = submittingState != UserAccountSubmittingState.Idle

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(contentPadding)
            .padding(top = HanimeDefaults.Spacing.itemVertical),
        verticalArrangement = Arrangement.spacedBy(HanimeDefaults.Spacing.itemVertical),
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val defaultPlaceholder = painterResource(R.drawable.h_chan_default_avatar)
                    AsyncImage(
                        model = account.avatarUrl,
                        contentDescription = account.username,
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(2.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentScale = ContentScale.Crop,
                        placeholder = defaultPlaceholder,
                        error = defaultPlaceholder,
                        fallback = defaultPlaceholder,
                    )

                    SmallFloatingActionButton(
                        onClick = {
                            VibrationUtil.performHapticFeedback(view)
                            onPickAvatar()
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 4.dp, y = 4.dp),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        shape = CircleShape,
                    ) {
                        if (isUpdatingAvatar) {
                            LoadingIndicator(modifier = Modifier.size(16.dp))
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_edit),
                                contentDescription = stringResource(R.string.change_avatar),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = account.username,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = "@${account.userId}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(
                        R.string.account_stats_summary,
                        account.subscriberCount,
                        account.videoCount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (!account.joinedLabel.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = account.joinedLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_profile),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.username)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_person),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_mail),
                            contentDescription = null
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                Button(
                    onClick = { onUpdateProfile(name.trim(), email.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting && name.isNotBlank() && email.isNotBlank(),
                ) {
                    if (isUpdatingProfile) {
                        LoadingIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.updating))
                    } else {
                        Text(stringResource(R.string.update_profile))
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.change_password),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(stringResource(R.string.old_password)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { oldPasswordVisible = !oldPasswordVisible }) {
                            Icon(
                                painter = if (oldPasswordVisible) painterResource(R.drawable.ic_visibility) else painterResource(
                                    R.drawable.ic_visibility_off
                                ),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (oldPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.new_password)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                painter = if (oldPasswordVisible) painterResource(R.drawable.ic_visibility) else painterResource(
                                    R.drawable.ic_visibility_off
                                ),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedTextField(
                    value = newPasswordConfirm,
                    onValueChange = { newPasswordConfirm = it },
                    label = { Text(stringResource(R.string.confirm_new_password)) },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lock),
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                painter = if (oldPasswordVisible) painterResource(R.drawable.ic_visibility) else painterResource(
                                    R.drawable.ic_visibility_off
                                ),
                                contentDescription = null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                TextButton(
                    onClick = onOpenPasswordReset,
                    modifier = Modifier.align(Alignment.Start),
                    contentPadding = PaddingValues(horizontal = 0.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.forgot_password),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Button(
                    onClick = {
                        if (newPassword != newPasswordConfirm) {
                            SonnerToast.warning(R.string.password_not_match)
                        } else {
                            onUpdatePassword(oldPassword, newPassword, newPasswordConfirm)
                            oldPassword = ""
                            newPassword = ""
                            newPasswordConfirm = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting && oldPassword.isNotBlank() && newPassword.isNotBlank() && newPasswordConfirm.isNotBlank(),
                ) {
                    if (isUpdatingPassword) {
                        LoadingIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .padding(end = 8.dp)
                        )
                        Text(stringResource(R.string.changing))
                    } else {
                        Text(stringResource(R.string.change_password))
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.errorContainer),
            shape = MaterialTheme.shapes.medium
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_exit_to_app),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.logout), fontWeight = FontWeight.Medium)
        }
    }
}

@Preview(showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun AccountScreenPreview() {
    ComponentPreview {
        AccountContent(
            account = UserAccount(
                csrfToken = "token",
                avatarUrl = "https://picsum.photos/200",
                username = "你的名字",
                email = "username@gmail.com",
                userId = "987654",
                joinedLabel = "加入新1年前",
                subscriberCount = 0,
                videoCount = 9,
            ),
            contentPadding = PaddingValues(),
            onUpdateProfile = { _, _ -> },
            onUpdatePassword = { _, _, _ -> },
            onPickAvatar = {},
            onLogout = {},
            onOpenPasswordReset = {},
            submittingState = UserAccountSubmittingState.Idle,
        )
    }
}
