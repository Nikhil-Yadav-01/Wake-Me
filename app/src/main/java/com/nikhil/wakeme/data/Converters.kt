package com.nikhil.wakeme.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromSetOfInt(set: Set<Int>): String {
        return Gson().toJson(set)
    }

    @TypeConverter
    fun toSetOfInt(json: String): Set<Int> {
        val type = object : TypeToken<Set<Int>>() {}.type
        return Gson().fromJson(json, type)
    }
}
