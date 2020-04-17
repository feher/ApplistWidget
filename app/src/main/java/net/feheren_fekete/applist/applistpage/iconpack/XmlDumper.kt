package net.feheren_fekete.applist.applistpage.iconpack

import android.content.res.XmlResourceParser
import org.xmlpull.v1.XmlPullParser
import java.util.*
import kotlin.collections.HashMap


object XmlDumper {

    const val TAG = "DUMP"


    private var namespaces = HashMap<String, String>()
    private var namespaceStack = Stack<NamespaceUriDepthPair>()

    fun dump(p: XmlResourceParser): String {
        val result = StringBuilder()

        namespaces.clear()
        namespaces.put("http://schemas.android.com/apk/res/android", "android")
        namespaceStack.clear()

        var nextEvent = p.next()
        val encoding = p.inputEncoding

        result.append(String.format("<?xml%s?>",
                if (encoding == null) "" else " encoding=\"$encoding\""))

        var depth = 1
        var extraBlankLine = false
        while (nextEvent != XmlPullParser.END_DOCUMENT) {
            if (nextEvent == XmlPullParser.START_TAG) {
                depth = p.depth
                var uri = p.namespace
                pushNamespaceStack(uri, depth)
                var prefix = getNamespacePrefix(uri)
                val sb = StringBuilder(getIndent(depth - 1) + "<" +
                        if (prefix == "") p.name else prefix + ":" + p.name)
                val attrCnt = p.attributeCount
                if (attrCnt > 0) {
                    for (i in 0 until attrCnt) {
                        uri = p.getAttributeNamespace(i)
                        pushNamespaceStack(uri, depth)
                        prefix = getNamespacePrefix(uri)
                        sb.append(String.format(
                                if (i == 0) " %s=\"%s\"" else "\n" + getIndent(depth) + "%s=\"%s\"",
                                if (prefix == "") p.getAttributeName(i) else prefix + ":" + p.getAttributeName(i),
                                p.getAttributeValue(i)
                        ))
                    }
                }

                for (pair in namespaceStack) {
                    if (pair.depth == depth) {
                        sb.append(String.format("\n" + getIndent(depth) + "xmlns:%s=\"%s\"",
                                getNamespacePrefix(pair.uri),
                                pair.uri
                        )
                        )
                    }
                }

                if (p.isEmptyElementTag) {
                    sb.append("/>\n")
                    p.next()
                    popNamespaceStack(depth)
                    depth--
                } else {
                    sb.append(">\n")
                }

                if (extraBlankLine) {
                    result.append("\n")
                    extraBlankLine = false
                }

                result.append(sb.toString())
            } else if (nextEvent == XmlPullParser.END_TAG) {
                result.append(getIndent(depth - 1) + "</" + p.name + ">\n")
                extraBlankLine = true
                popNamespaceStack(depth)
                depth--
            } else if (nextEvent == XmlPullParser.TEXT) {
                result.append(p.text)
            }

            nextEvent = p.next()
        }

        return result.toString()
    }

    private fun getIndent(depth: Int): String {
        val sb = StringBuilder()
        for (i in 0 until depth) {
            sb.append("    ")
        }
        return sb.toString()
    }

    private var currentPrefix = ('a'.toInt() - 1).toChar()
    private fun getNextPrefix(): String {
        return String(charArrayOf(++currentPrefix))
    }

    private fun getNamespacePrefix(namespaceUri: String?): String {
        var prefix = ""
        if (namespaceUri != null && "" != namespaceUri) {
            val p = namespaces.get(namespaceUri)
            if (p == null) {
                prefix = getNextPrefix()
                namespaces.put(namespaceUri, prefix)
            } else {
                prefix = p
            }
        }
        return prefix
    }

    private class NamespaceUriDepthPair(var uri: String, var depth: Int)

    private fun pushNamespaceStack(uri: String?, depth: Int) {
        if (uri == null || uri == "") {
            return
        }

        for (p in namespaceStack) {
            if (p.uri.equals(uri)) {
                return
            }
        }

        namespaceStack.push(NamespaceUriDepthPair(uri, depth))
    }

    private fun popNamespaceStack(depth: Int) {
        while (!namespaceStack.empty() && namespaceStack.peek().depth == depth) {
            namespaceStack.pop()
        }
    }
}
