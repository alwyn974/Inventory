package re.alwyn974.inventory.service

import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.RemoveObjectArgs
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import java.io.ByteArrayInputStream
import java.util.*

class MinioService(
    private val endpoint: String,
    private val accessKey: String,
    private val secretKey: String,
    private val bucketName: String
) {
    private val minioClient = MinioClient.builder()
        .endpoint(endpoint)
        .credentials(accessKey, secretKey)
        .build()

    init {
        // Create bucket if it doesn't exist
        val bucketExists = minioClient.bucketExists(
            BucketExistsArgs.builder().bucket(bucketName).build()
        )

        if (!bucketExists) {
            minioClient.makeBucket(
                MakeBucketArgs.builder().bucket(bucketName).build()
            )
        }
    }

    fun uploadImage(imageData: ByteArray, contentType: String): String {
        val fileName = "images/${UUID.randomUUID()}.${getFileExtension(contentType)}"

        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(fileName)
                .stream(ByteArrayInputStream(imageData), imageData.size.toLong(), -1)
                .contentType(contentType)
                .build()
        )

        return "$endpoint/$bucketName/$fileName"
    }

    fun deleteImage(imageUrl: String): Boolean {
        return try {
            val fileName = extractFileNameFromUrl(imageUrl)
            if (fileName != null) {
                minioClient.removeObject(
                    RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .`object`(fileName)
                        .build()
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getFileExtension(contentType: String): String {
        return when (contentType) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> "jpg"
        }
    }

    private fun extractFileNameFromUrl(imageUrl: String): String? {
        return try {
            val parts = imageUrl.split("/$bucketName/")
            if (parts.size == 2) parts[1] else null
        } catch (e: Exception) {
            null
        }
    }
}
