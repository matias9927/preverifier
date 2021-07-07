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

import java.nio.file.Files;
import java.nio.file.Path;
import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import jdk.internal.vm.Preverifier;

public class PreverifierTool {
	public static void main(String[] args) {
		byte[] newClassBytes = Preverifier.patch(args);
		String fileName;
		if (args[0].contains("/")) {
				fileName = args[0].substring(args[0].lastIndexOf("/"));
			}
			else {
				fileName = args[0];
			}
		try {
        	Path tmpDir;
        	if (!Files.exists(Path.of("/tmp/preverifier/"))) {
        		tmpDir = Files.createDirectory(Path.of("/tmp/preverifier/"));	
        	}
        	else {
        		tmpDir = Path.of("/tmp/preverifier/");
        	}
        	Path tmpFile = Path.of(tmpDir.toString() + fileName + ".class");
        	Files.write(tmpFile, newClassBytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new Error("Cannot write file", e);
        }
	}
}