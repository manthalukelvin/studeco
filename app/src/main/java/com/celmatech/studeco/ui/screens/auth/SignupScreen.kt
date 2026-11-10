package com.celmatech.studeco.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private val institutions = listOf(
    "kuhes" to "Kamuzu University of Health Sciences (KUHES)",
    "unima" to "University of Malawi",
    "mubas" to "Malawi University of Business and Applied Sciences",
    "must" to "Malawi University of Science and Technology",
    "luanar" to "Lilongwe University of Agriculture and Natural Resources",
    "mca" to "Malawi College of Accountancy",
    "other_uni" to "Other university",
    "other_sec" to "Other secondary school"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    onSignupSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var educationLevel by remember { mutableStateOf("university") }
    var institution by remember { mutableStateOf("kuhes") }
    var programme by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expandedEdu by remember { mutableStateOf(false) }
    var expandedInst by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Create account",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Join the digital campus",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password (min 8 characters)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(expanded = expandedEdu, onExpandedChange = { expandedEdu = it }) {
                OutlinedTextField(
                    value = if (educationLevel == "university") "University" else "Secondary",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Education level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedEdu) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedEdu, onDismissRequest = { expandedEdu = false }) {
                    DropdownMenuItem(text = { Text("University") }, onClick = {
                        educationLevel = "university"; expandedEdu = false
                    })
                    DropdownMenuItem(text = { Text("Secondary") }, onClick = {
                        educationLevel = "secondary"; expandedEdu = false
                    })
                }
            }
            Spacer(Modifier.height(10.dp))

            ExposedDropdownMenuBox(expanded = expandedInst, onExpandedChange = { expandedInst = it }) {
                OutlinedTextField(
                    value = institutions.find { it.first == institution }?.second ?: institution,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Institution") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expandedInst) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(expanded = expandedInst, onDismissRequest = { expandedInst = false }) {
                    institutions.forEach { (id, name) ->
                        DropdownMenuItem(text = { Text(name) }, onClick = {
                            institution = id; expandedInst = false
                        })
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = programme,
                onValueChange = { programme = it },
                label = { Text("Programme / Course (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    if (fullName.isBlank() || email.isBlank() || password.length < 8) {
                        errorMessage = "Please fill all required fields (password ≥ 8 chars)"
                        return@Button
                    }
                    isLoading = true
                    viewModel.signUp(email, password, fullName, educationLevel, institution, programme) { result ->
                        isLoading = false
                        result.onSuccess { onSignupSuccess() }
                            .onFailure { errorMessage = it.message ?: "Sign up failed" }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp, modifier = Modifier.height(24.dp))
                else Text("Create account")
            }

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onNavigateToLogin) {
                Text("Already have an account? Log in")
            }
        }
    }
}
