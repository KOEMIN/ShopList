package com.shoplist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val message: String = "",
    val timestamp: Timestamp? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    groupId: String,
    groupName: String,
    groupCode: String,
    onBackClick: () -> Unit
) {
    //Memanggil dan menyimpan mesin database Firebase Firestore ke dalam variabel db.
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance() // untuk mengenali pengguna yang sedang login

    val messages = remember {
        mutableStateListOf<ChatMessage>() // untuk mebuat daftae list khusus untuk chat
    }

    var messageText by remember {
        mutableStateOf("") // saat mengetik layar hp akan berubah secara otomatis
    }

    val currentUid = auth.currentUser?.uid ?: "" //mengambil id pengguna yang sedang login

    val lavenderBg = Color(0xFFF3EDF7)
    val primaryPurple = Color(0xFF6750A4)

    // SINKRONISASI PESAN CHAT REAL-TIME
    // Menggunakan DisposableEffect agar listener otomatis berhenti ketika user keluar dari ruang chat
    DisposableEffect(groupId) {
        // Mengakses sub-koleksi 'messages' di bawah dokumen grup, 
        // lalu diurutkan berdasarkan 'timestamp' kirim agar obrolan runtut.
        val listener = db.collection("groups")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    android.util.Log.e("Firestore", "Gagal mengambil pesan", error)
                    return@addSnapshotListener
                }

                // Kosongkan list pesan lama di memory HP agar bisa direfresh dengan daftar utuh terbaru
                messages.clear()

                snapshot?.documents?.forEach { document ->
                    // Konversi dokumen dari Firestore menjadi model data ChatMessage Kotlin
                    val msg = document.toObject(ChatMessage::class.java)
                    if (msg != null) {
                        // Tambahkan pesan ke list state Compose (UI akan langsung merender bubble baru)
                        messages.add(
                            msg.copy(id = document.id)
                        )
                    }
                }
            }

        //onDispose akan dieksekusi ketika pengguna keluar dari halaman atau ketika groupId berganti.
        onDispose {
            // BERSIH-BERSIH: Putuskan pipa langganan chat ke server agar menghemat daya baterai & internet
            listener.remove()
        }
    }

    //Scaffold adalah komponen dasar dari Material Design di Compose
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
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Ketik Pesan...", color = Color.Gray) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                    )
                    //Spacer memberikan jarak kosong secara horizontal
                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    IconButton(
                        onClick = {
                            // Validasi: Cegah pengiriman jika kolom teks kosong atau hanya berisi spasi
                            if (messageText.isBlank()) {
                                return@IconButton
                            }

                            // 1. Menyiapkan data pesan obrolan dalam bentuk Map
                            val data = hashMapOf(
                                "senderId" to currentUid, // ID Pengirim (untuk menentukan letak bubble kiri/kanan)
                                "senderName" to (
                                        auth.currentUser?.displayName
                                            ?: "User" // Mengambil Nama pengguna dari Firebase Auth, jika tidak ada akan menggunakan "User"
                                        ),
                                "message" to messageText.trim(), // Isi Pesan
                                "timestamp" to FieldValue.serverTimestamp() // Waktu kirim sinkron dari server Firebase
                            )

                            // 2. Simpan pesan baru ke sub-koleksi 'messages' di bawah grup chat terpilih
                            //proses pengiriman ke Firebase Firestore.
                            db.collection("groups")
                                .document(groupId)
                                .collection("messages")
                                .add(data) //Menambahkan dokumen data pesan baru.
                                .addOnSuccessListener {
                                    // 3. Jika sukses terkirim, kosongkan kolom ketik pesan
                                    messageText = ""
                                }
                        }
                    ) {
                        Icon(
                            Icons.Default.Send,
                            contentDescription = "Kirim",
                            tint = primaryPurple
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
    // agar dapa menscroll, menggunakan lazycolumn agar hemat memori
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items( //fungsi khusus di dalam LazyColumn untuk mengulang (looping) sebuah daftar data (list).
                items = messages,// untuk menerima variable massages
                key = { it.id } //untukagar saat ada pesan baru masuk atau pesan dihapus,
            // Compose tidak perlu menggambar ulang seluruh daftar chat memberikan identitas unik (ID) untuk setiap baris chat
            ) { message ->

                ChatBubble(
                    message = message,
                    isMine = message.senderId == currentUid,
                    primaryColor = primaryPurple
                )
            }
        }
    }
}
// untuk tampilan chat buble
@Composable
fun ChatBubble(
    message: ChatMessage,// untuk mengambil data user
    isMine: Boolean, // untuk mengecek apakah pesan dari user atau orang lain
    primaryColor: Color
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment =
            if (isMine)
                Alignment.End // jika pesan dari user maka posisi buble chat di kanan
            else
                Alignment.Start // jika pesan dari orang lain maka posisi buble chat di kiri
    ) {

        Text(
            text = message.senderName, //Mengambil data nama pengirim dari variabel message
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(
            modifier = Modifier.height(2.dp)
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color =
                if (isMine)
                    primaryColor
                else
                    Color(0xFFEAEAEA)
        ) {

            Text(
                text = message.message,
                modifier = Modifier.padding(12.dp),
                color =
                    if (isMine)
                        Color.White
                    else
                        Color.Black
            )
        }
    }
}