package com.example.smartattandanceapp

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.sqrt

class FaceClassifier(context: Context, modelPath: String) {
    private val interpreter: Interpreter
    private val inputSize = 112
    private val outputSize = 128

    init {
        val opts = Interpreter.Options().apply {
            setNumThreads(4)
            setUseNNAPI(true)
        }
        interpreter = Interpreter(loadModelFile(context, modelPath), opts)
    }

    private fun loadModelFile(context: Context, path: String): ByteBuffer {
        val fd: AssetFileDescriptor = context.assets.openFd(path)
        val fis = FileInputStream(fd.fileDescriptor)
        val fc = fis.channel
        return fc.map(FileChannel.MapMode.READ_ONLY, fd.startOffset, fd.declaredLength)
    }

    fun recognizeFace(bitmap: Bitmap): FloatArray {
        val scaled = if (bitmap.width != inputSize || bitmap.height != inputSize)
            Bitmap.createScaledBitmap(bitmap, inputSize, inputSize, true) else bitmap

        val buf = ByteBuffer.allocateDirect(4 * inputSize * inputSize * 3).apply {
            order(ByteOrder.nativeOrder())
        }
        val pixels = IntArray(inputSize * inputSize)
        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)

        for (p in pixels) {
            buf.putFloat(((p shr 16 and 0xFF) - 127.5f) / 128f)
            buf.putFloat(((p shr 8 and 0xFF) - 127.5f) / 128f)
            buf.putFloat(((p and 0xFF) - 127.5f) / 128f)
        }
        buf.rewind()

        val out = Array(1) { FloatArray(outputSize) }
        interpreter.run(buf, out)
        return normalizeEmbedding(out[0])
    }

    private fun normalizeEmbedding(v: FloatArray): FloatArray {
        var n = 0f
        for (x in v) n += x * x
        n = sqrt(n)
        return if (n == 0f) v else v.map { it / n }.toFloatArray()
    }

    fun close() { interpreter.close() }
}