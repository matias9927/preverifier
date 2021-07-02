// Test nested try-finally blocks. This should be compiled in Java 1.3 to ensure JSRs and RETs appear in the bytecode
class tryfinallyNested {
	public static void main(String[] args) {
		try {
		  System.out.println("Try!");
		  try {
		     System.out.println("Nested Try!");  // hm need two things to jsr to the finally
		  } catch (NullPointerException e) {
		  } finally {
		     System.out.println("Finally!");
		  }
		} catch (RuntimeException ex) {
		  System.out.println("Catch!");
		}
	}
}