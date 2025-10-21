package com.xunyidi.sealmeet.presentation.screen.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xunyidi.sealmeet.presentation.settings.SettingsScreen
import com.xunyidi.sealmeet.presentation.theme.AppColors
import com.xunyidi.sealmeet.presentation.theme.TextInverse

/**
 * 登录页面 - 卡片式设计
 */
@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToHome: () -> Unit = {},
    onNavigateToQuickMeeting: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    var showSettings by remember { mutableStateOf(false) }

    // 监听副作用
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is LoginContract.Effect.NavigateToHome -> {
                    onNavigateToHome()
                }
                is LoginContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.bgPage)
    ) {
        // 中间的登录卡片
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.bgCard
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Logo和标题
                Text(
                    text = "SealMeet",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.primaryDefault
                )
                
                Text(
                    text = "离线会议平板",
                    fontSize = 16.sp,
                    color = AppColors.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 用户名输入框
                OutlinedTextField(
                    value = state.username,
                    onValueChange = { 
                        viewModel.handleIntent(LoginContract.Intent.UsernameChanged(it))
                    },
                    label = { Text("用户名") },
                    placeholder = { Text("请输入用户名") },
                    singleLine = true,
                    isError = state.usernameError != null,
                    supportingText = {
                        state.usernameError?.let {
                            Text(
                                text = it,
                                color = AppColors.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.primaryDefault,
                        focusedLabelColor = AppColors.primaryDefault,
                        cursorColor = AppColors.primaryDefault
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // 密码输入框
                OutlinedTextField(
                    value = state.password,
                    onValueChange = { 
                        viewModel.handleIntent(LoginContract.Intent.PasswordChanged(it))
                    },
                    label = { Text("密码") },
                    placeholder = { Text("请输入密码") },
                    singleLine = true,
                    isError = state.passwordError != null,
                    supportingText = {
                        state.passwordError?.let {
                            Text(
                                text = it,
                                color = AppColors.error
                            )
                        }
                    },
                    visualTransformation = if (state.isPasswordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                viewModel.handleIntent(LoginContract.Intent.TogglePasswordVisibility)
                            }
                        ) {
                            Icon(
                                imageVector = if (state.isPasswordVisible) {
                                    Icons.Default.VisibilityOff
                                } else {
                                    Icons.Default.Visibility
                                },
                                contentDescription = if (state.isPasswordVisible) {
                                    "隐藏密码"
                                } else {
                                    "显示密码"
                                }
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.handleIntent(LoginContract.Intent.Login)
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppColors.primaryDefault,
                        focusedLabelColor = AppColors.primaryDefault,
                        cursorColor = AppColors.primaryDefault
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 登录按钮
                Button(
                    onClick = {
                        viewModel.handleIntent(LoginContract.Intent.Login)
                    },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.primaryDefault,
                        disabledContainerColor = AppColors.primaryDisabled
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = TextInverse,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "登录",
                            fontSize = 16.sp,
                            color = TextInverse
                        )
                    }
                }

                // 快速会议按钮
                OutlinedButton(
                    onClick = onNavigateToQuickMeeting,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = AppColors.primaryDefault
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AppColors.primaryDefault)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "快速会议",
                        fontSize = 16.sp
                    )
                }

                // 提示文本
                Text(
                    text = "测试账号: admin / 123456",
                    fontSize = 12.sp,
                    color = AppColors.textTertiary
                )
            }
        }
        
        // 左下角的设置按钮
        IconButton(
            onClick = { showSettings = true },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "开发设置",
                tint = AppColors.textSecondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
    
    // 显示设置对话框
    if (showSettings) {
        SettingsScreen(
            onDismiss = { showSettings = false }
        )
    }
}
