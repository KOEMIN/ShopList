package com.shoplist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavType // <-- IMPORT BARU UNTUK TIPE DATA ARGUMEN
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument // <-- IMPORT BARU UNTUK MENANGKAP ARGUMEN

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AppNavigation()
        }
    }
}

@Preview
@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login"
    ) {

        composable("login") {
            LoginPage(navController)
        }

        composable("register") {
            RegisterPage(navController)
        }

        // =======================================================================
        // PERUBAHAN 1: Sesuaikan rute "home" agar menerima aksi klik item grup
        // =======================================================================
        composable("home") {
            // Asumsi: HomePage Anda memiliki fungsi lambda (callback) saat sebuah grup diklik
            HomePage(
                onGroupClick = { id, code, name ->
                    // Berpindah halaman ke shopping_list sambil menyelipkan id dan nama grup
                    navController.navigate("shopping_list/$id/$code/$name")
                }
            )
        }

        // =======================================================================
        // PERUBAHAN 2: Tambahkan rute baru untuk memanggil BelanjaBarengScreen
        // =======================================================================
        composable(
            route = "shopping_list/{groupId}/{groupCode}/{groupName}",
            arguments = listOf(
                navArgument("groupId") { type = NavType.StringType },
                navArgument("groupCode") { type = NavType.StringType },
                navArgument("groupName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Mengambil ekstensi data argumen string yang dikirim oleh HomePage
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            val groupCode = backStackEntry.arguments?.getString("groupCode") ?: ""
            val groupName = backStackEntry.arguments?.getString("groupName") ?: "Belanja Bareng"

            // Memanggil screen baru yang Anda buat
            BelanjaBarengScreen(
                groupId = groupId,
                groupName = groupName,
                groupCode = groupCode,
                onBackClick = {
                    // Logika ketika tombol ArrowBack diklik (kembali ke halaman sebelumnya)
                    navController.popBackStack()
                }
            )
        }
    }
}