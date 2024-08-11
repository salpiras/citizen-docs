package com.salpiras.citizendocs.model.local.db

import androidx.room.TypeConverter
import java.time.Month
import java.time.Year
import java.util.Date

class Converters {
  @TypeConverter
  fun fromTimestamp(value: Long?): Date? {
    return value?.let { Date(it) }
  }

  @TypeConverter
  fun dateToTimestamp(date: Date?): Long? {
    return date?.time
  }

  @TypeConverter
  fun monthFromInt(value: Int?): Month? {
    return value?.let { Month.entries[it] }
  }

  @TypeConverter
  fun monthToNumber(month: Month?): Int? {
    return month?.value
  }

  @TypeConverter
  fun yearFromLong(value: Int?): Year? {
    return value?.let { Year.of(value) }
  }

  @TypeConverter
  fun yearToLong(year: Year?): Int? {
    return year?.value
  }

}