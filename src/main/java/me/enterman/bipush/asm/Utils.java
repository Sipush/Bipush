package me.enterman.bipush.asm;

import me.enterman.bipush.Main;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.CheckClassAdapter;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Utils {
	public Map<String, ClassTree> hierachy = new HashMap<>();
	public List<ClassNode> loadHierachy(ClassNode specificNode) {
		if (specificNode.name.equals("java/lang/Object")) {
			return Collections.emptyList();
		}
		if ((specificNode.access & Opcodes.ACC_INTERFACE) != 0) {
			getOrCreateClassTree(specificNode.name).parentClasses.add("java/lang/Object");
			return Collections.emptyList();
		}
		List<ClassNode> toProcess = new ArrayList<>();

		ClassTree thisTree = getOrCreateClassTree(specificNode.name);
		ClassNode superClass;/*
		if (DELETE_USELESS_CLASSES) {
			superClass = assureLoadedElseRemove(specificNode.name, specificNode.superName);
			if (superClass == null)
				//It got removed
				return toProcess;
		} else*/
			superClass = assureLoaded(specificNode.superName);
		if (superClass == null) {
			throw new IllegalArgumentException("Could not load " + specificNode.name);
		}
		ClassTree superTree = getOrCreateClassTree(superClass.name);
		superTree.subClasses.add(specificNode.name);
		thisTree.parentClasses.add(superClass.name);
		toProcess.add(superClass);

		for (String interfaceReference : specificNode.interfaces) {
			ClassNode interfaceNode;/*
			if (DELETE_USELESS_CLASSES) {
				interfaceNode = assureLoadedElseRemove(specificNode.name, interfaceReference);
				if (interfaceNode == null)
					//It got removed
					return toProcess;
			} else
			*/
				interfaceNode = assureLoaded(interfaceReference);
			if (interfaceNode == null) {
				throw new IllegalArgumentException("Could not load " + interfaceReference);
			}
			ClassTree interfaceTree = getOrCreateClassTree(interfaceReference);
			interfaceTree.subClasses.add(specificNode.name);
			thisTree.parentClasses.add(interfaceReference);
			toProcess.add(interfaceNode);
		}
		return toProcess;
	}
	public void loadHierachyAll(ClassNode classNode) {
		Set<String> processed = new HashSet<>();
		LinkedList<ClassNode> toLoad = new LinkedList<>();
		toLoad.add(classNode);
		while (!toLoad.isEmpty()) {
			for (ClassNode toProcess : loadHierachy(toLoad.poll())) {
				if (processed.add(toProcess.name)) {
					toLoad.add(toProcess);
				}
			}
		}
	}
	private ClassTree getOrCreateClassTree(String name) {
		return this.hierachy.computeIfAbsent(name, ClassTree::new);
	}
	public ClassNode assureLoaded(String ref) {
		ClassNode clazz = Main.INSTANCE.classpath.get(ref);
		if (clazz == null) {
			throw new TypeNotPresentException(ref,new ClassNotFoundException(ref));
		}
		return clazz;
	}

	public static byte[] toByteArray(ClassNode node) {
		if (node.innerClasses != null) {
			node.innerClasses.stream().filter(in -> in.innerName != null).forEach(in -> {
				if (in.innerName.indexOf('/') != -1) {
					in.innerName = in.innerName.substring(in.innerName.lastIndexOf('/') + 1); //Stringer
				}
			});
		}
		CustomClassWriter writer = new CustomClassWriter(ClassWriter.COMPUTE_FRAMES);
		Logger logger = LoggerFactory.getLogger(Utils.class);
		try {
			node.accept(writer);
		} catch (Throwable e) {
			if (e instanceof TypeNotPresentException) {
				TypeNotPresentException ex = (TypeNotPresentException) e;
				logger.error("Type not present: ",ex);
				try{
				ClassWriter nWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
				node.accept(nWriter);}catch (Throwable t){
					logger.error("Error: ",t);
					writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS);
					node.accept(writer);
				}
				//LoggerFactory.getLogger(Utils.class).error("Error: ",ex);
				//System.out.println("Error: " + ex.typeName() + " could not be found while writing " + node.name + ". Using COMPUTE_MAXS");

			} else if (e instanceof NegativeArraySizeException || e instanceof ArrayIndexOutOfBoundsException) {
				System.out.println("Error: failed to compute frames");
				writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS);
				node.accept(writer);
			} else if (e.getMessage() != null) {
				if (e.getMessage().contains("JSR/RET")) {
					System.out.println("ClassNode contained JSR/RET so COMPUTE_MAXS instead");
					writer = new CustomClassWriter(ClassWriter.COMPUTE_MAXS);
					node.accept(writer);
				} else {
					System.out.println("Error while writing " + node.name);
					e.printStackTrace(System.out);
				}
			} else {
				System.out.println("Error while writing " + node.name);
				e.printStackTrace(System.out);
			}
		}
		byte[] classBytes = writer.toByteArray();

		boolean isVerify = false;

		if (isVerify) {
			ClassReader cr = new ClassReader(classBytes);
			//cr.accept(,0);
			try {
				cr.accept(new CheckClassAdapter(new ClassWriter(0)), 0);
			} catch (Throwable t) {
				System.out.println("Error: " + node.name + " failed verification");
				t.printStackTrace(System.out);
			}
		}

		return classBytes;
	}
}
