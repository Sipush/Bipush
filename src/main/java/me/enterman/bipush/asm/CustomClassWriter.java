package me.enterman.bipush.asm;

import me.enterman.bipush.Main;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import static me.enterman.bipush.Main.*;

public class CustomClassWriter extends ClassWriter {

	public CustomClassWriter(int flags) {
		super(flags);
	}

	@Override
	protected String getCommonSuperClass(String type1, String type2) {
		return getCommonSuperClass1(type1, type2);
	}


	private String getCommonSuperClass1(String type1, String type2) {
		if (type1.equals("java/lang/Object") || type2.equals("java/lang/Object")) {
			return "java/lang/Object";
		}
		String a = getCommonSuperClass0(type1, type2);
		String b = getCommonSuperClass0(type2, type1);
		if (!a.equals("java/lang/Object")) {
			return a;
		}
		if (!b.equals("java/lang/Object")) {
			return b;
		}
		ClassNode first = utils.assureLoaded(type1);
		ClassNode second = utils.assureLoaded(type2);
		return getCommonSuperClass(first.superName, second.superName);
	}

	private String getCommonSuperClass0(String type1, String type2) {
		ClassNode first = utils.assureLoaded(type1);
		ClassNode second = utils.assureLoaded(type2);
		if (isAssignableFrom(type1, type2)) {
			return type1;
		} else if (isAssignableFrom(type2, type1)) {
			return type2;
		} else if (Modifier.isInterface(first.access) || Modifier.isInterface(second.access)) {
			return "java/lang/Object";
		} else {
			do {
				type1 = first.superName;
				first = utils.assureLoaded(type1);
			} while (!isAssignableFrom(type1, type2));
			return type1;
		}
	}
	public ClassTree getClassTree(String classNode) {
		ClassTree tree = utils.hierachy.get(classNode);
		if (tree == null) {
			utils.loadHierachyAll(utils.assureLoaded(classNode));
			return getClassTree(classNode);
		}
		return tree;
	}
	private boolean isAssignableFrom(String type1, String type2) {
		if (type1.equals("java/lang/Object"))
			return true;
		if (type1.equals(type2)) {
			return true;
		}
		utils.assureLoaded(type1);
		utils.assureLoaded(type2);
		ClassTree firstTree = getClassTree(type1);
		Set<String> allChilds1 = new HashSet<>();
		LinkedList<String> toProcess = new LinkedList<>();
		toProcess.addAll(firstTree.subClasses);
		while (!toProcess.isEmpty()) {
			String s = toProcess.poll();
			if (allChilds1.add(s)) {
				utils.assureLoaded(s);
				ClassTree tempTree = getClassTree(s);
				toProcess.addAll(tempTree.subClasses);
			}
		}
		return allChilds1.contains(type2);
	}
}
