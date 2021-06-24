/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Opcodes;
import jdk.internal.org.objectweb.asm.util.*;
import jdk.internal.org.objectweb.asm.tree.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.lang.reflect.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Iterator;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.*;
import java.nio.file.FileSystems;

/**
 * Class that patches a java class file taken as an argument,
 * replacing all JSR and RET instructions with a valid equivalent
 */
public class Preverifier extends ClassVisitor {
	
	private static HashSet<String> targetMethods = new HashSet<String>(); // Set containing each method with the desired opcode
	private static byte[] bytecode;
	private static ClassNode cn;

	public static void main(String[] args) {
        ClassReader cr;
        Path filePath;
		if (args[0]==null) {
			System.out.println("Must pass in a class file!");
			System.exit(-1);
		}
		else {
			System.out.println("Patching " + args[0] + ".class......");
		}

		try {
			filePath = FileSystems.getDefault().getPath(args[0]+".class");
		} catch (Exception e) {
			throw new Error("File not found", e);
		}
        try (FileInputStream fis = new FileInputStream(filePath.toFile())) {
			bytecode = readStream(fis, true);
            targetMethods = new HashSet<String>();
			cr = new ClassReader(bytecode);
			viewByteCode(cr, bytecode, "JSR");
			/*int offset = cr.header;	
			int magic = cr.readInt(0);
			int minor_version = cr.readUnsignedShort(4);
			int major_version = cr.readUnsignedShort(6);
			System.out.printf("Major: %d, Minor: %d, Magic: %x\n", major_version, minor_version, magic);*/
			cn = replaceOpcodes(cr, bytecode);
        } catch (IOException e) {
            throw new Error("Error reading file", e);
        }
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        //cn.accept(new InvokeDynamicPatcher(Opcodes.ASM5, cw), cr.EXPAND_FRAMES);
        cn.accept(cw);
        try {
            Files.write(filePath, cw.toByteArray(),
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public Preverifier(int api, ClassWriter cw) {
        super(api, cw);
    }

	// Convert inputstream of class file into byte array
    private static byte[] readStream(final InputStream inputStream, final boolean close)
            throws IOException {
        if (inputStream == null) {
            throw new IOException("Class not found");
        }
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] data = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                outputStream.write(data, 0, bytesRead);
            }
            outputStream.flush();
            return outputStream.toByteArray();
        } finally {
            if (close) {
                inputStream.close();
            }
        }
    }

	// got this code from: https://www.programcreek.com/java-api-examples/?project_name=brutusin%2Finstrumentation#
	// Reads the bytecode of a class and prints it
	public static void viewByteCode(ClassReader cr, byte[] bytecode, String opcode) throws IOException { // Originally bytes[] bytecode
		System.out.println();
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		final List<MethodNode> mns = cn.methods;
		Printer printer = new Textifier();
		TraceMethodVisitor mp = new TraceMethodVisitor(printer);
		for (MethodNode mn : mns) {
			InsnList inList = mn.instructions;
			System.out.println(mn.name);
			for (int i = 0; i < inList.size(); i++) {
				inList.get(i).accept(mp);
				StringWriter sw = new StringWriter();
				printer.print(new PrintWriter(sw));
				printer.getText().clear();
				System.out.print(sw.toString());
				if (sw.toString().contains(opcode)) {
					//System.out.println("Found Opcode: "+ opcode);
					targetMethods.add(mn.name);
				}
			}
		}
		System.out.println();
	}

