package com.example.warehouse.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {
    fun formatVnd(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))
        return formatter.format(amount)
    }

    fun formatNumber(number: Number): String {
        val formatter = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"))
        return formatter.format(number)
    }
}
