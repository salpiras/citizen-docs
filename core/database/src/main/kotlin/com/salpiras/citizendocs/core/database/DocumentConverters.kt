package com.salpiras.citizendocs.core.database

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

/**
 * Only the conversions actually used by [DocumentEntity]. The previous version also carried
 * `Month` and `Year` converters that no entity referenced.
 */
internal class DocumentConverters {
    // kotlinx-datetime 0.8 widened epoch days to Long, so no narrowing conversion is needed.
    @TypeConverter
    fun toLocalDate(epochDays: Long?): LocalDate? = epochDays?.let(LocalDate::fromEpochDays)

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDays()

    @TypeConverter
    fun toInstant(epochMillis: Long?): Instant? = epochMillis?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun fromInstant(instant: Instant?): Long? = instant?.toEpochMilliseconds()
}
