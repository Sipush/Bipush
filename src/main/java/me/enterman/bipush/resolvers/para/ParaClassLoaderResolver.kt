package me.enterman.bipush.resolvers.para

import me.enterman.bipush.MethodResolver
import org.apache.commons.io.IOUtils
import org.objectweb.asm.tree.MethodNode
import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

object ParaClassLoaderResolver:MethodResolver() {
	fun dump() {
		
		//new BipushFold().doOn(processClasses);
		val file = ZipFile(this::class.java.getResource("/jars/Remix.jar").file)
		val entries: Enumeration<out ZipEntry?> = file.entries()
		while (entries.hasMoreElements()) {
			val next = entries.nextElement()!!
			val data: ByteArray = IOUtils.toByteArray(file.getInputStream(next))
			try{
				if (!next.isDirectory && next.name.endsWith(".class")) {
					//logger.info("Reading Class file {}", next.getName());
					data[0] = 0x1F
					data[1] = 0x8B.toByte()
					data[2] = 0x08
					data[3] = 0x00
					data[4] = 0x00
					data[5] = 0x00
					data[6] = 0x00
					data[7] = 0x00
					GZIPInputStream(ByteArrayInputStream(data)).use {
						it.
					}
				}
			}catch (e:Exception){}
		}
			ZipOutputStream(FileOutputStream("Remix-GZipped.jar")).use {  }
		
		
	}
	
	override fun resolve(methodNode: MethodNode): Boolean {
	
	}
}