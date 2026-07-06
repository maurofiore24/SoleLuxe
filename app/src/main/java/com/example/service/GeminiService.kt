package com.example.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Moshi Mapped Gemini Request & Response Models ---

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    @field:Json(name = "mimeType") val mimeType: String,
    @field:Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    @field:Json(name = "text") val text: String? = null,
    @field:Json(name = "inlineData") val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    @field:Json(name = "parts") val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiThinkingConfig(
    @field:Json(name = "thinkingLevel") val thinkingLevel: String
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    @field:Json(name = "thinkingConfig") val thinkingConfig: GeminiThinkingConfig? = null,
    @field:Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    @field:Json(name = "contents") val contents: List<GeminiContent>,
    @field:Json(name = "generationConfig") val generationConfig: GeminiGenerationConfig? = null,
    @field:Json(name = "systemInstruction") val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    @field:Json(name = "content") val content: GeminiContent
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    @field:Json(name = "candidates") val candidates: List<GeminiCandidate>? = null
)

// --- Retrofit Service Interface ---

interface GeminiApi {
    @POST("v1beta/models/gemini-3.1-pro-preview:generateContent")
    suspend fun analyzeContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

class GeminiService(private val context: Context) {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val api = retrofit.create(GeminiApi::class.java)

    // Helper to convert bitmap to base64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    suspend fun analyzeFootwearAndLegwear(
        bitmap: Bitmap?,
        userComment: String
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiService", "Gemini API Key is a placeholder. Using intelligent fallback simulation.")
            return generateSimulatedAnalysis(userComment)
        }

        val promptText = """
            You are 'AuraAI', the premium luxury consultant for SoleLuxe, an exclusive high-fashion leg & feet modeling portfolio application.
            Your task is to analyze the fashion, styling, aesthetics, footwear, pedicures, anklets, or socks in the provided image/description.
            Provide a detailed, elegant, professional layout analysis in markdown format:
            1. Aesthetic Summary (Luxury rating from 1 to 10)
            2. Color Harmony & Palette suggestions
            3. Posing & Angle review for maximum leg symmetry
            4. Footwear & Accessory Matching recommendations (e.g. style of heels, ankle bracelets, or sheer stocking pattern pairings).
            Keep the tone classy, fashion-forward, professional, highly encouraging, and strictly safe-for-work.
            User's question/styling context: $userComment
        """.trimIndent()

        val parts = mutableListOf<GeminiPart>()
        parts.add(GeminiPart(text = promptText))

        if (bitmap != null) {
            val base64Data = bitmapToBase64(bitmap)
            parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data)))
        }

        val request = GeminiRequest(
            contents = listOf(GeminiContent(parts = parts)),
            generationConfig = GeminiGenerationConfig(
                thinkingConfig = GeminiThinkingConfig(thinkingLevel = "HIGH"),
                temperature = 0.7f
            )
        )

        return try {
            val response = api.analyzeContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "No analysis text could be generated. Try writing a descriptive comment!"
        } catch (e: Exception) {
            Log.e("GeminiService", "API call failure, falling back.", e)
            generateSimulatedAnalysis(userComment) + "\n\n*(Note: Displaying simulated analysis due to API feedback: ${e.localizedMessage})*"
        }
    }

    private fun generateSimulatedAnalysis(context: String): String {
        val isPedicure = context.contains("pedi", ignoreCase = true) || context.contains("nail", ignoreCase = true)
        val isHeels = context.contains("heel", ignoreCase = true) || context.contains("shoe", ignoreCase = true) || context.contains("sandal", ignoreCase = true)
        val isStockings = context.contains("stocking", ignoreCase = true) || context.contains("sock", ignoreCase = true) || context.contains("lace", ignoreCase = true)

        return when {
            isPedicure -> """
                ### ✨ AuraAI Style Analysis
                **Aesthetic Rating:** 9.2 / 10 (Sophisticated Harmony)

                #### 🎨 Color Harmony & Design Curation
                The choice of color creates a soft, luxurious focal accent. For high-end photography, matching pastel tones with a contrasting premium satin background (such as deep burgundy or plush slate) will magnify the depth of the details.

                #### 📐 Posing & Symmetry
                - Rest the foot at a comfortable 45-degree angle to create an elongated leg silhouette.
                - Keep toes relaxed to emphasize natural symmetry. 

                #### 💎 Accessories & Styling Pairings
                - Match your polish with a thin, minimalist platinum ring or a delicate gold ankle chain.
                - Best paired with open-toe, thin-strap designer sandals to showcase the pedicure art flawlessly.
            """.trimIndent()

            isHeels -> """
                ### ✨ AuraAI Style Analysis
                **Aesthetic Rating:** 9.6 / 10 (High-Fashion Runway)

                #### 🎨 Color Harmony & Curation
                Your choice of footwear features exquisite geometry. Deep velvet textures or glossy finishes pair beautifully with cool, low-contrast studio backdrops.

                #### 📐 Posing & Symmetry
                - Frame from a slightly low angle to maximize the vertical line of the legs.
                - Keep the ankle extended to align the curve of the calf with the stiletto’s structure.

                #### 💎 Accessories & Styling Pairings
                - Elevate this platform with ultra-sheer patterned thigh stockings for a luxury European catalog vibe.
                - Integrate a bold chunkier anklet if pairing with classic leather straps.
            """.trimIndent()

            isStockings -> """
                ### ✨ AuraAI Style Analysis
                **Aesthetic Rating:** 9.4 / 10 (Retro Elegance)

                #### 🎨 Color Harmony & Theme Curation
                A gorgeous vintage vibe. Neutral, high-contrast, and classy. The textured lace filters ambient light beautifully, creating instant visual rhythm.

                #### 📐 Posing & Symmetry
                - To highlight the lace alignment, capture in motion or with the leg moderately flexed.
                - Position in soft, directional side-lighting to enhance texture contrast.

                #### 💎 Accessories & Styling Pairings
                - Pair elegantly with patent leather pointed-toe stilettos to finalize the classic silhouette.
                - Minimal ankle accessories are recommended to avoid conflicting with the intricate lace details.
            """.trimIndent()

            else -> """
                ### ✨ AuraAI Style Analysis
                **Aesthetic Rating:** 9.0 / 10 (Artistic Composition)

                #### 🎨 Color Harmony & Curation
                A beautiful aesthetic focus. Using rich, warm natural colors along with deep slate gradients produces a highly tactile, premium visual texture.

                #### 📐 Posing & Symmetry
                - Frame using asymmetrical angles to capture high visual weight.
                - Keep the camera focus locked on the ankle line to emphasize the leg structure.

                #### 💎 Accessories & Styling Pairings
                - Decorate with a simple metallic anklet for a subtle sheen.
                - Pair with sandals or mesh socks to maintain a clean designer catalog look.
            """.trimIndent()
        }
    }
}
