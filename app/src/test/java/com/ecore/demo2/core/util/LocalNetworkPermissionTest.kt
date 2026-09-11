package com.ecore.demo2.core.util

import com.ecore.demo2.core.model.DataSourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalNetworkPermissionTest {

    @Test
    fun `required only in api mode on android 17 or newer`() {
        assertTrue(LocalNetworkPermission.isRequired(37, DataSourceType.API))
        assertTrue(LocalNetworkPermission.isRequired(38, DataSourceType.API))
    }

    @Test
    fun `not required in demo mode`() {
        assertFalse(LocalNetworkPermission.isRequired(37, DataSourceType.DEMO))
    }

    @Test
    fun `not required on older android versions`() {
        assertFalse(LocalNetworkPermission.isRequired(36, DataSourceType.API))
        assertFalse(LocalNetworkPermission.isRequired(29, DataSourceType.API))
    }
}
