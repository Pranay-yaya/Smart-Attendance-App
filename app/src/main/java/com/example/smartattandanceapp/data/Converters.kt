package com.example.smartattendanceapp.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for [FloatArray].
 *
 * Room cannot store FloatArray directly, so we serialise it to a JSON string
 * (via Gson) when writing to the DB and deserialise it back when reading.
 *
 * Registered in [AppDatabase] via @TypeConverters(Converters::class).
 */
class Converters {

    private val gson = Gson()

    /** FloatArray → JSON string  (stored in DB column) */
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String = gson.toJson(value.toList())

    /** JSON string → FloatArray  (read from DB column) */
    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson<List<Float>>(value, type).toFloatArray()
    }
}