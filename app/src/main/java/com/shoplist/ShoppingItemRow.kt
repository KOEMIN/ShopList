package com.shoplist

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    primaryColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    onDeleteClick: () -> Unit, // Parameter aksi hapus item
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween // Mendorong ikon hapus ke paling kanan
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Menyesuaikan kembali dengan parameter 'isChecked' milik model kamu
            Checkbox(
                checked = item.isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = primaryColor,
                    uncheckedColor = primaryColor
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Menyesuaikan kembali dengan parameter 'name' milik model kamu
            Text(
                text = item.name,
                fontSize = 16.sp,
                style = TextStyle(
                    color = if (item.isChecked) Color.LightGray else Color.Black,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
            )
        }

        // Tombol Tempat Sampah Merah untuk menghapus item
        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Hapus Barang",
                tint = Color.Red
            )
        }
    }
}