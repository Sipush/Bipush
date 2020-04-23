package me.enterman.bipush.utils

import com.google.common.graph.GraphBuilder
import com.google.common.graph.MutableGraph
import me.enterman.bipush.NodeUtils.terminates
import org.objectweb.asm.Label
import org.objectweb.asm.tree.*
@Suppress("UnstableAPIUsage")
object Graphs {
	fun fromMethod(methodNode: MethodNode):GraphData{
		val graph = GraphBuilder
				.directed()
				.allowsSelfLoops(true)
				.build<Int>()
		val hadNeg1 = methodNode.instructions.first !is LabelNode
		if (hadNeg1)
			graph.addNode(-1)

		val labelMap = mutableMapOf<Label, Int>()
		methodNode.instructions.forEach {
			if (it is LabelNode) {
				graph.addNode(labelMap.size)
				labelMap[it.label] = labelMap.size
			}
		}
		methodNode.instructions.forEach { inst ->
			when (inst) {
				is LabelNode -> {
					val thisInt = labelMap[inst.label]!!
					var prev = inst.previous
					while (true) {
						if (prev == null || prev is LabelNode) {
							if (graph.nodes().contains(thisInt - 1))
								graph.putEdge(thisInt - 1, thisInt)
							break
						}
						if (prev.terminates())
							break
						prev = prev.previous
					}
				}
				else -> {
					val thisInt: Int
					var prev = inst.previous
					while (true) {
						if (prev == null) {
							thisInt = -1
							break
						}
						if (prev is LabelNode) {
							thisInt = labelMap[prev.label]!!
							break
						}
						prev = prev.previous
					}
					//@Suppress("Duplicates")
					when (inst) {
						is JumpInsnNode -> {
							graph.putEdge(thisInt, labelMap[inst.label.label]!!)
						}
						is LookupSwitchInsnNode -> {
							inst.labels.forEach {
								graph.putEdge(thisInt, labelMap[it.label]!!)
							}
							graph.putEdge(thisInt, labelMap[inst.dflt.label]!!)
						}
						is TableSwitchInsnNode -> {
							inst.labels.forEach {
								graph.putEdge(thisInt, labelMap[it.label]!!)
							}
							graph.putEdge(thisInt, labelMap[inst.dflt.label]!!)
						}
					}
				}

			}
		}
		methodNode.tryCatchBlocks?.let { list ->
			list.forEach { node ->
				val labels = mutableListOf<Label>(node.start.label)
				var next = node.start.next
				while (next != null) {
					if (next == node.end) break
					if (next is LabelNode)
						labels.add(next.label)
					next = next.next
				}
				labels.map { label -> labelMap[label]!! }.forEach { from ->
					graph.putEdge(from, labelMap[node.handler.label]!!)
				}
			}
		}
		return GraphData(graph, hadNeg1, labelMap)
	}
}
@Suppress("UnstableAPIUsage")
data class GraphData(val graph:MutableGraph<Int>,val hadNeg1:Boolean, val labelMap: MutableMap<Label,Int>)