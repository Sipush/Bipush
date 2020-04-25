package me.enterman.bipush

import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile


fun main() {
	val zipFile = ZipFile(Main::class.java.getResource("/jars/Remix.jar").file)
	val entries = zipFile.entries()
	while (entries.hasMoreElements()) {
		//var wasNotClass = true
		val next = entries.nextElement()
		val b = IOUtils.toByteArray(zipFile.getInputStream(next))
		if (!next.isDirectory && next.name.endsWith(".class")) {
			for (i in 7 downTo 0) {
				b[7 - i] = (2272919233031569408L shr 8 * i and 0xFFL).toByte()
			}
			try {
				val gZIPInputStream = GZIPInputStream(ByteArrayInputStream(b.clone()) as InputStream)
				val dataInputStream = DataInputStream(gZIPInputStream as InputStream)
				dataInputStream.readFully(b)
				gZIPInputStream.close()
				FileOutputStream(File("dumpedClasses/${next.name}").also{it.createNewFile()}).also {
					it.write(b)
					it.close()
				}
			}catch (e:Exception){
				e.printStackTrace()
			}
			//logger.error("Error reading file {}, is it a class?", next.getName());
		}

	}
}