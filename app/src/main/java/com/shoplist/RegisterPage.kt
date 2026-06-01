package com.shoplist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun RegisterPage(navController: NavHostController) {

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(30.dp),
                textAlign = TextAlign.Center,
                text = "ShopList",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = {
                    Text("Name")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                },
                label = {
                    Text("Email")
                },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                },
                label = {
                    Text("Password")
                },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Sudah punya akun? Login",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    navController.navigate("login")
                }
            )

            Button(
                onClick = {

                    auth.createUserWithEmailAndPassword(
                        email,
                        password
                    ).addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            val uid = auth.currentUser?.uid ?: ""

                            val profileUpdates = userProfileChangeRequest {
                                displayName = name
                            }

                            auth.currentUser?.updateProfile(profileUpdates)
                                ?.addOnSuccessListener {

                                    val userData = hashMapOf(
                                        "name" to name,
                                        "email" to email,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )

                                    db.collection("users")
                                        .document(uid)
                                        .set(userData)
                                        .addOnSuccessListener {

                                            FirebaseAuth.getInstance().signOut()

                                            navController.navigate("login") {
                                                popUpTo("register") {
                                                    inclusive = true
                                                }
                                            }

                                        }
                                        .addOnFailureListener {
                                            errorMessage =
                                                it.message ?: "Firestore gagal"
                                        }

                                }

                        } else {

                            errorMessage =
                                task.exception?.message
                                    ?: "Register gagal"

                        }
                    }

                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }

            if (errorMessage.isNotEmpty()) {

                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error
                )

            }
        }
    }
}