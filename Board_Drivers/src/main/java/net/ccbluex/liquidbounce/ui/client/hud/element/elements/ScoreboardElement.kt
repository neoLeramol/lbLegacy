/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_WEBSITE
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRectInt
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11.glColor4f
import java.awt.Color

/**
 * CustomHUD scoreboard
 *
 * Allows to move and customize minecraft scoreboard
 */
@ElementInfo(name = "Scoreboard")
class ScoreboardElement(
    x: Double = 5.0, y: Double = 0.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.MIDDLE)
) : Element("Scoreboard", x, y, scale, side) {

    private val textColor by color("TextColor", Color.WHITE)
    private val backgroundColor by color("BackgroundColor", Color.BLACK.withAlpha(95))

    private val roundedRectRadius by float("Rounded-Radius", 3F, 0F..5F)

    private val rect by boolean("Rect", false)
    private val rectColor = color("RectangleColor", Color(0, 111, 255)) { rect }

    private val drawRectOnTitle by boolean("DrawRectOnTitle", false)
    private val titleRectColor by color("TitleRectColor", Color.BLACK.withAlpha(128)) { drawRectOnTitle }
    private val rectHeightPadding by int("TitleRectHeightPadding", 2, 0..10) { drawRectOnTitle }

    private val serverIp by choices("ServerIP", arrayOf("Normal", "None", "Client", "Website"), "Normal")
    private val number by boolean("Number", true)
    private val shadow by boolean("Shadow", false)
    private val font by font("Font", Fonts.minecraftFont)

    /**
     * Draw element
     */
    override fun drawElement(): Border? {
        assumeNonVolatile {

            val (fontRenderer, fontHeight) = font to ((font as? GameFontRenderer)?.height ?: font.FONT_HEIGHT)
            val textColor = textColor.rgb
            val backColor = backgroundColor.rgb

            val worldScoreboard = mc.theWorld.scoreboard ?: return null
            var currObjective: ScoreObjective? = null
            val playerTeam = worldScoreboard.getPlayersTeam(mc.thePlayer.name)

            if (playerTeam != null) {
                val colorIndex = playerTeam.chatFormat.colorIndex

                if (colorIndex >= 0) currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
            }

            val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

            val scoreboard = objective.scoreboard ?: return null
            var scoreCollection = scoreboard.getSortedScores(objective) ?: return null
            val scores = scoreCollection.filter { it.playerName?.startsWith("#") == false }

            scoreCollection = if (scores.size > 15) {
                scores.drop(scoreCollection.size - 15)
            } else scores

            var maxWidth = fontRenderer.getStringWidth(objective.displayName)

            for (score in scoreCollection) {
                val scorePlayerTeam = scoreboard.getPlayersTeam(score.playerName)
                val width = if (number) {
                    "${
                        ScorePlayerTeam.formatPlayerName(
                            scorePlayerTeam, score.playerName
                        )
                    }: ${EnumChatFormatting.RED}${score.scorePoints}"
                } else {
                    ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.playerName)
                }
                maxWidth = maxWidth.coerceAtLeast(fontRenderer.getStringWidth(width))
            }

            val maxHeight = scoreCollection.size * fontHeight
            val l1 = -maxWidth - 3 - if (rect) 3 else 0

            val inc = if (drawRectOnTitle) 2 else 0

            val (minX, maxX) = l1 - 4 to 7

            drawRoundedRectInt(minX, -(4 + inc), maxX, maxHeight + fontHeight + 2, backColor, roundedRectRadius)

            scoreCollection.filterNotNull().forEachIndexed { index, score ->
                val team = scoreboard.getPlayersTeam(score.playerName)

                var name = ScorePlayerTeam.formatPlayerName(team, score.playerName)
                val scorePoints = if (number) "${EnumChatFormatting.RED}${score.scorePoints}" else ""

                val width = 5 - if (rect) 4 else 0
                val height = maxHeight - index * fontHeight.toFloat()

                glColor4f(1f, 1f, 1f, 1f)

                if (serverIp != "Normal") {
                    try {
                        val nameWithoutFormatting = name?.replace(EnumChatFormatting.RESET.toString(), "")
                            ?.replace(Regex("[\u00a7&][0-9a-fk-or]"), "")?.trim()
                        val trimmedServerIP = mc.currentServerData?.serverIP?.trim()?.lowercase() ?: ""

                        val domainRegex =
                            Regex("\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}\\b")
                        val containsDomain = nameWithoutFormatting?.let { domainRegex.containsMatchIn(it) } == true

                        runCatching {
                            if (nameWithoutFormatting?.lowercase() == trimmedServerIP || containsDomain) {
                                val colorCode = name?.substring(0, 2) ?: "§9"
                                name = when (serverIp.lowercase()) {
                                    "none" -> ""
                                    "client" -> "$colorCode$CLIENT_NAME"
                                    "website" -> "$colorCode$CLIENT_WEBSITE"
                                    else -> return null
                                }
                            }
                        }.onFailure {
                            LOGGER.error("Error while changing Scoreboard Server IP: ${it.message}")
                        }
                    } catch (e: Exception) {
                        LOGGER.error("Error while drawing ScoreboardElement", e)
                    }
                }

                fontRenderer.drawString(name, l1.toFloat(), height, textColor, shadow)
                if (number) {
                    fontRenderer.drawString(
                        scorePoints,
                        (width - fontRenderer.getStringWidth(scorePoints)).toFloat(),
                        height,
                        textColor,
                        shadow
                    )
                }

                if (index == scoreCollection.size - 1) {
                    var title = objective.displayName ?: ""
                    val displayName = if (serverIp != "Normal") {
                        try {
                            val nameWithoutFormatting = title.replace(EnumChatFormatting.RESET.toString(), "")
                                .replace(Regex("[\u00a7&][0-9a-fk-or]"), "").trim()
                            val trimmedServerIP = mc.currentServerData?.serverIP?.trim()?.lowercase() ?: ""

                            val domainRegex =
                                Regex("\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}\\b")
                            val containsDomain = nameWithoutFormatting.let { domainRegex.containsMatchIn(it) } == true

                            if (nameWithoutFormatting.lowercase() == trimmedServerIP || containsDomain) {
                                val colorCode = title.substring(0, 2)
                                when (serverIp.lowercase()) {
                                    "none" -> ""
                                    "client" -> "$colorCode$CLIENT_NAME"
                                    "website" -> "$colorCode$CLIENT_WEBSITE"
                                    else -> return null
                                }
                            } else title
                        } catch (e: Exception) {
                            LOGGER.error("Error while drawing ScoreboardElement", e)
                            title
                        }
                    } else title

                    if (drawRectOnTitle) {
                        drawRoundedRectInt(
                            minX,
                            -(4 + inc),
                            maxX,
                            fontHeight - inc + rectHeightPadding,
                            titleRectColor.rgb,
                            roundedRectRadius,
                            RenderUtils.RoundedCorners.TOP_ONLY
                        )
                    }

                    glColor4f(1f, 1f, 1f, 1f)

                    fontRenderer.drawString(
                        displayName,
                        (minX..maxX).lerpWith(0.5F) - fontRenderer.getStringWidth(displayName) / 2,
                        height - fontHeight - inc,
                        textColor,
                        shadow
                    )
                }

                if (rect) {
                    val rectColor = when {
                        this.rectColor.rainbow -> ColorUtils.rainbow(400000000L * index).rgb
                        else -> this.rectColor.selectedColor().rgb
                    }

                    drawRoundedRect(
                        3.25F,
                        (if (index == scoreCollection.size - 1) -2F else height) - inc - 1.5F,
                        maxX - 0.25F,
                        (if (index == 0) fontHeight.toFloat() else height + fontHeight * 2F) + 2F,
                        rectColor,
                        roundedRectRadius,
                        RenderUtils.RoundedCorners.RIGHT_ONLY
                    )
                }
            }

            return Border(l1 - 4F, -4F - inc, 7F, maxHeight + fontHeight + 2F)
        }

        return null
    }

}
