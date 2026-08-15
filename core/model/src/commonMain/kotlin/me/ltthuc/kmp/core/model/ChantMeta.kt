package me.ltthuc.kmp.core.model

data class ChantMeta(
    val lessonId: String,
    val startMs: Long,
    val speedMs: Long,
    val edgeSpeedMs: Long,
    /**
     * Thời lượng mỗi thẻ từ trên ChantScreen. Bốn thẻ chạy hết là tới thẻ celebration,
     * nên `4 × slideMs` nên bằng [startMs] — lúc audio bắt đầu đọc nhanh danh sách từ.
     * Lesson nào không khai trong JSON thì dùng mặc định của màn hình.
     */
    val slideMs: Long?,
    /**
     * Mốc KẾT THÚC của từng thẻ từ, tính từ lúc audio bắt đầu (4 mốc, tăng dần).
     * Chính xác hơn [slideMs] vì bốn câu dạy dài không đều nhau — đo `cap` được
     * 2597/2466/2371/2277ms, cộng thêm 318ms lặng đầu file, nên một con số duy nhất
     * luôn cắt cụt câu đầu. Ưu tiên hơn [slideMs]; thiếu thì rơi về [slideMs].
     */
    val slideStartsMs: List<Long>?,
    val wordTimings: List<WordTiming>,
)
