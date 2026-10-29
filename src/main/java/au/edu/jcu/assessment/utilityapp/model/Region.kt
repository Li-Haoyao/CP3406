package au.edu.jcu.assessment.utilityapp.model
import java.util.Locale

enum class Region(val code: String) {
    CHINA("CN"), MALAYSIA("MY"), SINGAPORE("SG");
    companion object {
        fun fromValue(value: String?): Region? = when (value?.trim()?.lowercase(Locale.ROOT)) {
            "cn", "中国", "中国大陆", "china", "mainland china" -> CHINA
            "my", "马来西亚", "malaysia" -> MALAYSIA
            "sg", "新加坡", "singapore" -> SINGAPORE
            else -> null
        }
    }
}
