package com.shoplist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BelanjaBarengScreen(
    groupId: String,
    groupName: String,
    onBackClick: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val shoppingList = remember { mutableStateListOf<ShoppingItem>() }
    var newItemText by remember { mutableStateOf("") }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = groupName, fontSize = 20.sp, color = Color(0xFF1C1B1F))
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF1C1B1F))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = lavenderBg)
            )
        },
        bottomBar = {
            Surface(color = lavenderBg, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = newItemText,
                        onValueChange = { newItemText = it },
                        placeholder = { Text("Tambah barang...", color = Color.Gray) },
                        modifier = Modifier.weight(1f).height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.width(12.dp))

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
            items(items = shoppingList, key = { it.id }) { item ->
                // Memanggil Komponen Baris Item dari file terpisah yang telah dibuat
                ShoppingItemRow(
                    item = item,
                    primaryColor = primaryPurple,
                    onCheckedChange = { isChecked ->
                        db.collection("groups")
                            .document(groupId)
                            .collection("items")
                            .document(item.id)
                            .update("isChecked", isChecked)
                    }
                )
            }
        }
    }
}