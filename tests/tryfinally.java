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