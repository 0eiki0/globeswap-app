package tech.eiki.globeswap.ui.world

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Länderumrisse aus assets/world.txt (Natural Earth 1:110m, gemeinfrei), in Zehntelgrad einer
 * Plattkarte von 85° N bis 60° S.
 */
class WorldShapes(val countries: Map<String, List<FloatArray>>) {
    companion object {
        const val WIDTH = 3600f
        const val HEIGHT = 1450f

        fun parse(text: String): WorldShapes {
            val countries = mutableMapOf<String, MutableList<FloatArray>>()
            text.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.forEach { line ->
                val code = line.substringBefore('|')
                line.substringAfter('|').split(';').forEach { ring ->
                    val points = ring.trim().split(' ')
                    val coords = FloatArray(points.size * 2)
                    points.forEachIndexed { i, p ->
                        coords[i * 2] = p.substringBefore(',').toFloat()
                        coords[i * 2 + 1] = p.substringAfter(',').toFloat()
                    }
                    countries.getOrPut(code) { mutableListOf() }.add(coords)
                }
            }
            return WorldShapes(countries)
        }

        @Volatile private var cached: WorldShapes? = null

        fun load(context: Context): WorldShapes =
            cached ?: parse(context.assets.open("world.txt").bufferedReader().use { it.readText() }).also { cached = it }
    }
}

/** Weltkarte, auf der die Länder in [highlighted] farbig sind. */
@Composable
fun WorldMap(
    shapes: WorldShapes,
    highlighted: Set<String>,
    landColor: Color,
    highlightColor: Color,
    borderColor: Color,
    modifier: Modifier = Modifier,
) {
    val paths = remember(shapes) {
        shapes.countries.mapValues { (_, rings) ->
            Path().apply {
                rings.forEach { r ->
                    moveTo(r[0], r[1])
                    var i = 2
                    while (i < r.size) {
                        lineTo(r[i], r[i + 1]); i += 2
                    }
                    close()
                }
            }
        }
    }
    Canvas(
        modifier.fillMaxWidth().aspectRatio(WorldShapes.WIDTH / WorldShapes.HEIGHT)
            .semantics { contentDescription = "World map with ${highlighted.size} highlighted countries" },
    ) {
        val scale = size.width / WorldShapes.WIDTH
        drawContext.transform.scale(scale, scale, pivot = androidx.compose.ui.geometry.Offset.Zero)
        val stroke = Stroke(width = 0.8f / scale)
        paths.forEach { (code, path) ->
            drawPath(path, if (code in highlighted) highlightColor else landColor)
            drawPath(path, borderColor, style = stroke)
        }
    }
}

