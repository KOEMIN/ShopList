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
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
//deklarasi fungsi dan parameter
fun BelanjaBarengScreen(
    groupId: String,
    groupName: String,
    groupCode: String,
    onBackClick: () -> Unit,
    onChatClick: (String, String, String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = remember { database.shoppingItemDao() }
    val coroutineScope = rememberCoroutineScope()

    val shoppingList by dao.getItemsByGroup(groupId).collectAsState(initial = emptyList())
    var newItemText by remember { mutableStateOf("") }
    var showHistoryDialog by remember { mutableStateOf(false) }
    val lavenderBg = Color(0xFFF3EDF7)
    val primaryPurple = Color(0xFF6750A4)

    // SINKRONISASI DATA REAL-TIME (BARANG BELANJAAN)
    // DisposableEffect dipakai agar listener otomatis mati saat user keluar dari halaman ini.
    // Fungsi di dalam blok ini akan dieksekusi saat halaman pertama kali dibuka, atau jika nilai
    // groupId berubah (misalnya pengguna berpindah ke grup belanja lain).
    DisposableEffect(groupId) {
        // Fungsi ini membuat aplikasi berlangganan aliran data secara real-time.
        // Setiap kali ada perubahan di server (barang ditambah, dihapus, atau dicentang oleh orang lain),
        // Firebase akan langsung mengirimkan data terbaru (snapshot) ke aplikasi secara otomatis.
        val listenerRegistration = db.collection("groups")
            .document(groupId)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                //Jika terjadi masalah saat mengambil data (misalnya koneksi internet terputus atau izin baca ditolak oleh aturan keamanan Firebase),
                // sistem akan mengirimkan error.
                //Log.e bertugas mencetak pesan error tersebut secara tersembunyi di konsol
                //return@addSnapshotListener digunakan untuk langsung menghentikan proses pembaruan data tersebut karena datanya memang gagal diambil.
                if (error != null) {
                    android.util.Log.e("Firestore", "Gagal mengambil data barang", error)
                    return@addSnapshotListener
                }
                //Memastikan bahwa data dari Firebase (snapshot) benar-benar ada isinya sebelum diproses.
                if (snapshot != null) {
                    coroutineScope.launch {
                        //snapshot.documents berisi daftar mentah dokumen dari Firebase.
                        //mapNotNull untuk mengubah setiap dokumen tersebut menjadi objek ShoppingItem.
                        val localItems = snapshot.documents.mapNotNull { document ->
                            // Mengambil data mentah (yang formatnya seperti JSON dari Firebase)
                            // dan diubah menjadi objek Kotlin bernama ShoppingItem.
                            val item = document.toObject(ShoppingItem::class.java)
                            item?.let {
                                LocalShoppingItem(
                                    id = document.id,
                                    name = it.name,
                                    isChecked = it.isChecked,
                                    groupId = groupId
                                )
                            }
                        }
                        // Bersihkan data lama untuk grup ini dan masukkan data terupdate ke Room
                        dao.deleteAllItemsByGroup(groupId)
                        dao.insertItems(localItems)
                    }
                }
            }
        //onDispose hanya akan dieksekusi ketika pengguna keluar dari halaman (layar dihancurkan) atau ketika groupId berganti.
        onDispose {
            // BERSIH-BERSIH: Putuskan pipa langganan ke server agar tidak memakan RAM dan kuota internet di latar belakang
            listenerRegistration.remove()
        }
    }
// untuk mrngecek variabel showHistoryDialog.
// Jika bernilai true, maka baris kode di dalamnya dijalankan.
    if (showHistoryDialog) {
        HistoryDialog(
            groupId = groupId,
            db = db,
            //onDismiss = { showHistoryDialog = false }: Ini adalah aksi jika user menekan tombol "Tutup"
            // atau mengeklik area di luar pop-up. Variabel dikembalikan menjadi false,
            // sehingga pop-up otomatis menghilang dari layar.
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

                    IconButton(
                        onClick = {
                            if (newItemText.isNotBlank()) {
                                // 1. Bikin objek Map berisi nama barang dan status awal (belum dibeli)
                                val itemData = hashMapOf(
                                    "name" to newItemText.trim(),
                                    "isChecked" to false
                                )
                                // 2. Simpan barang baru ke sub-koleksi 'items' di bawah grup ini di Firestore
                                db.collection("groups")
                                    .document(groupId)
                                    .collection("items")
                                    .add(itemData)
                                    .addOnSuccessListener {
                                        // 3. Jika berhasil tersimpan, kosongkan kolom input teks
                                        newItemText = ""
                                    }
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
        // komponen Jetpack Compose untuk membuat daftar yang bisa digulir (scrollable list)
        //untuk menghemat ram
        LazyColumn(
            modifier = Modifier //mengatur tata letak
                .fillMaxSize() //mengatur ukuran agar memenuhi layar penuh yang tersedia.
                .padding(paddingValues)//mengatur jarak antar item
                .background(Color.White)//mengatur warna latar belakang
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)//mengatur jarak antar item
        ) {
            // PERBAIKAN: Menggunakan objek data list yang benar (shoppingList)
            //Memerintahkan LazyColumn untuk mengulang (melakukan loop) sebanyak jumlah
            // barang yang ada di dalam variabel shoppingList
            items(items = shoppingList, key = { it.id }) { item ->
                val uiItem = remember(item) {
                    ShoppingItem(
                        id = item.id,
                        name = item.name,
                        isChecked = item.isChecked
                    )
                }
                //kode ini untuk memanggil komponen khusus bernama ShoppingItemRow
                ShoppingItemRow(
                    item = uiItem,
                    primaryColor = primaryPurple,
                    onCheckedChange = { isChecked ->
                        // UPDATE SATU FIELD: Ketika kotak centang diklik, update status 'isChecked' saja di Firestore
                        db.collection("groups")
                            .document(groupId)
                            .collection("items")
                            .document(item.id)
                            .update("isChecked", isChecked)
                    },
                    onDeleteClick = {
                        // LOGIKA HAPUS DENGAN RIWAYAT (WRITE BATCH):
                        //dua alamat surat (referensi). itemRef adalah alamat barang tersebut di daftar aktif.
                        // historyRef adalah alamat baru untuk barang tersebut di dalam "tong sampah" atau riwayat (history).
                        val itemRef = db.collection("groups").document(groupId).collection("items").document(item.id)
                        val historyRef = db.collection("groups").document(groupId).collection("history").document(item.id)

                        // Menggunakan Batch agar kedua proses (copy ke history & delete dari items aktif)
                        // sukses secara bersamaan (atomik).
                        db.runBatch { batch ->
                            // 1. Salin data barang ke sub-koleksi 'history'
                            batch.set(historyRef, uiItem)
                            // 2. Hapus dokumen barang dari daftar aktif 'items'
                            batch.delete(itemRef)
                        // jika proses batch gagal dapat ditangani agar dapat terlacak kegagalannya
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
    //kode ini membuat sebuah state berupa daftar kosong untuk menampung barang-barang yang ada di riwayat.
    val historyItems = remember { mutableStateListOf<ShoppingItem>() }

    // Ambil data dari sub-collection "history"
    // Fungsi Compose yang digunakan untuk menjalankan suatu proses (seperti mengambil data)
    LaunchedEffect(groupId) {
        db.collection("groups")
            .document(groupId)
            .collection("history")
            .get() // "menarik" data saat itu juga sekali saja.
            .addOnSuccessListener { snapshot ->
                historyItems.clear()// mengosongkan data di memori hp agar tidak menumpuk
                for (document in snapshot.documents) {
                    val item = document.toObject(ShoppingItem::class.java)?.copy(id = document.id)
                    if (item != null) {
                        historyItems.add(item) //Memasukkan barang tersebut ke dalam memori historyItems,
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
                //Membuat daftar yang bisa di-scroll ke atas/bawah jika riwayatnya sangat panjang.
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(historyItems) { item ->//Mengulang setiap barang di riwayat untuk dicetak ke layar.
                        Text(
                            text = "• ${item.name}",
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