package com.jerrynifemi.brainblitz

import android.content.Context
import org.json.JSONObject

/**
 * Loads the offline question bank from assets/questionbank.json.
 * No network needed, ever.
 */
object QuestionBank {

    const val ALL_ID = "all"

    data class Category(val id: String, val name: String, val questions: List<Question>)

    private var cached: List<Category>? = null

    fun categories(context: Context): List<Category> {
        cached?.let { return it }

        val raw = context.assets.open("questionbank.json").bufferedReader().use { it.readText() }
        val root = JSONObject(raw)
        val arr = root.getJSONArray("categories")

        val out = ArrayList<Category>()
        for (i in 0 until arr.length()) {
            val c = arr.getJSONObject(i)
            val id = c.getString("id")
            val name = c.getString("name")
            val qs = c.getJSONArray("questions")

            val questions = ArrayList<Question>()
            for (j in 0 until qs.length()) {
                val q = qs.getJSONObject(j)
                val optsJson = q.getJSONArray("options")
                val options = ArrayList<String>()
                for (k in 0 until optsJson.length()) options.add(optsJson.getString(k))

                val answer = q.getInt("answer")
                // Guard against a bad question landing in the app: skip anything malformed.
                if (options.size < 2 || answer < 0 || answer >= options.size) continue

                questions.add(
                    Question(
                        text = q.getString("q"),
                        options = options,
                        answerIndex = answer,
                        categoryId = id,
                        categoryName = name
                    )
                )
            }
            out.add(Category(id, name, questions))
        }

        cached = out
        return out
    }

    fun byId(context: Context, id: String): Category? =
        categories(context).firstOrNull { it.id == id }

    fun questionsFor(context: Context, id: String): List<Question> =
        if (id == ALL_ID) categories(context).flatMap { it.questions }
        else byId(context, id)?.questions ?: emptyList()
}
