package net.feheren_fekete.applist.utils.glide

import com.bumptech.glide.load.Key
import com.bumptech.glide.signature.ObjectKey
import java.security.MessageDigest

class AppIconSignature(appVersionCode: Long): Key {

    private val objectKey = ObjectKey(appVersionCode.toString())

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        objectKey.updateDiskCacheKey(messageDigest)
    }

    override fun toString(): String {
        return objectKey.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is AppIconSignature) {
            return objectKey == other.objectKey
        }
        return false
    }

    override fun hashCode(): Int {
        return objectKey.hashCode()
    }

}
