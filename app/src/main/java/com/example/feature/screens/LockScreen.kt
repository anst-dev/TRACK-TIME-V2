package com.example.feature.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun LockScreen(
    correctPinCode: String,
    onSuccess: () -> Unit
) {
    var enteredText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ChronoBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = ChronoPrimary,
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Yêu cầu mã PIN Bảo mật",
            color = ChronoText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Vui lòng nhập mã để truy cập dữ liệu quản lý thời gian",
            color = ChronoTextDim,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // DOTS Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..4) {
                val filled = enteredText.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            if (filled) ChronoPrimary else Color.White.copy(alpha = 0.1f),
                            RoundedCornerShape(50)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (errorMessage.isNotEmpty()) {
            Text(text = errorMessage, color = QuadrantRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(40.dp))

        // GRID Numpad
        val buttons = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("C", "0", "◀")
        )

        buttons.forEach { row ->
            Row(
                modifier = Modifier.padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { d ->
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(ChronoSurface, RoundedCornerShape(50))
                            .clickable {
                                errorMessage = ""
                                if (d == "C") {
                                    enteredText = ""
                                } else if (d == "◀") {
                                    if (enteredText.isNotEmpty()) {
                                        enteredText = enteredText.dropLast(1)
                                    }
                                } else {
                                    if (enteredText.length < 4) {
                                        enteredText += d
                                        if (enteredText.length == 4) {
                                            if (enteredText == correctPinCode) {
                                                onSuccess()
                                            } else {
                                                errorMessage = "Mã PIN không khớp. Vui lòng nhập lại."
                                                enteredText = ""
                                            }
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = d,
                            color = ChronoText,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
