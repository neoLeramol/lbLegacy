/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.font

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.file.FileManager.fontsDir
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.HttpUtils.Downloader
import net.ccbluex.liquidbounce.utils.io.extractZipTo
import net.ccbluex.liquidbounce.utils.io.jsonArray
import net.ccbluex.liquidbounce.utils.io.readJson
import net.ccbluex.liquidbounce.utils.io.writeJson
import net.minecraft.client.gui.FontRenderer
import java.awt.Font
import java.io.File
import kotlin.system.measureTimeMillis

data class FontInfo(val name: String, val size: Int = -1, val isCustom: Boolean = false)

private val FONT_REGISTRY = LinkedHashMap<FontInfo, FontRenderer>()

object Fonts : MinecraftInstance {

    val minecraftFont: FontRenderer by lazy {
        mc.fontRendererObj
    }

    lateinit var font35: GameFontRenderer

    lateinit var font40: GameFontRenderer

    lateinit var fontBold180: GameFontRenderer

    private fun <T : FontRenderer> register(fontInfo: FontInfo, fontRenderer: T): T {
        FONT_REGISTRY[fontInfo] = fontRenderer
        return fontRenderer
    }

    fun loadFonts() {
        LOGGER.info("Start to load fonts.")
        val time = measureTimeMillis {
            downloadFonts()

            register(FontInfo(name = "Minecraft Font"), minecraftFont)

            font35 = register(FontInfo(name = "Roboto Medium", size = 35),
                getFontFromFile("Roboto-Medium.ttf", 35).asGameFontRenderer())

            font40 = register(FontInfo(name = "Roboto Medium", size = 40),
                getFontFromFile("Roboto-Medium.ttf", 40).asGameFontRenderer())

            fontBold180 = register(FontInfo(name = "Roboto Bold", size = 180),
                getFontFromFile("Roboto-Bold.ttf", 180).asGameFontRenderer())

            loadCustomFonts()
        }
        LOGGER.info("Loaded ${FONT_REGISTRY.size} fonts in ${time}ms")
    }

    private fun loadCustomFonts() {
        FONT_REGISTRY.keys.removeIf { it.isCustom }

        File(fontsDir, "fonts.json").apply {
            if (exists()) {
                val jsonElement = readJson()

                if (jsonElement !is JsonArray) return@apply

                for (element in jsonElement) {
                    if (element !is JsonObject) return@apply

                    val font = getFontFromFile(element["fontFile"].asString, element["fontSize"].asInt)

                    FONT_REGISTRY[FontInfo(font.name, font.size, isCustom = true)] = GameFontRenderer(font)
                }
            } else {
                createNewFile()
                writeJson(jsonArray())
            }
        }
    }

    fun downloadFonts() {
        val outputFile = File(fontsDir, "roboto.zip")
        if (!outputFile.exists()) {
            LOGGER.info("Downloading fonts...")
            Downloader.downloadWholeFile("$CLIENT_CLOUD/fonts/Roboto.zip", outputFile)
            LOGGER.info("Extracting fonts...")
            outputFile.extractZipTo(fontsDir)
        }
    }

    fun getFontRenderer(name: String, size: Int): FontRenderer {
        return FONT_REGISTRY.entries.firstOrNull { (fontInfo, _) ->
            fontInfo.size == size && fontInfo.name.equals(name, true)
        }?.value ?: minecraftFont
    }

    fun getFontDetails(fontRenderer: FontRenderer): FontInfo? {
        return FONT_REGISTRY.keys.firstOrNull { FONT_REGISTRY[it] == fontRenderer }
    }

    val fonts: List<FontRenderer>
        get() = FONT_REGISTRY.values.toList()

    private fun getFontFromFile(fontName: String, size: Int): Font = try {
        File(fontsDir, fontName).inputStream().use { inputStream ->
            Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont(Font.PLAIN, size.toFloat())
        }
    } catch (e: Exception) {
        LOGGER.warn("Exception during loading font[name=${fontName}, size=${size}]", e)
        Font("default", Font.PLAIN, size)
    }

    private fun Font.asGameFontRenderer(): GameFontRenderer {
        return GameFontRenderer(this@asGameFontRenderer)
    }

}
