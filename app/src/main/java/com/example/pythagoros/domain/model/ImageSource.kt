package com.example.pythagoros.domain.model

/**
 * Снимок условия, отданный на распознавание.
 *
 * Домен не знает про `android.net.Uri` и файлы, поэтому кадр описан ссылкой,
 * которую разворачивает слой data. [origin] нужен потому, что на экране камеры
 * два входа — затвор и выбор из галереи.
 */
data class ImageSource(
    val reference: String,
    val origin: Origin,
) {
    enum class Origin { Camera, Gallery }
}
