package com.ecore.demo2.core.util

import com.ecore.demo2.core.model.DataSourceType

/**
 * Desde Android 17 (API 37) una app necesita el permiso de red local (grupo "Dispositivos cercanos")
 * para conectarse a direcciones de la red local, como 10.0.2.2 en el emulador o la IP LAN de la laptop.
 * Sin él, las conexiones al backend local caducan sin llegar al servidor.
 */
object LocalNetworkPermission {
    const val NAME = "android.permission.ACCESS_LOCAL_NETWORK"
    const val MIN_SDK = 37

    fun isRequired(sdkInt: Int, dataSource: DataSourceType): Boolean =
        dataSource == DataSourceType.API && sdkInt >= MIN_SDK
}
