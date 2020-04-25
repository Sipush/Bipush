package me.enterman.bipush.resolvers

import org.objectweb.asm.tree.ClassNode

class ConstArray {
	companion object:Resolver(){
		override fun doOn(classes: Map<String, ClassNode>): Boolean {
			return false
		}
	}
}