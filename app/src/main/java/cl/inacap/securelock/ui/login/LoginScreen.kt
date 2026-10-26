package cl.inacap.securelock.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(viewModel: LoginViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    var mostrarPassword by remember { mutableStateOf(false) }
    val esRegistro = state.mode == AuthMode.REGISTER

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("SecureLock", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
        Text(
            if (esRegistro) "Crea tu cuenta" else "Control de acceso para tu locker",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(28.dp))

        if (esRegistro) {
            OutlinedTextField(
                value = state.nombre,
                onValueChange = viewModel::onNombreChange,
                label = { Text("Nombre") },
                singleLine = true,
                enabled = !state.isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
        }
        OutlinedTextField(
            value = state.email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Correo electrónico") },
            singleLine = true,
            enabled = !state.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) }),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = state.password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Contraseña") },
            supportingText = if (esRegistro) {
                { Text("Mínimo ${LoginViewModel.MIN_PASSWORD} caracteres") }
            } else null,
            singleLine = true,
            enabled = !state.isLoading,
            visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                TextButton(onClick = { mostrarPassword = !mostrarPassword }) { Text(if (mostrarPassword) "Ocultar" else "Mostrar") }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = if (esRegistro) ImeAction.Next else ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focus.moveFocus(FocusDirection.Down) },
                onDone = {
                    focus.clearFocus()
                    viewModel.submit()
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        if (esRegistro) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                label = { Text("Repite la contraseña") },
                singleLine = true,
                enabled = !state.isLoading,
                visualTransformation = if (mostrarPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focus.clearFocus()
                    viewModel.submit()
                }),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.error?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }
        state.info?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = viewModel::submit,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(if (esRegistro) "Crear cuenta" else "Iniciar sesión")
            }
        }
        Spacer(Modifier.height(4.dp))
        TextButton(onClick = viewModel::toggleMode, enabled = !state.isLoading) {
            Text(if (esRegistro) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Crear una")
        }
        if (!esRegistro) {
            TextButton(onClick = viewModel::resetPassword, enabled = !state.isLoading) { Text("Olvidé mi contraseña") }
        }

        if (!state.esDemo) return@Column
        Spacer(Modifier.height(20.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Modo demo", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Los datos son simulados en el teléfono. Cuenta de prueba: ${LoginViewModel.DEMO_EMAIL} / ${LoginViewModel.DEMO_PASSWORD}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row {
                OutlinedButton(onClick = viewModel::fillDemoCredentials, enabled = !state.isLoading) { Text("Usar cuenta demo") }
            }
        }
    }
}
