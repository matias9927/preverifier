public class helloworld {
	public static int add(int x, int y) {
		return x+y;
	}

	 static boolean surpriseTheProgrammer(boolean bVal) {
	 	while (bVal) {
	 		try {
				return true;
			} finally {
				break;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		System.out.println("Hello World");
		System.out.println(add(3,2));
	}
}
