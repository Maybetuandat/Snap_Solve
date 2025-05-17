package com.example.app_music.domain.utils

object UrlUtils {
    // Base URL của API server
    private val BASE_URL = ApiInstance.baseUrl

    /**
     * Chuyển đổi đường dẫn tương đối thành URL tuyệt đối
     *
     * @param relativePath Đường dẫn tương đối (ví dụ: "/images/image.jpg")
     * @return URL tuyệt đối (ví dụ: "http://example.com/images/image.jpg")
     */
    fun getAbsoluteUrl(relativePath: String?): String {
        if (relativePath == null) return ""

        // Nếu đã là URL tuyệt đối, trả về nguyên bản
        if (relativePath.startsWith("http://") || relativePath.startsWith("https://")) {
            return relativePath
        }

        // Đảm bảo relative path bắt đầu bằng "/"
        val normalizedPath = if (relativePath.startsWith("/")) relativePath else "/$relativePath"

        // Kết hợp base URL và đường dẫn tương đối
        return BASE_URL + normalizedPath
    }
}