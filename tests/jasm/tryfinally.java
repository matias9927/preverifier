// Test basic try-finally block. This should be compiled in Java 1.3 to ensure JSRs and RETs appear in the bytecode
class tryfinally {
	public static void main(String[] args) {
		tryCatchFinally();
	}

	static void tryCatchFinally() {
	    try {
	        tryItOut();
	    } catch (Exception e) {
	        handleExc(e);
	    } finally {
	        wrapItUp();
	    }
	}

	public static void tryItOut() {}
	public static void handleExc(Exception e) {}
	public static void wrapItUp() {}
}