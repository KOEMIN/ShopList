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
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()

    val messages = remember {
        mutableStateListOf<ChatMessage>()
    }

    var messageText by remember {
        mutableStateOf("")
    }

    val currentUid = auth.currentUser?.uid ?: ""

    val lavenderBg = Color(0xFFF3EDF7)
    val primaryPurple = Color(0xFF6750A4)

    DisposableEffect(groupId) {

        val listener = db.collection("groups")
            .document(groupId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    android.util.Log.e(
                        "Firestore",
                        "Gagal mengambil pesan",
                        error
                    )
                    return@addSnapshotListener
                }

                messages.clear()

                snapshot?.documents?.forEach { document ->

                    val msg = document.toObject(ChatMessage::class.java)

                    if (msg != null) {
                        messages.add(
                            msg.copy(id = document.id)
                        )
                    }
                }
            }

        onDispose {
            listener.remove()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = groupName,
                            fontSize = 18.sp
                        )

                        Text(
                            text = "Kode: $groupCode",
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = lavenderBg
                )
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
                        onValueChange = {
                            messageText = it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text("Ketik pesan...")
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Send
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(
                        modifier = Modifier.width(8.dp)
                    )

                    IconButton(
                        onClick = {

                            if (messageText.isBlank()) {
                                return@IconButton
                            }

                            val data = hashMapOf(
                                "senderId" to currentUid,
                                "senderName" to (
                                        auth.currentUser?.displayName
                                            ?: "User"
                                        ),
                                "message" to messageText.trim(),
                                "timestamp" to FieldValue.serverTimestamp()
                            )

                            db.collection("groups")
                                .document(groupId)
                                .collection("messages")
                                .add(data)
                                .addOnSuccessListener {
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

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            items(
                items = messages,
                key = { it.id }
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

@Composable
fun ChatBubble(
    message: ChatMessage,
    isMine: Boolean,
    primaryColor: Color
) {

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment =
            if (isMine)
                Alignment.End
            else
                Alignment.Start
    ) {

        Text(
            text = message.senderName,
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