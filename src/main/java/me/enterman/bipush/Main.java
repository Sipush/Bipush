package me.enterman.bipush;

import com.google.common.collect.ArrayListMultimap;
import me.enterman.bipush.asm.Utils;
import me.enterman.bipush.resolvers.BipushFold;
import me.enterman.bipush.resolvers.GraphCF;
import me.enterman.bipush.resolvers.LocalConstantField;
import me.enterman.bipush.resolvers.radon.SplitBlocks;
import org.apache.commons.io.IOUtils;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static me.enterman.bipush.asm.Utils.toByteArray;


public class Main implements Opcodes {
	public Main(){
		INSTANCE = this;
	}
	public static Main INSTANCE;
	public static Utils utils = new Utils();
	public Map<String,ClassNode> classpath = new HashMap<>();
	public Map<String,ClassNode> processClasses = new HashMap<>();
	public Map<String,byte[]> wasNotClasses = new HashMap<>();
	public Logger logger = LoggerFactory.getLogger(Main.class);


	public void main() throws Throwable{
		loadLibrary(new File("C:\\Program Files\\AdoptOpenJDK\\jdk-8.0.242.08-hotspot\\jre\\lib"));

		ZipFile zipFile = new ZipFile(Main.class.getResource("/CrackMe.jar").getFile());
		Enumeration<? extends ZipEntry> entries = zipFile.entries();
		while (entries.hasMoreElements()) {
			boolean wasNotClass = true;

			ZipEntry next = entries.nextElement();
			byte[] data = IOUtils.toByteArray(zipFile.getInputStream(next));
			try{
			if (!next.isDirectory() && next.getName().endsWith(".class")) {
				logger.info("Reading Class file {}",next.getName());
				ClassReader reader = new ClassReader(data);
				ClassNode node = new ClassNode();
				reader.accept(node, ClassReader.EXPAND_FRAMES);
				processClasses.put(next.getName(), node);
				wasNotClass = false;
			}
			} catch (Exception e){
				logger.error("Error reading file {}, is it a class?",next.getName());
			}
			if(wasNotClass){
				wasNotClasses.put(next.getName(),data);
			}


		}
		processClasses.forEach((k,v) -> classpath.put(k.replace(".class",""),v));
		boolean changes = true;
		new GraphCF().doOn(processClasses);
		while(changes){
			changes = or(
					new BipushFold().doOn(processClasses),
					//new LocalConstantField().doOn(processClasses)
					new SplitBlocks().doOn(processClasses)
			);
		}

		//new BipushFold().doOn(processClasses);


		ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream("CrackedDonut.jar"));

		wasNotClasses.forEach((name,data) -> {
			try {
				ZipEntry ze = new ZipEntry(name);
				zipOut.putNextEntry(ze);
				zipOut.write(data);
				zipOut.closeEntry();
			} catch (IOException e){
				logger.error("Error writing entry {}",name,e);
			}
		});

		processClasses.values().forEach(classNode -> {
			try {
				byte[] b = toByteArray(classNode);
				if (b != null) {
					zipOut.putNextEntry(new ZipEntry(classNode.name + ".class"));
					zipOut.write(b);
					zipOut.closeEntry();
				}
			} catch (IOException e) {
				logger.error("Error writing entry {}", classNode.name, e);
			}
		});

		zipOut.close();
		//ZipInputStream is = new ZipInputStream(Main.class.getResourceAsStream("/CrackMeInstead.jar"));

	}
	public static void main(String[] args) throws Throwable {

		new Main().main();

	}

	boolean or(boolean b, boolean... b1){
		if(b)
			return true;
		for (boolean value : b1) {
			if (value)
				return true;
		}
		return false;
	}
	public void loadLibrary(File file){
		if (file.isFile()) {
			try {
				classpath.putAll(loadClasspathFile(file, false));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			File[] files = file.listFiles(child -> child.getName().endsWith(".jar"));
			if (files != null) {
				for (File child : files) {
					try {
						classpath.putAll(loadClasspathFile(child, false));
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	private Map<String, ClassNode> loadClasspathFile(File file, boolean skipCode) throws IOException {
		Map<String, ClassNode> map = new HashMap<>();

		ZipFile zipIn = new ZipFile(file);
		Enumeration<? extends ZipEntry> entries = zipIn.entries();
		while (entries.hasMoreElements()) {
			ZipEntry ent = entries.nextElement();
			if (ent.getName().endsWith(".class")) {
				ClassReader reader = new ClassReader(zipIn.getInputStream(ent));
				ClassNode node = new ClassNode();
				reader.accept(node, (skipCode ? 0 : 0) | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
				map.put(node.name, node);

				// setConstantPool(node, new ConstantPool(reader));
			}
		}
		zipIn.close();

		return map;
	}
}
