package com.spzgroup.spz.data

import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipInputStream

// An EPUB is a zip file: container.xml points to an .opf file,
// and the .opf file lists the chapters in reading order.
object EpubReader {

    // Unzips the EPUB if needed and returns the chapter files in reading order
    fun open(epubFile: File, targetDir: File): List<File> {
        if (!File(targetDir, "META-INF/container.xml").exists()) {
            try {
                unzip(epubFile, targetDir)
            } catch (e: Exception) {
                targetDir.deleteRecursively() // do not keep a half-unzipped book
                throw e
            }
        }
        val opfFile = findOpfFile(targetDir)
        return readChapters(opfFile, targetDir)
    }

    private fun unzip(zipFile: File, targetDir: File) {
        targetDir.mkdirs()
        val rootPath = targetDir.canonicalPath + File.separator

        ZipInputStream(FileInputStream(zipFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                // Refuse entries like "../../x" that try to leave the folder
                if (!outFile.canonicalPath.startsWith(rootPath)) {
                    throw IOException("Bad zip entry: ${entry.name}")
                }
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { zip.copyTo(it) }
                }
                entry = zip.nextEntry
            }
        }
    }

    private fun findOpfFile(rootDir: File): File {
        var opfPath: String? = null
        parseXml(File(rootDir, "META-INF/container.xml")) { parser ->
            while (opfPath == null && parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "rootfile") {
                    opfPath = parser.getAttributeValue(null, "full-path")
                }
            }
        }
        val path = opfPath ?: throw IOException("No OPF file in this EPUB")
        return File(rootDir, path)
    }

    private fun readChapters(opfFile: File, rootDir: File): List<File> {
        val baseDir = opfFile.parentFile ?: return emptyList()
        val rootPath = rootDir.canonicalPath + File.separator

        val manifest = mutableMapOf<String, String>() // id -> file path
        val spine = mutableListOf<String>() // ids in reading order

        parseXml(opfFile) { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType != XmlPullParser.START_TAG) continue
                when (parser.name) {
                    "item" -> {
                        val id = parser.getAttributeValue(null, "id")
                        val href = parser.getAttributeValue(null, "href")
                        val type = parser.getAttributeValue(null, "media-type") ?: ""
                        // Only the text chapters, not images or stylesheets
                        if (id != null && href != null && type.contains("html")) {
                            manifest[id] = href
                        }
                    }
                    "itemref" -> parser.getAttributeValue(null, "idref")?.let { spine.add(it) }
                }
            }
        }

        return spine
            .mapNotNull { manifest[it] }
            .map { File(baseDir, Uri.decode(it)) }
            .filter { it.exists() && it.canonicalPath.startsWith(rootPath) }
    }

    private fun parseXml(file: File, block: (XmlPullParser) -> Unit) {
        FileInputStream(file).use { stream ->
            val parser = Xml.newPullParser()
            parser.setInput(stream, null)
            block(parser)
        }
    }
}