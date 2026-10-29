package tech.eiki.globeswap.util

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Das Foto wird auf das Format des Suchers zugeschnitten. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application = Application::class, sdk = [34])
class CenterCropTest {
    @Test
    fun cropsToViewfinderAspect() {
        val bmp = Bitmap.createBitmap(1200, 1600, Bitmap.Config.ARGB_8888) // 3:4 hochkant
        val tall = ImageUtils.centerCrop(bmp, 9f / 16f)
        assertEquals(1600, tall.height)
        assertEquals(900, tall.width)
        val same = ImageUtils.centerCrop(bmp, 3f / 4f)
        assertEquals(1200, same.width)
        assertEquals(1600, same.height)
    }

    @Test
    fun jpegHasViewfinderAspect() {
        // Sensorbild quer (4:3), um 90° gedreht → hochkant, dann auf 9:16 zugeschnitten
        val sensor = Bitmap.createBitmap(1600, 1200, Bitmap.Config.ARGB_8888)
        val jpeg = ImageUtils.toJpeg(sensor, 90, mirror = false, aspect = 9f / 16f)
        val out = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)
        assertEquals(1600, out.height)
        assertEquals(900, out.width)
    }
}
