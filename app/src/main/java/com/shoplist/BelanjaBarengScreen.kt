package com.shoplist
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BelanjaBarengScreen(
    groupId: String,
    groupName: String,
    groupCode: String,
    onBackClick: () -> Unit,
    onChatClick: (String, String, String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val shoppingList = remember { mutableStateListOf<ShoppingItem>() }
    var newItemText by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val lavenderBg = Color(0xFFF3EDF7)
    val primaryPurple = Color(0xFF6750A4)

    // Sinkronisasi data real-time dengan sub-collection Firestore
    DisposableEffect(groupId) {
        val listenerRegistration = db.collection("groups")
            .document(groupId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("Firestore", "Gagal mengambil data barang", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    shoppingList.clear()
                    for (document in snapshot.documents) {
                        val item = document.toObject(ShoppingItem::class.java)?.copy(id = document.id)
                        if (item != null) {
                            shoppingList.add(item)
                        }
                    }
                }
            }

        onDispose {
            listenerRegistration.remove()
        }
    }

    if (showHistoryDialog) {
        HistoryDialog(
            groupId = groupId,
            db = db,
            onDismiss = { showHistoryDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "$groupName - $groupCode", fontSize = 20.sp, color = Color(0xFF1C1B1F))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color(0xFF1C1B1F))
                    }
                },
                actions = {
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "History Hapus"
                        )
                    }
                    IconButton(
                        onClick = {
                            onChatClick(
                                groupId,
                                groupName,
                                groupCode
                            )
                        }
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Chat"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = lavenderBg)
            )
        },

        bottomBar = {
            Surface(
                color = lavenderBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        placeholder = { Text("Tambah barang...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Tombol Tambah Barang Polos Bulat
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(primaryPurple)
                            .clickable {
                                if (newItemText.isNotBlank()) {
                                    val itemData = hashMapOf(
                                        "name" to newItemText.trim(),
                                        "isChecked" to false
                                    )
                                    db.collection("groups")
                                        .document(groupId)
                                        .collection("items")
                                        .add(itemData)
                                        .addOnSuccessListener {
                                            newItemText = ""
                                        }
                                }
                            }
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // PERBAIKAN: Menggunakan objek data list yang benar (shoppingList)
            items(items = shoppingList, key = { it.id }) { item ->
                ShoppingItemRow(
                    item = item,
                    primaryColor = primaryPurple,
                    onCheckedChange = { isChecked ->
                        db.collection("groups")
                            .document(groupId)
                            .collection("items")
                            .document(item.id)
                            .update("isChecked", isChecked)
                    },
                    onDeleteClick = {
                        // PERBAIKAN: Gunakan Batch untuk memindahkan item ke history, lalu hapus dari daftar utama
                        val itemRef = db.collection("groups").document(groupId).collection("items").document(item.id)
                        val historyRef = db.collection("groups").document(groupId).collection("history").document(item.id)

                        db.runBatch { batch ->
                            // 1. Simpan ke history
                            batch.set(historyRef, item)
                            // 2. Hapus dari list aktif
                            batch.delete(itemRef)
                        }.addOnFailureListener { e ->
                            android.util.Log.e("Firestore", "Gagal memindahkan ke history", e)
                        }
                    }
                )
            }
        }
    }
}

// Komponen baru untuk menampilkan Dialog History
@Composable
fun HistoryDialog(
    groupId: String,
    db: FirebaseFirestore,
    onDismiss: () -> Unit
) {
    val historyItems = remember { mutableStateListOf<ShoppingItem>() }

    // Ambil data dari sub-collection "history"
    LaunchedEffect(groupId) {
        db.collection("groups")
            .document(groupId)
            .collection("history")
            .get()
            .addOnSuccessListener { snapshot ->
                historyItems.clear()
                for (document in snapshot.documents) {
                    val item = document.toObject(ShoppingItem::class.java)?.copy(id = document.id)
                    if (item != null) {
                        historyItems.add(item)
                    }
                }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Riwayat Dihapus") },
        text = {
            if (historyItems.isEmpty()) {
                Text("Belum ada barang yang dihapus.", color = Color.Gray)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyItems) { item ->
                        Text(
                            text = "• ${item.name}", // Pastikan atribut "name" sesuai dengan properti di data class ShoppingItem kamu
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        }
    )
}