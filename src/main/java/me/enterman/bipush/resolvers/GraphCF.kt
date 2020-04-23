package me.enterman.bipush.resolvers

import com.google.common.graph.EndpointPair
import guru.nidi.graphviz.engine.Format
import guru.nidi.graphviz.engine.Graphviz
import guru.nidi.graphviz.engine.GraphvizCmdLineEngine
import guru.nidi.graphviz.parse.Parser
import me.enterman.bipush.utils.Graphs
import org.apache.commons.io.IOUtils
import org.jgrapht.graph.guava.MutableGraphAdapter
import org.jgrapht.nio.dot.DOTExporter
import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import java.io.StringWriter
import java.util.concurrent.TimeUnit

fun main() {
    val data = IOUtils.toByteArray(GraphCF::class.java.getResourceAsStream("/jars/1EntryPoint.class"))
	val reader = ClassReader(data)
	val node = ClassNode()
	reader.accept(node, ClassReader.EXPAND_FRAMES)
	GraphCF("fun").doOn(mutableMapOf("" to node))
}

// const val  = true
@Suppress("UnstableApiUsage")
class GraphCF(private val folderName: String) : Resolver() {
	override fun doOn(classes: Map<String, ClassNode>): Boolean {
		classes.values.forEach { classNode ->
			classNode.methods.forEach { methodNode ->
                export(methodNode,classNode)
			}
		}
		return false
	}
    fun export(method: MethodNode,classNode: ClassNode){
        val data = Graphs.fromMethod(method)
        val graph = data.graph
        val hadNeg1 = data.hadNeg1

        val exporter = DOTExporter<Int, EndpointPair<Int>>()
        val writer = StringWriter()
        exporter.exportGraph(MutableGraphAdapter(graph), writer)
        // println(writer.toString())
        val graph2 = Parser().read(writer.toString())
        graph2.nodes().forEach {
            it.setName((Integer.parseInt(it.name().toString()) - if (hadNeg1) 2 else 1).toString())
        }
        Graphviz.useEngine(GraphvizCmdLineEngine().timeout(1, TimeUnit.DAYS))
        try {
            Graphviz.fromGraph(graph2)
					.totalMemory((1024L * 1024L * 1024L * 4L).toInt())
                    .render(Format.PNG).toFile(File("${folderName}/${classNode.name}/" +
                            method.name
                                    .replace("<", "")
                                    .replace(">", "")
                            + " " +
                            method.desc
                                    .replace("/", ".")
                                    .replace("[", "Arr")
                                    .replace("(", "")
                                    .replace(")", "-")
                            +
                            ".png"))
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
}