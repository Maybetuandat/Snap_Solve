package com.example.app_music.domain.utils

import android.util.Log

object UrlUtils {
    // Base URL của API server
    private val BASE_URL = ApiInstance.baseUrl

    /**
     * Chuyển đổi đường dẫn tương đối thành URL tuyệt đối
     * Nếu là URL tuyệt đối thì thay thế IP:port cũ bằng IP:port hiện tại
     *
     * @param relativePath Đường dẫn tương đối hoặc URL tuyệt đối
     * @return URL tuyệt đối với IP:port hiện tại
     */
    fun getAbsoluteUrl(relativePath: String?): String {
        Log.d("hiep:", relativePath ?:"null")
        if (relativePath == null) return ""

        // Nếu đã là URL tuyệt đối, normalize nó
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return normalizeUrl(relativePath)
        }

        // Đảm bảo relative path bắt đầu bằng "/"
        val normalizedPath = if (relativePath.startsWith("/")) relativePath else "/$relativePath"
        // Kết hợp base URL và đường dẫn tương đối
        return BASE_URL + normalizedPath
    }

    /**
     * Thay thế IP:port cũ trong URL bằng IP:port hiện tại từ ApiInstance
     *
     * @param fullUrl URL đầy đủ có thể chứa IP:port cũ
     * @return URL với IP:port hiện tại
     */
    private fun normalizeUrl(fullUrl: String): String {
        // Regex để tìm pattern http://IP:PORT
        val urlPattern = Regex("^(https?://)([^/]+)(/.*)$")
        val match = urlPattern.find(fullUrl)
        var result = ""
        if (match != null) {
            val protocol = match.groupValues[1] // http:// hoặc https://
            val oldHostPort = match.groupValues[2] // IP:port cũ
            val path = match.groupValues[3] // đường dẫn còn lại

            // Lấy host:port hiện tại từ BASE_URL
            val currentHostPort = BASE_URL.removePrefix("http://").removePrefix("https://")

            // Tạo URL mới với host:port hiện tại
            result = protocol + currentHostPort + path
        } else {
            // Nếu không match pattern, trả về URL gốc
            result = fullUrl
        }
        Log.d("result", result)
        return result
    }
}