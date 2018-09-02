package com.example.in2de.photobomber

import android.os.Environment
import android.os.StatFs

class StorageUtils {
    fun externalMemoryAvailable(): Boolean {
        return android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
    }

    fun getPercentageUtilizationAfterOperation(totalByesOfOperation:Long):Int{
        val memUsed = getTotalExternalMemorySizeInBytes() - getAvailableExternalMemorySizeInBytes() +
                totalByesOfOperation
        val percentageUsed = (memUsed*100/getTotalExternalMemorySizeInBytes())
        return percentageUsed.toInt()
    }

    fun getAvailableExternalMemorySizeInBytes(): Long {
        if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.getPath())
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            return (availableBlocks * blockSize)
        } else {
            return -1L
        }
    }

    fun getTotalExternalMemorySizeInBytes(): Long {
        if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.getPath())
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return (totalBlocks * blockSize)
        } else {
            return -1L
        }
    }

    fun getAvailableExternalMemorySize(): String {
        if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.getPath())
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            return formatSize(availableBlocks * blockSize)
        } else {
            return "ERROR"
        }
    }

    fun getTotalExternalMemorySize(): String {
        if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.getPath())
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return formatSize(totalBlocks * blockSize)
        } else {
            return "ERROR"
        }
    }

    fun getAvailableExternalMemorySizeInMiB(): Int {
        if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.getPath())
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            return ((availableBlocks * blockSize) / 1048576).toInt()
        } else {
            return -1
        }
    }

    fun getTotalExternalMemorySizeInMiB(): Int {
        if (externalMemoryAvailable()) {
            val path = Environment.getExternalStorageDirectory()
            val stat = StatFs(path.getPath())
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            return ((totalBlocks * blockSize) / 1048576).toInt()
        } else {
            return -1
        }
    }

    fun formatSize(size: Long): String {
        var size = size
        var suffix: String? = null

        if (size >= 1024) {
            suffix = "KB"
            size /= 1024
            if (size >= 1024) {
                suffix = "MB"
                size /= 1024
                if (size >= 1024) {
                    suffix = "GB"
                    size /= 1024
                }
            }
        }

        val resultBuffer = StringBuilder(java.lang.Long.toString(size))

        var commaOffset = resultBuffer.length - 3
        while (commaOffset > 0) {
            resultBuffer.insert(commaOffset, ',')
            commaOffset -= 3
        }

        if (suffix != null) resultBuffer.append(suffix)
        return resultBuffer.toString()
    }
}