package com.example.smartattandanceapp.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromFloatArray(value: FloatArray): String = Gson().toJson(value.toList())

    @TypeConverter
    fun toFloatArray(value: String): FloatArray {
        val type = object : TypeToken<List<Float>>() {}.type
        return Gson().fromJson<List<Float>>(value, type).toFloatArray()
    }
}