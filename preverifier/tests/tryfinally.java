public class tryfinally {

	public static int m(boolean b) {
		int i;
		try {
			if (b) {
				System.out.println("Return 1");
				return 1;
			}
			i = 2;
			System.out.println("Try\ni=" + i);
		} finally {
			if (b) {
				i = 3;
				System.out.println("Finally\ni=" + i);
			}
		}
		return i;
	}

	public static void main(String[] args) {
		System.out.println(m(true));
		//System.out.println(m(false));
	}
}