package com.shoplist

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID


// Tambahkan joinCode di Model Data agar bisa ditampilkan di UI
data class ShoppingGroup(
    val id: String,
    val name: String,
    val pendingItemsCount: Int,
    val joinCode: String
)

// Enum untuk mengatur jenis Dialog yang muncul
enum class GroupDialogState {
    NONE, CHOOSE_ACTION, CREATE_GROUP, JOIN_GROUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    onGroupClick: (String, String, String) -> Unit,
    onLogoutClick: () -> Unit // <-- TAMBAHAN BARU
) {
    val context = LocalContext.current
    val groupList = remember { mutableStateListOf<ShoppingGroup>() }

    // State untuk Dialog
    var dialogState by remember { mutableStateOf(GroupDialogState.NONE) }
    var groupNameInput by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }

    // PROSES MENGAMBIL DATA GRUP SECARA REAL-TIME DARI FIRESTORE
    // LaunchedEffect(Unit) berjalan sekali saat halaman Home pertama kali dibuka
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            // FILTER PRIVASI: Hanya ambil dokumen dari koleksi 'groups' 
            // yang mana field 'members' (array UID) mengandung UID saya yang sedang login
            db.collection("groups")
                .whereArrayContains("members", currentUserId)
                // REAL-TIME LISTENER: Berlangganan perubahan data langsung ke server
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        android.util.Log.e("Firestore", "Gagal mengambil data", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        // Bersihkan daftar grup lama di memori HP agar terhindar dari duplikasi
                        groupList.clear()
                        
                        // Iterasi setiap dokumen grup dari server
                        for (document in snapshot.documents) {
                            val name = document.getString("name") ?: ""
                            val code = document.getString("joinCode") ?: "-"
                            val pendingCount = 0

                            // Konversi dokumen ke model ShoppingGroup, lalu tambahkan ke State List
                            groupList.add(
                                ShoppingGroup(
                                    id = document.id,
                                    name = name,
                                    pendingItemsCount = pendingCount,
                                    joinCode = code
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
                    Text("Kita Belanja", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF6750A4)),
                actions = {
                    // =======================================================
                    // PERUBAHAN DI SINI: Mengganti Box dengan IconButton
                    // =======================================================
                    IconButton(onClick = { onLogoutClick() }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(containerColor = Color.White, tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FloatingActionButton(
                        onClick = { dialogState = GroupDialogState.CHOOSE_ACTION }, // Munculkan pilihan
                        containerColor = Color(0xFFD0BCFF),
                        contentColor = Color(0xFF381E72),
                        shape = CircleShape,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, "Tambah/Gabung Grup")
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(top = 8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(groupList) { group ->
                GroupCardItem(
                    group = group,
                    onClick = {
                        // Saat kartu diklik, kirim ID, Kode, dan Nama ke MainActivity
                        onGroupClick(group.id, group.joinCode, group.name)
                    }
                )
            }
        }

        // KUMPULAN DIALOG (CHOOSE, CREATE, JOIN)
        when (dialogState) {
            GroupDialogState.NONE -> {} // Tidak menampilkan apa-apa

            // 1. Dialog Pilihan Awal
            GroupDialogState.CHOOSE_ACTION -> {
                AlertDialog(
                    onDismissRequest = { dialogState = GroupDialogState.NONE },
                    title = { Text("Pilih Aksi", fontWeight = FontWeight.Bold) },
                    text = { Text("Apa yang ingin kamu lakukan?") },
                    confirmButton = {
                        Button(
                            onClick = { dialogState = GroupDialogState.CREATE_GROUP },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) { Text("Buat Grup Baru") }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { dialogState = GroupDialogState.JOIN_GROUP }
                        ) { Text("Gabung Grup", color = Color(0xFF6750A4)) }
                    }
                )
            }

            // 2. Dialog Membuat Grup Baru
            GroupDialogState.CREATE_GROUP -> {
                AlertDialog(
                    onDismissRequest = {
                        dialogState = GroupDialogState.NONE
                        groupNameInput = ""
                    },
                    title = { Text("Buat Grup Baru", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = groupNameInput,
                            onValueChange = { groupNameInput = it },
                            label = { Text("Nama Grup") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (groupNameInput.isNotBlank()) {
                                    val db = FirebaseFirestore.getInstance()
                                    val auth = FirebaseAuth.getInstance()
                                    val currentUserId = auth.currentUser?.uid ?: return@Button

                                    // 1. GENERATE KODE UNIK: Potong UUID jadi 6 karakter huruf kapital untuk kode gabung grup
                                    val generatedCode = UUID.randomUUID().toString().substring(0, 6).uppercase()

                                    // 2. STRUKTUR DATA GRUP: Siapkan map data untuk disimpan di Firestore
                                    val groupData = hashMapOf(
                                        "name" to groupNameInput.trim(), // Nama grup belanja
                                        "createdBy" to currentUserId, // UID pembuat grup
                                        "createdAt" to FieldValue.serverTimestamp(), // Waktu pembuatan dari server
                                        "joinCode" to generatedCode, // Kode undangan 6 digit
                                        "members" to listOf(currentUserId) // Anggota pertama (pembuat grup)
                                    )

                                    // 3. SIMPAN GRUP KE FIRESTORE: Masukkan data ke koleksi 'groups'
                                    db.collection("groups").add(groupData)
                                        .addOnSuccessListener {
                                            // 4. Tutup dialog dan kosongkan form input
                                            dialogState = GroupDialogState.NONE
                                            groupNameInput = ""
                                            Toast.makeText(context, "Grup berhasil dibuat!", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) { Text("Buat") }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogState = GroupDialogState.NONE }) {
                            Text("Batal", color = Color(0xFF6750A4))
                        }
                    }
                )
            }

            // 3. Dialog Bergabung dengan Grup
            GroupDialogState.JOIN_GROUP -> {
                AlertDialog(
                    onDismissRequest = {
                        dialogState = GroupDialogState.NONE
                        joinCodeInput = ""
                    },
                    title = { Text("Gabung Grup", fontWeight = FontWeight.Bold) },
                    text = {
                        Column {
                            Text("Masukkan 6 digit kode dari temanmu.", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { joinCodeInput = it.uppercase() }, // Paksa huruf besar
                                label = { Text("Kode Grup") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (joinCodeInput.isNotBlank()) {
                                    val db = FirebaseFirestore.getInstance()
                                    val auth = FirebaseAuth.getInstance()
                                    val currentUserId = auth.currentUser?.uid ?: return@Button

                                    // 1. CARI GRUP BERDASARKAN KODE: Lakukan query ke Firestore untuk mencari grup dengan joinCode yang cocok
                                    db.collection("groups")
                                        .whereEqualTo("joinCode", joinCodeInput.trim())
                                        .get()
                                        .addOnSuccessListener { querySnapshot ->
                                            if (!querySnapshot.isEmpty) {
                                                // Jika ketemu, ambil ID dokumen grup tersebut
                                                val docId = querySnapshot.documents[0].id

                                                // 2. GABUNG GRUP (UPDATE): Masukkan UID saya ke dalam array 'members'
                                                // Menggunakan arrayUnion agar UID hanya masuk sekali (mencegah duplikat jika klik berkali-kali)
                                                db.collection("groups").document(docId)
                                                    .update("members", FieldValue.arrayUnion(currentUserId))
                                                    .addOnSuccessListener {
                                                        // 3. Tutup dialog dan bersihkan input
                                                        dialogState = GroupDialogState.NONE
                                                        joinCodeInput = ""
                                                        Toast.makeText(context, "Berhasil gabung ke grup!", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                // Jika tidak ada grup yang menggunakan kode tersebut
                                                Toast.makeText(context, "Kode grup tidak ditemukan!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6750A4))
                        ) { Text("Gabung") }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogState = GroupDialogState.NONE }) {
                            Text("Batal", color = Color(0xFF6750A4))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun GroupCardItem(group: ShoppingGroup, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() }, // Ini bagian yang membuat kartu bisa merespons klik
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = group.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1C1B1F))

                // Menampilkan Kode Grup agar bisa di-share ke teman
                Surface(
                    color = Color(0xFFF3EDF7),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = "Kode: ${group.joinCode}",
                        fontSize = 12.sp,
                        color = Color(0xFF6750A4),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}