package com.example.smartattendanceapp

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * FaceClassifier using ML Kit Face Detection.
 *
 * Generates a 128-dimensional embedding from face landmarks.
 * Uses cosine similarity for face matching.
 * Similarity threshold: >= 0.70 is considered a match.
 */
class FaceClassifier(context: Context, modelPath: String = "") {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .build()
    )

    companion object {
        private const val TAG = "FaceClassifier"
        const val SIMILARITY_THRESHOLD = 0.70f
        private const val EMBEDDING_SIZE = 128

        /**
         * Cosine similarity between two L2-normalised embedding vectors.
         * Returns a value in [0, 1] where 1 = identical faces.
         */
        fun calculateSimilarity(v1: FloatArray, v2: FloatArray): Float {
            if (v1.size != v2.size || v1.isEmpty()) return 0f
            var dot = 0f; var n1 = 0f; var n2 = 0f
            for (i in v1.indices) {
                dot += v1[i] * v2[i]
                n1  += v1[i] * v1[i]
                n2  += v2[i] * v2[i]
            }
            val denom = sqrt(n1) * sqrt(n2)
            return if (denom == 0f) 0f else (dot / denom).coerceIn(0f, 1f)
        }

        /** L2-normalise a vector in-place */
        fun normalizeEmbedding(embedding: FloatArray): FloatArray {
            val norm = sqrt(embedding.map { it * it }.sum())
            return if (norm == 0f) embedding else FloatArray(embedding.size) { embedding[it] / norm }
        }
    }

    /**
     * Recognise a face in [bitmap] and return a 128-D embedding.
     * Runs synchronously via a CountDownLatch so it can be called from a coroutine.
     * Returns a zero vector if no face is detected.
     */
    fun recognizeFace(bitmap: Bitmap): FloatArray {
        val result = FloatArray(EMBEDDING_SIZE)
        val latch  = java.util.concurrent.CountDownLatch(1)

        val image = InputImage.fromBitmap(bitmap, 0)
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val embedding = generateEmbedding(bitmap, faces[0])
                    normalizeEmbedding(embedding).copyInto(result)
                    Log.d(TAG, "Face detected — embedding generated (${faces.size} face(s) found)")
                } else {
                    Log.w(TAG, "No face detected in image")
                }
                latch.countDown()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed: ${e.message}")
                latch.countDown()
            }

        latch.await(3, java.util.concurrent.TimeUnit.SECONDS)
        return result
    }

    /**
     * Build a 128-D feature vector from ML Kit face landmarks + metadata.
     *
     * Strategy:
     *  - Slots 0-79:  normalised (x, y) for up to 40 landmark types
     *  - Slots 80-95: inter-landmark distances (geometry)
     *  - Slot  96:    smiling probability
     *  - Slot  97:    left-eye open probability
     *  - Slot  98:    right-eye open probability
     *  - Slots 99-127: zero-padded
     */
    private fun generateEmbedding(
        bitmap: Bitmap,
        face: com.google.mlkit.vision.face.Face
    ): FloatArray {
        val embedding = FloatArray(EMBEDDING_SIZE)
        val W = bitmap.width.toFloat().coerceAtLeast(1f)
        val H = bitmap.height.toFloat().coerceAtLeast(1f)

        // All ML Kit landmark types
        val landmarkTypes = listOf(
            FaceLandmark.LEFT_EYE, FaceLandmark.RIGHT_EYE,
            FaceLandmark.LEFT_EAR, FaceLandmark.RIGHT_EAR,
            FaceLandmark.LEFT_CHEEK, FaceLandmark.RIGHT_CHEEK,
            FaceLandmark.NOSE_BASE,
            FaceLandmark.MOUTH_LEFT, FaceLandmark.MOUTH_RIGHT, FaceLandmark.MOUTH_BOTTOM
        )

        // Normalised landmark coordinates → slots 0-19
        var idx = 0
        for (type in landmarkTypes) {
            val lm = face.getLandmark(type)
            if (lm != null && idx + 1 < EMBEDDING_SIZE) {
                embedding[idx]     = lm.position.x / W
                embedding[idx + 1] = lm.position.y / H
            }
            idx += 2
        }

        // Helper to get normalised position or null
        fun pos(type: Int): Pair<Float, Float>? {
            val lm = face.getLandmark(type) ?: return null
            return lm.position.x / W to lm.position.y / H
        }

        fun dist(a: Pair<Float, Float>?, b: Pair<Float, Float>?): Float {
            if (a == null || b == null) return 0f
            val dx = a.first - b.first; val dy = a.second - b.second
            return sqrt(dx * dx + dy * dy)
        }

        // Key distances → slots 20-35
        val le = pos(FaceLandmark.LEFT_EYE);  val re = pos(FaceLandmark.RIGHT_EYE)
        val nb = pos(FaceLandmark.NOSE_BASE)
        val ml = pos(FaceLandmark.MOUTH_LEFT);val mr = pos(FaceLandmark.MOUTH_RIGHT)
        val mb = pos(FaceLandmark.MOUTH_BOTTOM)
        val lc = pos(FaceLandmark.LEFT_CHEEK); val rc = pos(FaceLandmark.RIGHT_CHEEK)

        val distances = floatArrayOf(
            dist(le, re),   // eye spread
            dist(le, nb),   // left eye to nose
            dist(re, nb),   // right eye to nose
            dist(nb, mb),   // nose to chin
            dist(ml, mr),   // mouth width
            dist(le, ml),   // left eye to mouth
            dist(re, mr),   // right eye to mouth
            dist(lc, rc),   // cheek width
            dist(le, lc),   // eye-cheek ratio
            dist(re, rc),
            dist(nb, ml),
            dist(nb, mr),
            dist(le, mb),
            dist(re, mb),
            dist(lc, mb),
            dist(rc, mb)
        )

        // Normalise distances by face bounding box diagonal
        val bb   = face.boundingBox
        val diag = sqrt((bb.width().toFloat() / W).let { it * it } +
                (bb.height().toFloat() / H).let { it * it }).coerceAtLeast(0.01f)
        for (i in distances.indices) {
            val slot = 20 + i
            if (slot < EMBEDDING_SIZE) embedding[slot] = distances[i] / diag
        }

        // Head pose angles (normalised to [-1, 1]) → slots 36-38
        if (36 < EMBEDDING_SIZE) embedding[36] = face.headEulerAngleX / 90f
        if (37 < EMBEDDING_SIZE) embedding[37] = face.headEulerAngleY / 90f
        if (38 < EMBEDDING_SIZE) embedding[38] = face.headEulerAngleZ / 90f

        // Classification probabilities → slots 39-41
        if (39 < EMBEDDING_SIZE) embedding[39] = face.smilingProbability  ?: 0f
        if (40 < EMBEDDING_SIZE) embedding[40] = face.leftEyeOpenProbability  ?: 0f
        if (41 < EMBEDDING_SIZE) embedding[41] = face.rightEyeOpenProbability ?: 0f

        // Bounding box aspect ratio → slot 42
        if (42 < EMBEDDING_SIZE) {
            val ar = if (bb.height() > 0) bb.width().toFloat() / bb.height().toFloat() else 0f
            embedding[42] = ar.coerceIn(0f, 2f) / 2f
        }

        return embedding
    }

    fun close() {
        try { detector.close() } catch (e: Exception) { /* ignore */ }
    }
}