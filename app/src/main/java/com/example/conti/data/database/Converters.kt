package com.example.conti.data.database

import androidx.room.TypeConverter
import java.time.LocalDate

/**
 * TypeConverters per Room Database.
 *
 * Room non supporta nativamente i tipi Java 8+ Time API (LocalDate, LocalDateTime, ecc.),
 * quindi dobbiamo fornire dei converter per trasformare questi tipi in tipi primitivi
 * che Room può salvare nel database SQLite.
 *
 * Conversione:
 * - LocalDate → Long (epoch day: numero di giorni dal 1970-01-01)
 * - Long → LocalDate
 */
class Converters {

    /**
     * Converte un LocalDate in Long (epoch day)
     * @param date LocalDate da convertire
     * @return Numero di giorni dal 1970-01-01, o null se date è null
     */
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    /**
     * Converte un Long (epoch day) in LocalDate
     * @param epochDay Numero di giorni dal 1970-01-01
     * @return LocalDate corrispondente, o null se epochDay è null
     */
    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? {
        return epochDay?.let { LocalDate.ofEpochDay(it) }
    }
}