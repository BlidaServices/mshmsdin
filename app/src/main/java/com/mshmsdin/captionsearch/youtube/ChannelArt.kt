package com.mshmsdin.captionsearch.youtube

import com.mshmsdin.captionsearch.R

object ChannelArt {
    private val byTitle = listOf(
        "بثوث" to R.drawable.ch_live,
        "الردود" to R.drawable.ch_radood,
        "التلبيس" to R.drawable.ch_talbis,
        "شمس الدين" to R.drawable.ch_shams
    )

    fun iconFor(title: String): Int =
        byTitle.firstOrNull { title.contains(it.first) }?.second ?: R.drawable.ch_shams

    val covers = intArrayOf(
        R.drawable.cover_01, R.drawable.cover_02, R.drawable.cover_03,
        R.drawable.cover_04, R.drawable.cover_05, R.drawable.cover_06
    )
}
