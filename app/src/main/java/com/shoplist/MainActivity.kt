package com.shoplist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // =======================================================================
    // PERUBAHAN BARU: Cek status login user saat aplikasi dibuka
    // =======================================================================
    val auth = FirebaseAuth.getInstance()
    val startingScreen = if (auth.currentUser != null) {
        "home" // Jika user sudah login, langsung buka Home
    } else {
        "login" // Jika belum, buka halaman Login
    }

    NavHost(
        navController = navController,
        startDestination = startingScreen // <-- Gunakan variabel startingScreen di sini
    ) {

        composable("login") {
            LoginPage(navController)
        }

        composable("register") {
            RegisterPage(navController)
        }

        composable("home") {
            HomePage(
                onGroupClick = { id, code, name ->
                    navController.navigate("shopping_list/$id/$code/$name")
                },
                // ==========================================================
                // TAMBAHAN BARU: Fungsi untuk menangani klik tombol Logout
                // ==========================================================
                onLogoutClick = {
                    // 1. Hapus sesi di Firebase
                    FirebaseAuth.getInstance().signOut()

                    // 2. Arahkan kembali ke halaman login, dan hapus SEMUA riwayat halaman
                    navController.navigate("login") {
                        popUpTo(navController.graph.id) {
                            inclusive = true
                        }
                    }
                }
            )
        }

        composable(
            route = "shopping_list/{groupId}/{groupCode}/{groupName}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("groupCode") { type = NavType.StringType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val groupCode = backStackEntry.arguments?.getString("groupCode") ?: ""
            val groupName = backStackEntry.arguments?.getString("groupName") ?: "Belanja Bareng"

            BelanjaBarengScreen(
                groupId = groupId,
                groupName = groupName,
                groupCode = groupCode,
                onBackClick = {
                    navController.popBackStack()
                },
                onChatClick = { id, name, code ->
                    navController.navigate(
                        "chat/$id/$name/$code"
                    )
                }
            )
        }

        composable(
            route = "chat/{groupId}/{groupName}/{groupCode}"
        ) { backStackEntry ->

            ChatScreen(
                groupId = backStackEntry.arguments?.getString("groupId") ?: "",
                groupName = backStackEntry.arguments?.getString("groupName") ?: "",
                groupCode = backStackEntry.arguments?.getString("groupCode") ?: "",
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}