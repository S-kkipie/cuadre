package pe.aido.cuadre

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { Home() } }
    }
}

@Composable
private fun Home() {
    val context = LocalContext.current
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Cuadre", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Para confirmar pagos reales de Yape/Plin, activa el acceso a notificaciones.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = {
                // Prominent disclosure must be shown BEFORE this (Play policy). TODO: disclosure screen.
                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }) { Text("Activar acceso a notificaciones") }
        }
    }
}
