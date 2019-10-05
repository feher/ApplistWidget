package net.feheren_fekete.applist.utils.glide

import com.bumptech.glide.load.Key
import com.bumptech.glide.signature.ObjectKey
import java.io.File
import java.security.MessageDigest

class FileSignature(file: File): Key {

    private val objectKey = ObjectKey(file.absolutePath +
            ", lastModified=" + file.lastModified())

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        objectKey.updateDiskCacheKey(messageDigest)
    }

    override fun toString(): String {
        return objectKey.toString()
    }

    override fun equals(o: Any?): Boolean {
        if (o is FileSignature) {
            val other = o as FileSignature
            return objectKey == other.objectKey
        }
        return false
    }

    override fun hashCode(): Int {
        return objectKey.hashCode()
    }

}
