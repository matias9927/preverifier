class tryfinallyBytecode {
	void tryCatchFinally() {
	    try {
	        tryItOut();
	    } catch (Exception e) {
	        handleExc(e);
	    } finally {
	        wrapItUp();
	    }
	}

	public void tryItOut() {}
	public void handleExc(Exception e) {}
	public void wrapItUp() {}
}

class tryfinally {
	public static void main(String[] args) {
		tryfinallyBytecode b = new tryfinallyBytecode();
		b.tryCatchFinally();
	}
}