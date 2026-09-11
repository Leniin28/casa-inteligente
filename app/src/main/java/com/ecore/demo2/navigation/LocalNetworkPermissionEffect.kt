package com.ecore.demo2.navigation

import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.util.LocalNetworkPermission

/**
 * Pide el permiso de red local cuando la fuente de datos es la API (backend en la laptop).
 * Si el usuario lo deniega, las llamadas fallan y las pantallas muestran el error de conexión.
 */
@Composable
fun LocalNetworkPermissionEffect(dataSource: DataSourceType?) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(dataSource) {
        if (dataSource == null || !LocalNetworkPermission.isRequired(Build.VERSION.SDK_INT, dataSource)) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(context, LocalNetworkPermission.NAME) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) launcher.launch(LocalNetworkPermission.NAME)
    }
}
