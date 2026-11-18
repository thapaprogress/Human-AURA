package com.example.natureaura.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.natureaura.engine.AuraReadingNarrative
import com.example.natureaura.model.AuraResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Composites captured CameraX front-camera portraits (or Sanctuary backdrop fallback)
 * using on-device ML Kit Selfie Segmentation so the glowing multi-layer chakra corona
 * radiates behind the person's head & shoulders silhouette, adds analog film grain,
 * and frames it in an offline Aura Polaroid card.
 */
class AuraPhotoComposer(private val context: Context) {

    companion object {
        private const val PHOTO_WIDTH = 900
        private const val PHOTO_HEIGHT = 1200
    }

    suspend fun captureAndComposeAuraPhoto(
        imageCapture: ImageCapture?,
        auraResult: AuraResult,
        narrative: AuraReadingNarrative,
        includePolaroidFrame: Boolean = true,
    ): String? {
        val rawBitmap = if (imageCapture != null) {
            captureFromCamera(imageCapture)
        } else {
            null
        }

        return withContext(Dispatchers.IO) {
            val isRealPortrait = rawBitmap != null
            val baseBitmap = rawBitmap ?: loadFallbackSanctuaryPortrait()
            val segmentedMask = if (isRealPortrait) {
                extractSelfieSegmentationMask(baseBitmap)
            } else {
                null
            }

            val auraComposited = compositeAuraCorona(baseBitmap, segmentedMask, auraResult)
            if (baseBitmap !== auraComposited) baseBitmap.recycle()

            val grainApplied = applySubtleFilmGrain(auraComposited, auraResult.timestampMs)
            val finalBitmap = if (includePolaroidFrame) {
                wrapInPolaroidFrame(grainApplied, auraResult, narrative).also {
                    if (it !== grainApplied) grainApplied.recycle()
                }
            } else {
                grainApplied
            }

            saveOfflineJpeg(finalBitmap, auraResult.timestampMs)
        }
    }

