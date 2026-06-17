package com.example.feature.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.task.TaskViewModel
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    viewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    val isPinEnabled by viewModel.isPinEnabled.collectAsState()
    val pinCode by viewModel.pinCode.collectAsState()

    var isPinToggled by remember { mutableStateOf(isPinEnabled) }
    var pinValue by remember { mutableStateOf(pinCode) }

    var isProPurchased by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ChronoSurface),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .wrapContentHeight()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cài đặt & Bảo mật",
                    color = ChronoText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = ChronoText)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // PIN security setup row
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ChronoSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = ChronoPrimary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Bảo mật bằng mã PIN",
                                color = ChronoText,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Switch(
                            checked = isPinToggled,
                            onCheckedChange = {
                                isPinToggled = it
                                if (!it) {
                                    viewModel.savePinCode("", false)
                                }
                            }
                        )
                    }

                    if (isPinToggled) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Nhập mã khóa PIN (4 chữ số):",
                            color = ChronoTextDim,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = {
                                if (it.length <= 4) {
                                    pinValue = it
                                    viewModel.savePinCode(it, true)
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ChronoText,
                                unfocusedTextColor = ChronoText,
                                focusedBorderColor = ChronoPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Play Billing Config Row (PRO Upgrade)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = ChronoSurfaceElevated),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payment, contentDescription = null, tint = Color.Yellow)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Nâng cấp gói PRO",
                                    color = ChronoText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                        text = if (isProPurchased) "Đã đăng ký tài khoản PRO" else "Giá chỉ 49,000 VND / Tháng",
                                    color = ChronoTextDim,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        if (isProPurchased) {
                            Box(
                                modifier = Modifier
                                    .background(QuadrantGreen.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Đã mở", color = QuadrantGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { isProPurchased = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Mua ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ChronoPrimary, contentColor = ChronoOnPrimary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Hoàn tất", fontWeight = FontWeight.Bold)
            }
        }
    }
}
