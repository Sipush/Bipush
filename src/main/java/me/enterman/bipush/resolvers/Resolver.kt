package me.enterman.bipush.resolvers

import org.objectweb.asm.tree.ClassNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class Resolver {
    
    val logger:Logger = LoggerFactory.getLogger(this::class.java)
    abstract fun doOn(classes: Map<String,ClassNode>) : Boolean
}