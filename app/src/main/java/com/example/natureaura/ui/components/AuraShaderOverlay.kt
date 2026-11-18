package com.example.natureaura.ui.components

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import com.example.natureaura.model.AuraResult
import com.example.ui.theme.ChakraCrown
import com.example.ui.theme.ChakraHeart
import com.example.ui.theme.ChakraThroat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private const val AURA_AGSL_SHADER = """
    uniform float2 iResolution;
    uniform float iTime;
    uniform float iPulseFreq;
    uniform float iBalance;
    uniform float3 iColorPrimary;
    uniform float3 iColorSecondary;
    uniform float3 iColorTertiary;

    half4 main(float2 fragCoord) {
        float2 uv = fragCoord / iResolution.xy;
        float aspect = iResolution.x / max(iResolution.y, 1.0);

        float2 center = float2(0.5, 0.44);
        float2 p = (uv - center) * float2(aspect, 1.0);
        float2 ellipseP = float2(p.x * 1.18, p.y * 0.84);
        float dist = length(ellipseP);
        float angle = atan(ellipseP.y, ellipseP.x);

        // Heartbeat & swirling organic plasma waves
        float beat = 0.5 + 0.5 * sin(iTime * iPulseFreq * 6.28318);
        float wave1 = sin(angle * 3.0 + iTime * 1.15) * 0.042;
        float wave2 = cos(angle * 5.0 - iTime * 0.85) * 0.028;
        float coronaDist = dist + wave1 + wave2 - beat * 0.024;

        // Open face window in center while radiating richly around head, shoulders, and perimeter
        float innerClear = smoothstep(0.15, 0.32, coronaDist);
        float outerFade = 1.0 - smoothstep(0.54, 0.98, coronaDist);
        float haloMask = innerClear * outerFade;

        // Rich perimeter mist filling screen edges
        float ambientEdge = smoothstep(0.24, 0.72, dist) * 0.52;

        // Multi-lobe spectral blend bound to dominant, secondary, and tertiary chakras
        float mixA = 0.5 + 0.5 * sin(angle * 2.0 + iTime * 0.7 + uv.y * 2.4);
        float mixB = 0.5 + 0.5 * cos(angle * 3.0 - iTime * 0.5 + uv.x * 2.2);
        float3 col = mix(iColorPrimary, iColorSecondary, mixA);
        col = mix(col, iColorTertiary, mixB * 0.65);

        // Preserve chromatic saturation cleanly
        float intensity = clamp(haloMask * (0.64 + 0.28 * iBalance) + ambientEdge * (0.55 + 0.35 * iBalance), 0.0, 0.88);
        return half4(col * intensity, intensity);
    }
"""

/**
 * Dynamic multi-color radial aura overlay bound directly to AuraCalculator output.
 * Uses Android AGSL RuntimeShader on API 33+ and an offscreen-composited multi-lobe
 * radial gradient Canvas fallback on API 24-32.
 */
@Composable
fun AuraShaderOverlay(
    auraResult: AuraResult?,
    heartRateBpm: Double?,
    isScanning: Boolean,
    modifier: Modifier = Modifier,
) {
    val sortedChakras = remember(auraResult) {
        auraResult?.chakras?.sortedByDescending { it.intensity }
    }

    val primaryColor = sortedChakras?.getOrNull(0)?.chakra?.color ?: ChakraHeart
    val secondaryColor = sortedChakras?.getOrNull(1)?.chakra?.color ?: ChakraThroat
    val tertiaryColor = sortedChakras?.getOrNull(2)?.chakra?.color ?: ChakraCrown

    val targetBalance = when {
        auraResult != null -> auraResult.overallBalance.coerceIn(0.45f, 1.0f)
        isScanning -> 0.82f
        else -> 0.55f
    }
    val animatedBalance by animateFloatAsState(
        targetValue = targetBalance,
        animationSpec = tween(durationMillis = 700),
        label = "aura_balance",
    )

    val infiniteTransition = rememberInfiniteTransition(label = "aura_shader_time")
    val timeSec by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 62.8318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 48_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shader_time",
    )

    val pulseHz = ((heartRateBpm ?: 64.0) / 60.0).toFloat().coerceIn(0.65f, 2.6f)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(AURA_AGSL_SHADER) }
        val brush = remember(shader) { ShaderBrush(shader) }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", timeSec)
            shader.setFloatUniform("iPulseFreq", pulseHz)
            shader.setFloatUniform("iBalance", animatedBalance)
            shader.setFloatUniform("iColorPrimary", primaryColor.red, primaryColor.green, primaryColor.blue)
            shader.setFloatUniform("iColorSecondary", secondaryColor.red, secondaryColor.green, secondaryColor.blue)
            shader.setFloatUniform("iColorTertiary", tertiaryColor.red, tertiaryColor.green, tertiaryColor.blue)

            drawRect(brush = brush, blendMode = BlendMode.SrcOver)
        }
    } else {
        Canvas(
            modifier = modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.44f
            val beatScale = 1f + 0.05f * sin(timeSec * pulseHz * 2f * PI.toFloat())
            val radius = size.maxDimension * 0.62f * beatScale

            val orbitX = cos(timeSec * 0.8f) * size.minDimension * 0.08f
            val orbitY = sin(timeSec * 0.6f) * size.minDimension * 0.07f

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.22f to Color.Transparent,
                        0.45f to primaryColor.copy(alpha = 0.72f * animatedBalance),
                        0.74f to secondaryColor.copy(alpha = 0.55f * animatedBalance),
                        1.0f to tertiaryColor.copy(alpha = 0.28f * animatedBalance),
                    ),
                    center = Offset(cx + orbitX, cy + orbitY),
                    radius = radius,
                ),
                radius = radius,
                center = Offset(cx + orbitX, cy + orbitY),
                blendMode = BlendMode.SrcOver,
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.0f to Color.Transparent,
                        0.26f to Color.Transparent,
                        0.56f to tertiaryColor.copy(alpha = 0.60f * animatedBalance),
                        0.84f to secondaryColor.copy(alpha = 0.36f * animatedBalance),
                        1.0f to Color.Transparent,
                    ),
                    center = Offset(cx - orbitX, cy - orbitY),
                    radius = radius * 1.05f,
                ),
                radius = radius * 1.05f,
                center = Offset(cx - orbitX, cy - orbitY),
                blendMode = BlendMode.Plus,
            )
        }
    }
}
