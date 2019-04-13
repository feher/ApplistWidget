package net.feheren_fekete.applist.applistpage.model

import android.content.Intent

class ShortcutData(id: Long,
                   packageName: String,
                   name: String,
                   customName: String,
                   val intent: Intent) : StartableData(id, packageName, name, customName) {

    private var cachedIntentUri: String? = null

    private val intentUri: String
        get() {
            if (cachedIntentUri == null) {
                cachedIntentUri = intent.toUri(0)
            }
            return cachedIntentUri!!
        }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is ShortcutData) {
            return false
        }
        return intentUri == other.intentUri
    }
}
