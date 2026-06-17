package com.example.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Trang chủ")
    object List : Screen("list", "Danh sách")
    object Stats : Screen("stats", "Báo cáo")
    object Sync : Screen("sync", "Đồng bộ")
    object Ai : Screen("ai_insights", "AI Phân tích")
}