    fun getShareableUri(photoPath: String): Uri? {
        val file = File(photoPath)
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file,
            )
        }.getOrNull()
    }

    private suspend fun extractSelfieSegmentationMask(bitmap: Bitmap): Bitmap? {
        return withTimeoutOrNull(2_500L) {
            suspendCancellableCoroutine { cont ->
                runCatching {
                    val options = SelfieSegmenterOptions.Builder()
                        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                        .build()
                    val segmenter = Segmentation.getClient(options)
                    val input = InputImage.fromBitmap(bitmap, 0)
                    segmenter.process(input)
                        .addOnSuccessListener { mask ->
                            val alphaMask = runCatching {
                                buildFeatheredAlphaMask(
                                    buffer = mask.buffer,
                                    maskW = mask.width,
                                    maskH = mask.height,
                                    targetW = bitmap.width,
                                    targetH = bitmap.height,
                                )
                            }.getOrNull()
                            runCatching { segmenter.close() }
                            if (cont.isActive) cont.resume(alphaMask)
                        }
                        .addOnFailureListener {
                            runCatching { segmenter.close() }
                            if (cont.isActive) cont.resume(null)
                        }
                }.onFailure {
                    if (cont.isActive) cont.resume(null)
                }
            }
        }
    }

    private fun buildFeatheredAlphaMask(
        buffer: ByteBuffer,
        maskW: Int,
        maskH: Int,
        targetW: Int,
        targetH: Int,
    ): Bitmap {
        buffer.rewind()
        val pixels = IntArray(maskW * maskH)
        var foregroundCount = 0
        for (i in pixels.indices) {
            val conf = buffer.float
            val alpha = ((conf - 0.25f) / 0.55f).coerceIn(0f, 1f)
            if (alpha > 0.5f) foregroundCount++
            val aByte = (alpha * 255f).toInt().coerceIn(0, 255)
            pixels[i] = Color.argb(aByte, 255, 255, 255)
        }
        val small = Bitmap.createBitmap(pixels, maskW, maskH, Bitmap.Config.ARGB_8888)
        return if (maskW != targetW || maskH != targetH) {
            Bitmap.createScaledBitmap(small, targetW, targetH, true).also { small.recycle() }
        } else {
            small
        }
    }

    private suspend fun captureFromCamera(imageCapture: ImageCapture): Bitmap? =
        suspendCancellableCoroutine { cont ->
            val executor = ContextCompat.getMainExecutor(context)
            imageCapture.takePicture(
                executor,
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val decoded = runCatching { decodeAndMirrorFrontProxy(image) }.getOrNull()
                        image.close()
                        if (cont.isActive) cont.resume(decoded)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        if (cont.isActive) cont.resume(null)
                    }
                },
            )
        }

    private fun decodeAndMirrorFrontProxy(image: ImageProxy): Bitmap? {
        val buffer = image.planes.firstOrNull()?.buffer ?: return null
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val raw = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f)
        }
        val transformed = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
        if (transformed !== raw) raw.recycle()
        return centerCropScale(transformed, PHOTO_WIDTH, PHOTO_HEIGHT)
    }

    private fun loadFallbackSanctuaryPortrait(): Bitmap {
        val bg = BitmapFactory.decodeResource(context.resources, R.drawable.img_aura_sanctuary_bg)
        return if (bg != null) {
            centerCropScale(bg, PHOTO_WIDTH, PHOTO_HEIGHT)
        } else {
            Bitmap.createBitmap(PHOTO_WIDTH, PHOTO_HEIGHT, Bitmap.Config.ARGB_8888).apply {
                eraseColor(Color.rgb(10, 22, 18))
            }
        }
    }

    private fun centerCropScale(source: Bitmap, targetW: Int, targetH: Int): Bitmap {
        val scale = maxOf(
            targetW.toFloat() / source.width.toFloat(),
            targetH.toFloat() / source.height.toFloat(),
        )
        val scaledW = (source.width * scale).toInt().coerceAtLeast(targetW)
        val scaledH = (source.height * scale).toInt().coerceAtLeast(targetH)
        val scaled = Bitmap.createScaledBitmap(source, scaledW, scaledH, true)
        val cropX = ((scaledW - targetW) / 2).coerceAtLeast(0)
        val cropY = ((scaledH - targetH) / 2).coerceAtLeast(0)
        val cropped = Bitmap.createBitmap(scaled, cropX, cropY, targetW, targetH)
        if (scaled !== cropped) scaled.recycle()
        if (source !== scaled && source !== cropped) source.recycle()
        return cropped
    }

    private fun compositeAuraCorona(
        base: Bitmap,
        subjectMask: Bitmap?,
        result: AuraResult,
    ): Bitmap {
        val output = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val w = output.width.toFloat()
        val h = output.height.toFloat()

        // 1. Studio darkroom backdrop toning so aura colors stay rich even in bright rooms
        val backdropMatrix = android.graphics.ColorMatrix().apply {
            val scale = if (subjectMask != null) 0.40f else 0.56f
            set(
                floatArrayOf(
                    scale * 0.92f, 0f, 0f, 0f, -6f,
                    0f, scale * 0.96f, 0f, 0f, -2f,
                    0f, 0f, scale * 1.06f, 0f, 4f,
                    0f, 0f, 0f, 1f, 0f,
                ),
            )
        }
        val backdropPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            colorFilter = android.graphics.ColorMatrixColorFilter(backdropMatrix)
        }
        canvas.drawBitmap(base, 0f, 0f, backdropPaint)

        val sorted = result.chakras.sortedByDescending { it.intensity }
        val primary = sorted[0].chakra.color.toArgb()
        val secondary = sorted.getOrElse(1) { sorted[0] }.chakra.color.toArgb()
        val tertiary = sorted.getOrElse(2) { sorted[0] }.chakra.color.toArgb()

        val scoreMap = result.chakras.associateBy { it.chakra }
        fun chakraColor(type: com.example.natureaura.model.ChakraType): Int = type.color.toArgb()
        fun chakraStrength(type: com.example.natureaura.model.ChakraType): Float =
            (scoreMap[type]?.intensity ?: 0.5f).coerceIn(0.25f, 1f)

        // 2. Wide studio chromatic wash + 7-zone spatial aura clouds
        val screenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }

        // Broad dual-tone atmospheric background field
        screenPaint.shader = android.graphics.LinearGradient(
            0f,
            0f,
            w,
            h,
            intArrayOf(
                withAlpha(primary, 165),
                withAlpha(secondary, 155),
                withAlpha(tertiary, 165),
            ),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, screenPaint)

        // Spatial aura plumes mapped top-to-bottom around head, shoulders, heart, and core
        data class AuraBloom(val cx: Float, val cy: Float, val radius: Float, val color: Int, val alpha: Int)

        val blooms = listOf(
            AuraBloom(
                w * 0.50f,
                h * 0.13f,
                w * 0.62f,
                chakraColor(com.example.natureaura.model.ChakraType.CROWN),
                (150 + 85 * chakraStrength(com.example.natureaura.model.ChakraType.CROWN)).toInt(),
            ),
            AuraBloom(
                w * 0.50f,
                h * 0.25f,
                w * 0.54f,
                chakraColor(com.example.natureaura.model.ChakraType.THIRD_EYE),
                (135 + 85 * chakraStrength(com.example.natureaura.model.ChakraType.THIRD_EYE)).toInt(),
            ),
            AuraBloom(
                w * 0.20f,
                h * 0.41f,
                w * 0.56f,
                chakraColor(com.example.natureaura.model.ChakraType.THROAT),
                (140 + 85 * chakraStrength(com.example.natureaura.model.ChakraType.THROAT)).toInt(),
            ),
            AuraBloom(
                w * 0.80f,
                h * 0.41f,
                w * 0.56f,
                secondary,
                (155 + 80 * sorted.getOrElse(1) { sorted[0] }.intensity).toInt(),
            ),
            AuraBloom(
                w * 0.50f,
                h * 0.48f,
                w * 0.72f,
                chakraColor(com.example.natureaura.model.ChakraType.HEART),
                (145 + 85 * chakraStrength(com.example.natureaura.model.ChakraType.HEART)).toInt(),
            ),
            AuraBloom(
                w * 0.24f,
                h * 0.69f,
                w * 0.58f,
                chakraColor(com.example.natureaura.model.ChakraType.SOLAR_PLEXUS),
                (135 + 85 * chakraStrength(com.example.natureaura.model.ChakraType.SOLAR_PLEXUS)).toInt(),
            ),
            AuraBloom(
                w * 0.76f,
                h * 0.70f,
                w * 0.58f,
                chakraColor(com.example.natureaura.model.ChakraType.SACRAL),
                (135 + 85 * chakraStrength(com.example.natureaura.model.ChakraType.SACRAL)).toInt(),
            ),
            AuraBloom(
                w * 0.50f,
                h * 0.88f,
                w * 0.65f,
                chakraColor(com.example.natureaura.model.ChakraType.ROOT),
                (140 + 85 * chakraStrength(com.example.natureaura.model.ChakraType.ROOT)).toInt(),
            ),
        )

        blooms.forEach { bloom ->
            screenPaint.shader = RadialGradient(
                bloom.cx,
                bloom.cy,
                bloom.radius,
                intArrayOf(
                    withAlpha(bloom.color, bloom.alpha),
                    withAlpha(bloom.color, (bloom.alpha * 0.62f).toInt()),
                    Color.TRANSPARENT,
                ),
                floatArrayOf(0f, 0.52f, 1f),
                Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, w, h, screenPaint)
        }

        // Additive luminous core blooms for the dominant and secondary chakras
        val addPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.ADD)
        }
        addPaint.shader = RadialGradient(
            w * 0.36f,
            h * 0.34f,
            w * 0.56f,
            intArrayOf(
                withAlpha(primary, 130),
                withAlpha(primary, 55),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, addPaint)

        addPaint.shader = RadialGradient(
            w * 0.66f,
            h * 0.44f,
            w * 0.52f,
            intArrayOf(
                withAlpha(secondary, 115),
                withAlpha(tertiary, 48),
                Color.TRANSPARENT,
            ),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, addPaint)

        // 3. When ML Kit Selfie Segmentation extracted a subject mask, paint a dilated
        // silhouette halo behind the person, restore natural foreground, then wrap soft rim light!
        if (subjectMask != null) {
            // Dilated outer silhouette corona
            drawSilhouetteHalo(canvas, subjectMask, primary, scale = 1.09f, alpha = 175)
            drawSilhouetteHalo(canvas, subjectMask, secondary, scale = 1.045f, alpha = 145)

            // Crisp natural subject foreground
            val fg = Bitmap.createBitmap(base.width, base.height, Bitmap.Config.ARGB_8888)
            val fgCanvas = Canvas(fg)
            fgCanvas.drawBitmap(base, 0f, 0f, null)
            val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            fgCanvas.drawBitmap(subjectMask, 0f, 0f, maskPaint)
            canvas.drawBitmap(fg, 0f, 0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { alpha = 248 })
            fg.recycle()
            subjectMask.recycle()
        }

        // 4. Chromatic light-wrap kissing perimeter & shoulders while leaving face clear
        screenPaint.shader = RadialGradient(
            w * 0.50f,
            h * 0.43f,
            h * 0.60f,
            intArrayOf(
                Color.TRANSPARENT,
                withAlpha(primary, 82),
                withAlpha(secondary, 108),
                withAlpha(tertiary, 76),
            ),
            floatArrayOf(0.24f, 0.54f, 0.80f, 1.0f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w, h, screenPaint)

        // 5. Deep camera edge vignette for high contrast
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w * 0.5f,
                h * 0.45f,
                h * 0.68f,
                intArrayOf(Color.TRANSPARENT, Color.argb(128, 4, 9, 8)),
                floatArrayOf(0.58f, 1.0f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, w, h, vignettePaint)

        return output
    }

    private fun drawSilhouetteHalo(
        canvas: Canvas,
        mask: Bitmap,
        haloColor: Int,
        scale: Float,
        alpha: Int,
    ) {
        val matrix = Matrix().apply {
            postScale(scale, scale, mask.width * 0.5f, mask.height * 0.56f)
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.alpha = alpha
            colorFilter = android.graphics.PorterDuffColorFilter(haloColor, PorterDuff.Mode.SRC_IN)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
        }
        canvas.drawBitmap(mask, matrix, paint)
    }

    private fun applySubtleFilmGrain(bitmap: Bitmap, seed: Long): Bitmap {
        val rng = Random(seed)
        val paint = Paint().apply { strokeWidth = 2f }
        val canvas = Canvas(bitmap)
        val w = bitmap.width
        val h = bitmap.height
        for (i in 0 until 1800) {
            val x = rng.nextFloat() * w
            val y = rng.nextFloat() * h
            val lum = if (rng.nextBoolean()) 255 else 0
            paint.color = Color.argb(11, lum, lum, lum)
            canvas.drawPoint(x, y, paint)
        }
        return bitmap
    }

    private fun wrapInPolaroidFrame(
        photo: Bitmap,
        result: AuraResult,
        narrative: AuraReadingNarrative,
    ): Bitmap {
        val frameW = photo.width + 72
        val frameH = photo.height + 240
        val framed = Bitmap.createBitmap(frameW, frameH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(framed)

        val sorted = result.chakras.sortedByDescending { it.intensity }
        val primary = sorted[0].chakra.color.toArgb()
        val secondary = sorted.getOrElse(1) { sorted[0] }.chakra.color.toArgb()
        val tertiary = sorted.getOrElse(2) { sorted[0] }.chakra.color.toArgb()

        // Deep charcoal sanctuary film mount with subtle dominant chromatic wash
        canvas.drawColor(Color.rgb(13, 24, 20))
        val mountWash = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.LinearGradient(
                0f,
                0f,
                frameW.toFloat(),
                frameH.toFloat(),
                intArrayOf(
                    withAlpha(primary, 42),
                    withAlpha(secondary, 26),
                    withAlpha(tertiary, 38),
                ),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, frameW.toFloat(), frameH.toFloat(), mountWash)

        val photoRect = RectF(36f, 36f, 36f + photo.width, 36f + photo.height)
        canvas.drawBitmap(photo, 36f, 36f, null)

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            shader = android.graphics.LinearGradient(
                photoRect.left,
                photoRect.top,
                photoRect.right,
                photoRect.bottom,
                intArrayOf(primary, secondary, tertiary),
                null,
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(photoRect, borderPaint)

        // Proportional 7-Chakra Spectral Color Ribbon beneath the portrait
        val ribbonTop = photo.height + 56f
        val ribbonBottom = ribbonTop + 16f
        val ribbonLeft = 40f
        val ribbonTotalW = frameW - 80f
        val totalIntensity = sorted.sumOf { it.intensity.toDouble().coerceAtLeast(0.05) }.coerceAtLeast(0.1)
        var cursorX = ribbonLeft
        val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        sorted.forEachIndexed { idx, score ->
            val segW = if (idx == sorted.lastIndex) {
                (ribbonLeft + ribbonTotalW - cursorX).coerceAtLeast(0f)
            } else {
                (ribbonTotalW * (score.intensity / totalIntensity.toFloat())).coerceAtLeast(8f)
            }
            ribbonPaint.color = score.chakra.color.toArgb()
            canvas.drawRect(cursorX, ribbonTop, cursorX + segW, ribbonBottom, ribbonPaint)
            cursorX += segW
        }

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(245, 241, 233)
            textSize = 36f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(186, 206, 198)
            textSize = 23f
            typeface = Typeface.MONOSPACE
        }
        val dateText = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.US)
            .format(Date(result.timestampMs))

        canvas.drawText(narrative.archetypeTitle, 40f, photo.height + 132f, titlePaint)
        canvas.drawText(
            "${(result.overallBalance * 100).toInt()}% HARMONY · ${result.dominantChakra.chakra.colorName.uppercase()} · $dateText",
            40f,
            photo.height + 180f,
            subPaint,
        )

        return framed
    }

    private fun saveOfflineJpeg(bitmap: Bitmap, timestampMs: Long): String? {
        val dir = File(context.filesDir, "aura_photos").apply { mkdirs() }
        val file = File(dir, "aura_$timestampMs.jpg")
        return runCatching {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            bitmap.recycle()
            file.absolutePath
        }.getOrNull()
    }

    fun deletePhotoFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun withAlpha(argb: Int, alpha: Int): Int {
        return Color.argb(
            alpha.coerceIn(0, 255),
            Color.red(argb),
            Color.green(argb),
            Color.blue(argb),
        )
    }
}
