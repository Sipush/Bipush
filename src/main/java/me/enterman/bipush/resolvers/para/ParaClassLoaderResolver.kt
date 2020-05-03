package me.enterman.bipush.resolvers.para

import me.enterman.bipush.Main
import me.enterman.bipush.MethodResolver
import me.enterman.bipush.asm.Utils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TypeInsnNode
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.util.zip.GZIPInputStream

object ParaClassLoaderResolver : MethodResolver() {
	fun dump(path: String, i: Int) {
		
		//new BipushFold().doOn(processClasses);
		//val file = ZipFile(this::class.java.getResource("/jars/Remix.jar").file)
		val entry = Main.INSTANCE.zipFile.getEntry(path) ?: throw AssertionError()
		var data = ByteArray(i)
		DataInputStream(Main.INSTANCE.zipFile.getInputStream(entry)).use {
			it.readFully(data,0,it.available())
		}
		
		//logger.info("Reading Class file {}", next.getName());
		data[0] = 0x1F
		data[1] = 0x8B.toByte()
		data[2] = 0x08
		data[3] = 0x00
		data[4] = 0x00
		data[5] = 0x00
		data[6] = 0x00
		data[7] = 0x00
		GZIPInputStream(ByteArrayInputStream(data.clone())).use {
			data = it.readBytes()
		}
		
		//logger.info("Reading Class file {}", next.getName());
		val reader = ClassReader(data)
		val node = ClassNode()
		reader.accept(node, ClassReader.EXPAND_FRAMES)
		val pathNew = path.replace(".class","")
		if(pathNew.startsWith("/"))
			pathNew.replaceFirst("/","")
		node.name = pathNew
		/* Main.INSTANCE.wasNotClasses.keys.first {
			it.contains(path,true) || path.contains(it,true)
		}*/
		Main.INSTANCE.wasNotClasses[path] = Utils.toByteArray(node)
		
	}
	
	override fun resolve(methodNode: MethodNode): Boolean {
		val first = methodNode.instructions.first
		if (first is TypeInsnNode && first.desc == "c/t/d/t" && first.opcode == NEW) {
		
		}
		return false
	}
}