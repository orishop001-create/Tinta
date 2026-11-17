package id.tinta.app.ui.components

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Menjaga perilaku Backspace di awal isi baris heading dan butir. */
class LineMarkerGuardTest {

    private fun value(text: String, caret: Int) = TextFieldValue(text, TextRange(caret))

    @Test
    fun backspaceSpasiHeadingHanyaMelepasPagar() {
        val guarded = protectLineMarkers(value("## Judul", 3), value("##Judul", 2))
        assertEquals("Judul", guarded.text)
        assertEquals(0, guarded.selection.start)
    }

    @Test
    fun backspaceSpasiHeadingDiBarisKedua() {
        val guarded = protectLineMarkers(value("a\n## T", 5), value("a\n##T", 4))
        assertEquals("a\nT", guarded.text)
        assertEquals(2, guarded.selection.start)
    }

    @Test
    fun backspaceSpasiButirHanyaMelepasTandaButir() {
        val guarded = protectLineMarkers(value("- isi", 2), value("-isi", 1))
        assertEquals("isi", guarded.text)
        assertEquals(0, guarded.selection.start)
    }

    @Test
    fun backspaceTandaButirTidakMenyisakanSpasi() {
        val guarded = protectLineMarkers(value("- isi", 1), value(" isi", 0))
        assertEquals("isi", guarded.text)
        assertEquals(0, guarded.selection.start)
    }

    @Test
    fun backspaceBiasaTidakDiubah() {
        val guarded = protectLineMarkers(value("abc", 3), value("ab", 2))
        assertEquals("ab", guarded.text)
        assertEquals(2, guarded.selection.start)
    }

    @Test
    fun backspaceBintangInlineTidakDianggapButir() {
        val guarded = protectLineMarkers(value("*a*", 1), value("a*", 0))
        assertEquals("a*", guarded.text)
    }
}
