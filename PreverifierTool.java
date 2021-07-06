public class PreverifierTool {
	public static void main(String[] args) {
		Preverifier p = new Preverifier();
		byte[] newClassBytes = p.patch(args);
		String fileName = args[0];
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