package com.ssbmax.core.data.local

import androidx.room.TypeConverter
import com.ssbmax.core.domain.model.GenderTag

class RoomTypeConverters {

    @TypeConverter
    fun genderTagToString(value: GenderTag): String = value.name

    @TypeConverter
    fun stringToGenderTag(value: String): GenderTag =
        GenderTag.entries.firstOrNull { it.name == value } ?: GenderTag.MIXED
}
