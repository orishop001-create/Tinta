package id.tinta.app.util

enum class DiffKind { SAME, ADD, REMOVE }

data class DiffLine(val kind: DiffKind, val text: String)

object Diff {
    fun lines(old: String, new: String): List<DiffLine> {
        val a = old.lines().take(700)
        val b = new.lines().take(700)
        if (a.size.toLong() * b.size.toLong() > 400_000L) {
            return a.map { DiffLine(DiffKind.REMOVE, it) } +
                b.map { DiffLine(DiffKind.ADD, it) }
        }
        val dp = Array(a.size + 1) { IntArray(b.size + 1) }
        for (i in a.indices.reversed()) {
            for (j in b.indices.reversed()) {
                dp[i][j] = if (a[i] == b[j]) dp[i + 1][j + 1] + 1
                else maxOf(dp[i + 1][j], dp[i][j + 1])
            }
        }
        val out = mutableListOf<DiffLine>()
        var i = 0
        var j = 0
        while (i < a.size && j < b.size) {
            when {
                a[i] == b[j] -> {
                    out += DiffLine(DiffKind.SAME, a[i])
                    i++
                    j++
                }
                dp[i + 1][j] >= dp[i][j + 1] -> {
                    out += DiffLine(DiffKind.REMOVE, a[i])
                    i++
                }
                else -> {
                    out += DiffLine(DiffKind.ADD, b[j])
                    j++
                }
            }
        }
        while (i < a.size) out += DiffLine(DiffKind.REMOVE, a[i++])
        while (j < b.size) out += DiffLine(DiffKind.ADD, b[j++])
        return out
    }
}
