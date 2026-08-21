package me.ltthuc.kmp.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import me.ltthuc.kmp.core.resource.Res
import me.ltthuc.kmp.core.resource.quicksand_bold
import me.ltthuc.kmp.core.resource.quicksand_regular
import org.jetbrains.compose.resources.Font

/**
 * Font cho MỌI chữ cái và từ thuộc phần học — thẻ chữ, blend, chant, từ vựng, mini game,
 * bản đồ bài học. KHÔNG dùng cho chrome (nút, menu, cài đặt): chỗ đó giữ font mặc định.
 *
 * Vì sao cần: font mặc định của Material3 (Roboto) vẽ chữ `a` dạng double-storey — có móc
 * trên đầu. Nhưng màn tracing lại dựng chữ `a` từ hai nét rời, một bát tròn mở phải cộng
 * một nét thẳng — dạng single-storey, cũng là dạng sách Oxford Phonics World dùng. Bé nhìn
 * thấy một dáng chữ mà tập viết một dáng khác. Quicksand là font hình học bo tròn nên cho
 * chữ `a` đúng dạng viết tay — bát tròn cộng nét thẳng, không móc.
 *
 * Chỉ mỗi chữ `a` lệch, nhưng dùng nguyên font cho phần học thì nét đều hơn là ghép hai
 * font trong cùng một từ — chữ `a` lấy từ font khác sẽ lệch bề dày và chiều cao so với
 * chữ bên cạnh.
 *
 * Nạp cả nét 400 lẫn 700 vì màn ghép vần đặt chữ đậm. Font một nét (Didact Gothic, ABeeZee)
 * thì Compose phải làm đậm giả — tô dày viền ra, chỗ nét giao nhau dính lại thành mảng, rõ
 * nhất ở cỡ chữ lớn. Nếu sau này đổi font khác, kiểm tra font đó CÓ nét 700 thật hay không.
 *
 * Hai file này TRÍCH RA từ bản biến thiên `Quicksand[wght].ttf` của kho google/fonts (Quicksand
 * không phát hành file tĩnh). Nạp thẳng file biến thiên thì Android chỉ lấy nét mặc định rồi
 * làm đậm giả, tức mất đúng thứ ta cần. Muốn dựng lại:
 *     pip install fonttools
 *     python -c "from fontTools.varLib.instancer import instantiateVariableFont; ..."  # wght=400 và 700
 */
val LocalPhonicsFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

/** Dựng [FontFamily] cho phần học. Gọi một lần ở [GrabeeTheme], nơi khác đọc qua [LocalPhonicsFontFamily]. */
@Composable
internal fun rememberPhonicsFontFamily(): FontFamily = FontFamily(
    Font(Res.font.quicksand_regular, FontWeight.Normal),
    Font(Res.font.quicksand_bold, FontWeight.Bold),
)
