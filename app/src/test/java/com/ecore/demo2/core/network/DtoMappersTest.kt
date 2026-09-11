package com.ecore.demo2.core.network

import com.ecore.demo2.core.model.DataSourceType
import com.ecore.demo2.core.model.DeviceStatus
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.network.dto.DashboardDto
import com.ecore.demo2.core.network.dto.DeviceDto
import com.ecore.demo2.core.network.mapper.toDomain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

/** Verifica que el JSON snake_case del backend se decodifica al modelo de dominio. */
class DtoMappersTest {

    @Test
    fun `decodes dashboard json from the backend`() {
        val json = """
            {
              "electricity": {"timestamp": "2026-09-11T18:30:00Z", "voltage": 12.1, "current": 1.03,
                              "power": 12.5, "energy_today_kwh": 0.182},
              "water": {"timestamp": "2026-09-11T18:30:00Z", "flow_liters_per_minute": 1.2, "liters_today": 184.0},
              "active_devices": 3,
              "unread_alerts": 2,
              "budgets": [{"id": "electricity", "resource_type": "electricity", "limit": 5.0,
                           "current_usage": 2.1, "estimated_final_usage": 5.8,
                           "estimated_limit_date": "2026-09-26",
                           "period_start": "2026-09-01", "period_end": "2026-09-30"}],
              "system_status": {"backend_connected": true, "esp32_connected": false,
                                "last_update": "2026-09-11T18:30:00.123456Z", "data_source": "demo"},
              "unknown_field": "se ignora"
            }
        """.trimIndent()

        val dashboard = NetworkJson.decodeFromString<DashboardDto>(json).toDomain()

        assertEquals(Instant.parse("2026-09-11T18:30:00Z"), dashboard.electricity.timestamp)
        assertEquals(0.182, dashboard.electricity.energyTodayKwh, 0.0)
        assertEquals(1.2, dashboard.water.flowLitersPerMinute, 0.0)
        assertEquals(ResourceType.ELECTRICITY, dashboard.budgets.single().resourceType)
        assertEquals(LocalDate.of(2026, 9, 26), dashboard.budgets.single().estimatedLimitDate)
        assertEquals(DataSourceType.API, dashboard.systemStatus.dataSource)
        assertEquals("demo", dashboard.systemStatus.backendMode)
    }

    @Test
    fun `unknown enum values fall back safely`() {
        val json = """{"id": "x", "name": "Nuevo", "type": "robot", "estimated_power_watts": 3.0,
                       "status": "probably_active"}"""

        val device = NetworkJson.decodeFromString<DeviceDto>(json).toDomain()

        assertEquals(DeviceStatus.PROBABLY_ACTIVE, device.status)
        assertEquals("OTHER", device.type.name)
        assertNull(device.confidence)
    }
}
