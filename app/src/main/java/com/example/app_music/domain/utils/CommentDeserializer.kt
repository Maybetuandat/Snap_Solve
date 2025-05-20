package com.example.app_music.domain.utils

import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.example.app_music.domain.model.Comment
import java.lang.reflect.Type

class CommentDeserializer : JsonDeserializer<Comment> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Comment {
        val jsonObject = json?.asJsonObject ?: throw JsonParseException("Invalid JSON")

        val id = jsonObject.get("id").asLong
        val content = jsonObject.get("content").asString
        val images = if (jsonObject.has("images") && !jsonObject.get("images").isJsonNull) {
            val imageType = object : TypeToken<List<String>>() {}.type
            context?.deserialize<List<String>>(jsonObject.get("images"), imageType) ?: emptyList()
        } else {
            emptyList()
        }

        val createDate = java.time.LocalDate.parse(jsonObject.get("createDate").asString)
        val user = context?.deserialize<com.example.app_music.domain.model.User>(
            jsonObject.get("user"),
            com.example.app_music.domain.model.User::class.java
        ) ?: throw JsonParseException("User is required")

        val replyCount = jsonObject.get("replyCount").asInt

        // Handle replies safely
        val replies = if (jsonObject.has("replies") && !jsonObject.get("replies").isJsonNull) {
            val replyType = object : TypeToken<List<Comment>>() {}.type
            context?.deserialize<List<Comment>>(jsonObject.get("replies"), replyType) ?: emptyList()
        } else {
            emptyList()
        }

        return Comment(
            id = id,
            content = content,
            images = images,
            createDate = createDate,
            user = user,
            parentComment = null, // Backend không trả về parentComment để tránh circular reference
            replies = replies,
            replyCount = replyCount
        )
    }
}