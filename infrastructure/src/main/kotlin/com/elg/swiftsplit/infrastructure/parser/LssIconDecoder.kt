package com.elg.swiftsplit.infrastructure.parser

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

object LssIconDecoder {
    
    private val PNG_HEADER = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
    private val JPEG_HEADER = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

    fun decode(base64: String?): Bitmap? {
        if (base64.isNullOrBlank()) return null
        
        return try {
            val cleaned = base64.trim().substringAfter(",")
            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
            
            // Try to find the image header within the potential .NET binary serialization
            val offset = findHeaderOffset(bytes)
            if (offset != -1) {
                BitmapFactory.decodeByteArray(bytes, offset, bytes.size - offset)
            } else {
                // Fallback to standard decoding if no header found
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun findHeaderOffset(bytes: ByteArray): Int {
        // Look for PNG
        val pngIdx = indexOf(bytes, PNG_HEADER)
        if (pngIdx != -1) return pngIdx
        
        // Look for JPEG
        val jpegIdx = indexOf(bytes, JPEG_HEADER)
        if (jpegIdx != -1) return jpegIdx
        
        return -1
    }

    private fun indexOf(source: ByteArray, target: ByteArray): Int {
        if (target.isEmpty()) return 0
        outer@ for (i in 0..source.size - target.size) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }
}
