package com.shoplist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable // <-- 1. IMPORT BARU YANG HARUS DITAMBAH
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

// Model data
data class ShoppingGroup(
    val id: String,
    val name: String,
    val code: String,
    val pendingItemsCount: Int
)

@Preview
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    // 2. TAMBAHKAN PARAMETER LAMBDA DI SINI (Diberi default kosong agar @Preview tetap bekerja)
    onGroupClick: (String, String, String) -> Unit = { _, _, _ -> }
) {
    val groupList = remember { mutableStateListOf<ShoppingGroup>() }
    var showDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUid = auth.currentUser?.uid ?: return@LaunchedEffect

        db.collection("groups")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    android.util.Log.e("Firestore", "Gagal mengambil data", error)
                    return@addSnapshotListener
                }

                groupList.clear()

                snapshot?.documents?.forEach { document ->

                    db.collection("groups")
                        .document(document.id)
                        .collection("users")
                        .document(currentUid)
                        .get()
                        .addOnSuccessListener { userDoc ->

                            if (userDoc.exists()) {

                                val name = document.getString("name") ?: ""
                                val code = document.getString("code") ?: ""

                                groupList.add(
                                    ShoppingGroup(
                                        id = document.id,
                                        name = name,
                                        code = code,
                                        pendingItemsCount = 0
                                    )
                                )
                            }
                        }
                }
            }
    }

    Scaffold(
        containerColor = Color(0xFFF7F9FC),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Kita Belanja",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF6750A4)
                ),
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(32.dp)
                            .background(color = Color(0xFFD0BCFF), shape = CircleShape)
                    )
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Navigasi ke History */ }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "History",
                            tint = Color(0xFF381E72)
                        )
                    }

                    IconButton(onClick = { /* Navigasi ke Home */ }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = Color(0xFF381E72)
                        )
                    }

                    FloatingActionButton(
                        onClick = { showDialog = true },
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp),
                        elevation = FloatingActionButtonDefaults.elevation(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah Grup"
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = 8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(groupList) { group ->
                // 3. OPER PARAMETER ONGROUPCLICK KE COMPONENT CARD
                GroupCardItem(group = group, onGroupClick = onGroupClick)
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDialog = false
                    newGroupName = ""
                },
                title = {
                    Text(text = "Buat Grup Baru", fontWeight = FontWeight.Bold)
                },
                text = {
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        label = { Text("Nama Grup") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newGroupName.isNotBlank()) {
                                val db = FirebaseFirestore.getInstance()
                                val auth = FirebaseAuth.getInstance()
                                val currentUserId = auth.currentUser?.uid ?: ""

                                val groupData = hashMapOf(
                                    "name" to newGroupName.trim(),
                                    "code" to List(4) {(('A'..'Z') + ('0'..'9')).random()}.joinToString(""),
                                    "createdBy" to currentUserId,
                                    "createdAt" to FieldValue.serverTimestamp()
                                )

                                db.collection("groups")
                                    .add(groupData)
                                    .addOnSuccessListener { groupRef ->

                                        val userData = hashMapOf(
                                            "uid" to currentUserId,
                                            "joinedAt" to FieldValue.serverTimestamp()
                                        )

                                        groupRef.collection("users")
                                            .document(currentUserId)
                                            .set(userData)
                                            .addOnSuccessListener {
                                                showDialog = false
                                                newGroupName = ""
                                            }
                                            .addOnFailureListener { e ->
                                                android.util.Log.e("Firestore", "Gagal menambah user", e)
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        android.util.Log.e("Firestore", "Gagal membuat grup", e)
                                    }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                    ) {
                        Text("Buat")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog = false
                            newGroupName = ""
                        }
                    ) {
                        Text("Batal", color = Color(0xFF6750A4))
                    }
                }
            )
        }
    }
}

@Composable
fun GroupCardItem(
    group: ShoppingGroup,
    onGroupClick: (String, String, String) -> Unit // <-- 4. TAMBAHKAN PARAMETER DI SINI JUGA
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            // 5. TAMBAHKAN CLICKABLE AGAR KARTU BISA DIKLIK DAN MENGIRIM ID & NAMA
            .clickable { onGroupClick(group.id, group.code, group.name) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = group.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1B1F)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (group.pendingItemsCount > 0) "${group.pendingItemsCount} barang belum dibeli" else "Semua belanjaan beres!",
                fontSize = 14.sp,
                color = if (group.pendingItemsCount > 0) Color(0xFF49454F) else Color(0xFF008069)
            )
        }
    }
}