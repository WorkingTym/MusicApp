package com.example.music

import androidx.annotation.DrawableRes
import androidx.annotation.RawRes


data class Track(
    val name: String,
    val desc: String,
    @RawRes val id: Int,
    @DrawableRes val image: Int
) {
    constructor() : this("Ik Mulaqaat", "Ik Mulaqaat Unplugged Ft Ayushmann Khurrana - Dream Girl _ Nushrat B _ Meet Bros _ Shabbir Ahmed", R.raw.ik, R.drawable.ek_mulakat)
}

val songs= listOf(
    Track("Ik Mulaqaat","Ik Mulaqaat Unplugged Ft Ayushmann Khurrana - Dream Girl _ Nushrat B _ Meet Bros _ Shabbir Ahmed",R.raw.ik,R.drawable.ek_mulakat),
    Track("Kabira","Kabira - Yeh Jawaani Hai Deewani | Ranbir Kapoor, Deepika Padukone",R.raw.kabira,R.drawable.kabira),
    Track("Monta Re","Monta Re (Lootera) _ Unplugged Cover _ Sudipta Das",R.raw.monta,R.drawable.montare),
    Track("Nazm Nazm","Nazm Nazm - Lyrical _ Bareilly Ki Barfi _ Kriti Sanon, Ayushmann Khurrana & Rajkummar Rao _ Arko (1)",R.raw.nazm,R.drawable.nazm),
    Track("Naina Da Kya Kasoor","Naina Da Kya Kasoor - Full Video _ AndhaDhun _ Ayushmann Khurrana _ Radhika Apte _ Amit Trivedi",R.raw.naina,R.drawable.naina),

)