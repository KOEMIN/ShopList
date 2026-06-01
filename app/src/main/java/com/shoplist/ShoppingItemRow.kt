package com.shoplist

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ShoppingItemRow(
    item: ShoppingItem,
    primaryColor: Color,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tampilan Checkbox
        Checkbox(
            checked = item.isChecked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = primaryColor,
                uncheckedColor = primaryColor
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Tampilan Nama Barang dengan efek coret jika isChecked = true
        Text(
            text = item.name,
            fontSize = 16.sp,
            style = TextStyle(
                color = if (item.isChecked) Color.LightGray else Color.Black,
                textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
            )
        )
    }
}