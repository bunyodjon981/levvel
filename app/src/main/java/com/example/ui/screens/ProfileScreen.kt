package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.model.CloudNotificationItem
import com.example.model.UserProfile
import com.example.ui.theme.CoralMagenta
import com.example.ui.theme.CyanElectric
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GoldBeat
import com.example.ui.theme.StudioObsidian
import com.example.ui.theme.StudioOutline
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    currentUser: UserProfile?,
    usageCount: Int,
    firebaseConnected: Boolean,
    notifications: List<CloudNotificationItem>,
    authLoading: Boolean,
    authError: String?,
    onClearAuthError: () -> Unit,
    onRegister: (username: String, email: String, password: String, confirmPassword: String) -> Unit,
    onLogin: (identifier: String, password: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (currentUser == null) {
        SignedOutAuthView(
            usageCount = usageCount,
            authLoading = authLoading,
            authError = authError,
            onClearAuthError = onClearAuthError,
            onRegister = onRegister,
            onLogin = onLogin,
            modifier = modifier
        )
    } else {
        SignedInProfileView(
            user = currentUser,
            usageCount = usageCount,
            firebaseConnected = firebaseConnected,
            notifications = notifications,
            onLogout = onLogout,
            modifier = modifier
        )
    }
}

@Composable
private fun SignedOutAuthView(
    usageCount: Int,
    authLoading: Boolean,
    authError: String?,
    onClearAuthError: () -> Unit,
    onRegister: (String, String, String, String) -> Unit,
    onLogin: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var modeTab by remember { mutableIntStateOf(0) } // 0 = Ro'yxatdan o'tish, 1 = Kirish

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var regPassword by remember { mutableStateOf("") }
    var regConfirmPassword by remember { mutableStateOf("") }

    var loginIdentifier by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CyanElectric, CoralMagenta))),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = "Profil",
                tint = Color(0xFF0B0B13),
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Lavelle Profil",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Loyihalaringiz va shablonlaringizni saqlash uchun hisob oching yoki kiring.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
        if (usageCount > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = StudioSurfaceVariant,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Ilovadan foydalanish: $usageCount marta",
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldBeat,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Segmented Mode Switcher: Ro'yxatdan o'tish vs Kirish
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(StudioSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = {
                    modeTab = 0
                    onClearAuthError()
                },
                color = if (modeTab == 0) CyanElectric else Color.Transparent,
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_tab_register")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Ro'yxatdan o'tish",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (modeTab == 0) Color(0xFF001F26) else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                onClick = {
                    modeTab = 1
                    onClearAuthError()
                },
                color = if (modeTab == 1) CyanElectric else Color.Transparent,
                shape = RoundedCornerShape(11.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("profile_tab_login")
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Kirish",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (modeTab == 1) Color(0xFF001F26) else TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            color = StudioSurface,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, StudioOutline.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (modeTab == 0) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            onClearAuthError()
                        },
                        label = { Text("Username") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = CyanElectric) },
                        singleLine = true,
                        colors = authFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_username_input")
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            onClearAuthError()
                        },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null, tint = CyanElectric) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = authFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_email_input")
                    )

                    OutlinedTextField(
                        value = regPassword,
                        onValueChange = {
                            regPassword = it
                            onClearAuthError()
                        },
                        label = { Text("Parol") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = CyanElectric) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Ko'rish",
                                    tint = TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = authFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_password_input")
                    )

                    OutlinedTextField(
                        value = regConfirmPassword,
                        onValueChange = {
                            regConfirmPassword = it
                            onClearAuthError()
                        },
                        label = { Text("Parolni takrorlash") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = CoralMagenta) },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = authFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reg_confirm_password_input")
                    )

                    if (authError != null) {
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = { onRegister(username, email, regPassword, regConfirmPassword) },
                        enabled = !authLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("register_submit_button")
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF001F26),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Ro'yxatdan o'tish va kirish",
                                color = Color(0xFF001F26),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = loginIdentifier,
                        onValueChange = {
                            loginIdentifier = it
                            onClearAuthError()
                        },
                        label = { Text("Username yoki Email") },
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = CyanElectric) },
                        singleLine = true,
                        colors = authFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_identifier_input")
                    )

                    OutlinedTextField(
                        value = loginPassword,
                        onValueChange = {
                            loginPassword = it
                            onClearAuthError()
                        },
                        label = { Text("Parol") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = CyanElectric) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Ko'rish",
                                    tint = TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = authFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input")
                    )

                    if (authError != null) {
                        Text(
                            text = authError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = { onLogin(loginIdentifier, loginPassword) },
                        enabled = !authLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = CyanElectric),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_submit_button")
                    ) {
                        if (authLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF001F26),
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Kirish",
                                color = Color(0xFF001F26),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SignedInProfileView(
    user: UserProfile,
    usageCount: Int,
    firebaseConnected: Boolean,
    notifications: List<CloudNotificationItem>,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(user.createdAt) {
        if (user.createdAt <= 0L) "Bugun"
        else SimpleDateFormat("dd.MM.yyyy", Locale.US).format(Date(user.createdAt))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(StudioObsidian),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = StudioSurface,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(CyanElectric.copy(0.6f), CoralMagenta.copy(0.5f))),
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CyanElectric, CoralMagenta))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.username.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color(0xFF0B0B13),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = if (firebaseConnected) EmeraldSuccess.copy(alpha = 0.16f) else StudioSurfaceVariant,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = if (firebaseConnected) "Firebase bilan sinxron" else "Lokal hisob • $dateStr",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (firebaseConnected) EmeraldSuccess else GoldBeat,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ProfileStatBox(
                    label = "Montajlar",
                    value = "${user.montageCount}",
                    accent = CyanElectric,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatBox(
                    label = "Foydalanish",
                    value = "$usageCount",
                    accent = GoldBeat,
                    modifier = Modifier.weight(1f)
                )
                ProfileStatBox(
                    label = "Qo'shilgan",
                    value = dateStr,
                    accent = CoralMagenta,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (notifications.isNotEmpty()) {
            item {
                Text(
                    text = "Oxirgi e'lon va xabarlar",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White
                )
            }
            items(notifications.take(5), key = { it.id }) { item ->
                Surface(
                    color = StudioSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, StudioOutline.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.NotificationsActive,
                            contentDescription = null,
                            tint = GoldBeat,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(item.message, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Chiqish",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hisobdan chiqish", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ProfileStatBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = StudioSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, StudioOutline.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun authFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = StudioSurfaceElevated,
    unfocusedContainerColor = StudioSurfaceElevated,
    focusedBorderColor = CyanElectric,
    unfocusedBorderColor = StudioOutline.copy(alpha = 0.6f),
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)
