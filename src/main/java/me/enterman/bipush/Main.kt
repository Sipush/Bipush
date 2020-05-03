package me.enterman.bipush

import me.enterman.bipush.asm.Utils
import me.enterman.bipush.resolvers.general.BipushFold
import me.enterman.bipush.resolvers.unknown.RC4Decrypter
import me.tongfei.progressbar.ProgressBar
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class MainContext(var changes:Boolean,val processClasses:MutableMap<String,ClassNode>)
fun MainContext.perform(operation: MainContext.() -> Boolean){
	if(!changes) {
		changes = operation(this)
	} else {
		operation(this)
	}
}
fun main() {
	Main(File(Main::class.java.getResource("/jars/Remix-Dx8.jar").file),"Remix-DxDebug.jar")
			.lib(File("C:\\Program Files\\AdoptOpenJDK\\jdk-8.0.242.08-hotspot\\jre\\lib"))
			.lib(File("F:/Delphi/libraries/"))
			.doRepeatedly {
		perform {
			println("1")
			BipushFold().doOn(processClasses)
		}
		perform {
			println("2")
			RC4Decrypter.doOn(processClasses)
			false
		}
				perform {
					println("3")
					RC4Decrypter.doOn(processClasses)
					false
				}
	}.write()
}


class Main(val inputFile: File,val outputFilename:String) {
	@JvmField
	var classpath: MutableMap<String, ClassNode> = HashMap()
	var processClasses: MutableMap<String, ClassNode> = HashMap()
	var wasNotClasses: MutableMap<String, ByteArray> = HashMap()
	var logger = LoggerFactory.getLogger(Main::class.java)
	var zipFile: ZipFile
	var zipOut = ZipOutputStream(FileOutputStream(outputFilename))
	fun lib(file:File):Main{
		if (file.isFile && file.name.endsWith(".jar")) {
			try {
				classpath.putAll(loadClasspathFile(file))
			} catch (e: IOException) {
				e.printStackTrace()
			}
		} else {
			val files = file.listFiles()
			if (files != null) {
				for (child in files) {
					try {
						lib(child)
					} catch (e: IOException) {
						e.printStackTrace()
					}
				}
			}
		}
		return this
	}
	init {
		zipFile = ZipFile(inputFile)
		val entries = zipFile.entries()
		while (entries.hasMoreElements()) {
			var wasNotClass = true
			val next = entries.nextElement()
			val data = IOUtils.toByteArray(zipFile.getInputStream(next))
			try {
				if (!next.isDirectory && next.name.endsWith(".class")) {
					//logger.info("Reading Class file {}", next.getName());
					val reader = ClassReader(data)
					val node = ClassNode()
					reader.accept(node, ClassReader.EXPAND_FRAMES)
					processClasses[next.name] = node
					wasNotClass = false
				}
			} catch (e: Exception) {
				//logger.error("Error reading file {}, is it a class?", next.getName());
			}
			if (wasNotClass) {
				wasNotClasses[next.name] = data
			}
		}
		processClasses.forEach { (k: String, v: ClassNode) -> classpath[k.replace(".class", "")] = v }
	}
	
	
	fun doRepeatedly(operation : MainContext.() -> Unit) : Main{
		val context = MainContext(false, processClasses)
		do {
			context.changes = false
			operation(context)
		} while(context.changes)
		return this
	}
	
	fun write(){
		val progress = ProgressBar("Writing Other files", wasNotClasses.size.toLong())
		wasNotClasses.forEach { (name: String?, data: ByteArray?) ->
			try {
				val ze = ZipEntry(name)
				zipOut.putNextEntry(ze)
				zipOut.write(data)
				zipOut.closeEntry()
			} catch (e: IOException) {
				logger.error("Error writing entry {}", name, e)
			}
			progress.step()
		}
		progress.close()
		val progress2 = ProgressBar("Writing processed classess", processClasses.size.toLong())
		processClasses.values.forEach(Consumer { classNode: ClassNode ->
			try {
				val b = Utils.toByteArray(classNode)
				if (b != null) {
					zipOut.putNextEntry(ZipEntry(classNode.name + ".class"))
					zipOut.write(b)
					zipOut.closeEntry()
				}
			} catch (e: IOException) {
				logger.error("Error writing entry {}", classNode.name, e)
			}
			progress2.step()
		})
		progress2.close()
		zipOut.close()
	}
	
	@Throws(IOException::class)
	private fun loadClasspathFile(file: File): Map<String, ClassNode> {
		val map: MutableMap<String, ClassNode> = HashMap()
		val zipIn = ZipFile(file)
		val entries = zipIn.entries()
		while (entries.hasMoreElements()) {
			val ent = entries.nextElement()
			if (ent.name.endsWith(".class")) {
				val reader = ClassReader(zipIn.getInputStream(ent))
				val node = ClassNode()
				reader.accept(node, 0 or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES)
				map[node.name] = node
				//logger.info(node.name);
				// setConstantPool(node, new ConstantPool(reader));
			}
		}
		zipIn.close()
		return map
	}
	
	companion object {
		//@JvmField
		lateinit var INSTANCE: Main
		@JvmField
		var utils = Utils()
		
	}
	
	init {
		INSTANCE = this
	}
}