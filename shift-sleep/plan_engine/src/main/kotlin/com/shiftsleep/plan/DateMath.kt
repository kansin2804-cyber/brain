package com.shiftsleep.plan

import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal object DateMath {
    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun parse(date: String): LocalDate = LocalDate.parse(date, iso)

    fun format(date: LocalDate): String = date.format(iso)

    fun plusDays(date: String, days: Int): String =
        format(parse(date).plusDays(days.toLong()))

    fun minusDays(date: String, days: Int): String =
        format(parse(date).minusDays(days.toLong()))
}
