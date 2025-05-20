package com.example.app_music.domain.utils

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class LocalDateTypeAdapter : TypeAdapter<LocalDate>() {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    override fun write(out: JsonWriter, value: LocalDate?) {
        if (value == null) {
            out.nullValue()
        } else {
            out.value(formatter.format(value))
        }
    }

    override fun read(reader: JsonReader): LocalDate? {
        return when (reader.peek()) {
            com.google.gson.stream.JsonToken.NULL -> {
                reader.nextNull()
                null
            }
            else -> {
                val dateStr = reader.nextString()
                try {
                    LocalDate.parse(dateStr, formatter)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}