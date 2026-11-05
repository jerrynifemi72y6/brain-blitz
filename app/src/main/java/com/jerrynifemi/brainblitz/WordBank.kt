package com.jerrynifemi.brainblitz

import android.content.Context
import org.json.JSONObject

/** Loads the word search packs from assets/wordbank.json. */
object WordBank {

    data class Pack(val id: String, val name: String, val words: List<String>)

    private var cached: List<Pack>? = null

    fun packs(context: Context): List<Pack> {
        cached?.let { return it }
        val raw = context.assets.open("wordbank.json").bufferedReader().use { it.readText() }
        val arr = JSONObject(raw).getJSONArray("packs")
        val out = ArrayList<Pack>()
        for (i in 0 until arr.length()) {
            val p = arr.getJSONObject(i)
            val words = p.getJSONArray("words")
            val list = ArrayList<String>()
            for (j in 0 until words.length()) list.add(words.getString(j).uppercase())
            if (list.isNotEmpty()) out.add(Pack(p.getString("id"), p.getString("name"), list))
        }
        cached = out
        return out
    }
}
