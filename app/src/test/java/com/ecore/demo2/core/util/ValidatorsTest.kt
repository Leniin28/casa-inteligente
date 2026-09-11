package com.ecore.demo2.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `validates emails`() {
        assertTrue(Validators.isValidEmail("demo@casa.local"))
        assertTrue(Validators.isValidEmail("  ana.perez@uni.edu  "))
        assertFalse(Validators.isValidEmail("demo"))
        assertFalse(Validators.isValidEmail("demo@"))
    }

    @Test
    fun `validates password length`() {
        assertTrue(Validators.isValidPassword("demo1234"))
        assertFalse(Validators.isValidPassword("12345"))
    }

    @Test
    fun `normalizes backend urls`() {
        assertEquals("http://10.0.2.2:8000/", Validators.normalizeBaseUrl("http://10.0.2.2:8000"))
        assertEquals("http://192.168.1.50:8000/", Validators.normalizeBaseUrl(" http://192.168.1.50:8000/ "))
        assertNull(Validators.normalizeBaseUrl("10.0.2.2:8000"))
        assertNull(Validators.normalizeBaseUrl("ftp://server"))
        assertNull(Validators.normalizeBaseUrl(""))
    }
}