	// Builds map for cloning instructions
	public static Map<LabelNode, LabelNode> cloneLabels(InsnList insns) {
		HashMap<LabelNode, LabelNode> labelMap = new HashMap<LabelNode, LabelNode>();
		for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
   	    	if (insn.getType() == 8) {
				labelMap.put((LabelNode) insn, new LabelNode());
			}
		}
		return labelMap;
	}

	/*
	 * Replaces JST and RET opcodes in the class file
	 * bytecode: byte array containing the contents of the class file
	 * cr: ClassReader
	 * Returns ClassNode with altered instruction list
	 */
	public static ClassNode replaceOpcodes(ClassReader cr, byte[] bytecode) throws IOException {	
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		List<MethodNode> mns = cn.methods;
		System.out.println("Class name: " + cn.name + "\nMethods: " + mns.size());
		boolean mustExpand = false; // Flag for expanding bytecode when JSRs and RETs overlap
		for (MethodNode mn : mns) {
			if (targetMethods.contains(mn.name)) {
				InsnList inList = mn.instructions;
				InsnList newInst = new InsnList();
				System.out.println("Method name: " + mn.name + " Instructions: " + inList.size());
				// Return label for RET
				LabelNode retLb = null;
				// Map for cloning instructions
				Map<LabelNode, LabelNode> cloneMap = cloneLabels(inList);
				// Maps a RET instruction to the label it must return to once converted to GOTO instruction
				HashMap<AbstractInsnNode, LabelNode> retLabelMap = new HashMap<AbstractInsnNode, LabelNode>();				
				// Set of ASTORE instructions that must be removed
				HashSet<VarInsnNode> astoreToRemove = new HashSet<VarInsnNode>(); 				
				for (int i = 0; i < inList.size(); i++) {
					mustExpand = false;

					if (inList.get(i).getOpcode() == Opcodes.JSR) {
						boolean hasRet = false; // Check if JSR has a matching RET
						System.out.println("Replacing JSR...");
						LabelNode lb = ((JumpInsnNode)inList.get(i)).label;
						
						// Start from the target label and find the next RET instruction
						HashSet<VarInsnNode> astores = new HashSet<VarInsnNode>(); // List of all ASTORE instructions in subroutine
						for(int j = inList.indexOf(lb); j < inList.size(); j++) {
							if (inList.get(j).getOpcode() == Opcodes.RET) {
								hasRet = true;
								if (retLabelMap.containsKey(inList.get(j))) {
									System.out.println("Another JSR points to this RET!");
									mustExpand = true;
								}
								else {
									retLb = new LabelNode(new Label());
									retLabelMap.put(inList.get(j), retLb);
								}
								// Once RET is found, find and remove associated ASTORE
								for (VarInsnNode n : astores) {
									if (n.var == ((VarInsnNode)inList.get(j)).var) {
										// Mark the matching ASTORE to be ignored
										astoreToRemove.add(n);
									}
								}
								break;
							}
							else if (inList.get(j).getOpcode() == Opcodes.ASTORE) {
								astores.add((VarInsnNode)inList.get(j));
							}
						}
						if (!hasRet) {
							throw new Error("Verifier Error. JSR has no matching RET");
						}
						if (mustExpand) {
							System.out.println("Expanding code...");
							for (AbstractInsnNode n = inList.get(inList.indexOf(lb)+2); n.getOpcode() != Opcodes.RET; n=n.getNext()) {
								newInst.add(n.clone(cloneMap));
							}
						}
						else {	
							newInst.add(new JumpInsnNode(Opcodes.GOTO, lb));
							newInst.add(retLb);
						}
					}
					else if (inList.get(i).getOpcode() == Opcodes.RET) {
						System.out.println("Replacing RET...");
						// Replace RET with GOTO which jumps to the label corresponding to its associated JSR
						newInst.add(new JumpInsnNode(Opcodes.GOTO, retLabelMap.get(inList.get(i))));
					}
					else if (inList.get(i).getOpcode() == Opcodes.ASTORE) {
						if (astoreToRemove.contains(inList.get(i))) {
							System.out.println("ASTORE removed");
						}
					}
					else {
						newInst.add(inList.get(i));
					}
				}
				if (astoreToRemove.isEmpty()) {
					throw new Error("Verifier Error");
				}
				inList.clear();
				inList.add(newInst);
				inList.resetLabels(); // Don't know if this is necessary
			}
		} 
		return cn;
    }
}
