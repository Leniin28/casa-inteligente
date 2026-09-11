package com.ecore.demo2.core.repository.demo

import com.ecore.demo2.core.model.HistoryPeriod
import com.ecore.demo2.core.model.MessageRole
import com.ecore.demo2.core.model.ResourceType
import com.ecore.demo2.core.util.AppException
import com.ecore.demo2.testutil.FakeSessionStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class DemoSmartHomeDataSourceTest {

    private val clock = Clock.fixed(Instant.parse("2026-09-11T18:30:00Z"), ZoneOffset.UTC)
    private val dataSource = DemoSmartHomeDataSource(clock, FakeSessionStore())

    @Test
    fun `demo login accepts the demo account and rejects invalid credentials`() = runTest {
        val session = dataSource.login("demo@casa.local", "demo1234")
        assertEquals("demo@casa.local", session.user.email)

        try {
            dataSource.login("demo@casa.local", "123")
            fail("Debería rechazar una contraseña corta")
        } catch (_: AppException) {
        }
    }

    @Test
    fun `dashboard is consistent with the individual endpoints`() = runTest {
        val dashboard = dataSource.getDashboard()

        assertEquals(dataSource.getCurrentElectricity(), dashboard.electricity)
        assertEquals(dataSource.getAlerts().count { !it.read }, dashboard.unreadAlerts)
        assertEquals(2, dashboard.budgets.size)
    }

    @Test
    fun `saving a budget changes its limit`() = runTest {
        dataSource.saveBudget(ResourceType.WATER, 1234.0, "water")

        val water = dataSource.getBudgets().first { it.resourceType == ResourceType.WATER }
        assertEquals(1234.0, water.limit, 0.0)
    }

    @Test
    fun `marking an alert as read is remembered`() = runTest {
        val unread = dataSource.getAlerts().first { !it.read }
        dataSource.markAlertRead(unread.id)

        assertTrue(dataSource.getAlerts().first { it.id == unread.id }.read)
    }

    @Test
    fun `assistant answers questions about consumption`() = runTest {
        val reply = dataSource.sendAssistantMessage("¿Cuánta electricidad consumo?", emptyList())

        assertEquals(MessageRole.ASSISTANT, reply.role)
        assertTrue(reply.text.contains(" W"))
    }

    @Test
    fun `history sizes follow the requested period`() = runTest {
        assertEquals(24, dataSource.getHistory(HistoryPeriod.DAY).size)
        assertEquals(7, dataSource.getElectricityHistory(HistoryPeriod.WEEK).size)
    }
}
