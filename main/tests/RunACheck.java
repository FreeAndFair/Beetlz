import main.Beetlz;


public class RunACheck {

	/**
	 * @param args
	 */
	public static void main(String[] args) {		
		String current = System.getProperty("java.class.path");
		
		String[] parts = current.split("[:;]");
		
		String path = parts[0];
		
		System.out.println("This is a regression test. First run should not produce any errors.\n" +
				"The second run produces 3 Java errors, 1 Java warnings, 6 JML errors and 5 JML warnings.");
		
		if(path.endsWith("/bin")|| path.endsWith("\\bin")) {
			path = path.substring(0, path.length()-3);
			{
				String[] my_args = {	                
		                "-userSettings", path + "examples/zoo/custom_zoo.txt",
		                "-files", path + "examples/zoo"
		                };
				
				System.out.println("****************** First run *********************");
				final Beetlz checker = new Beetlz(my_args, System.err, System.out);
			    checker.run();
			}
			
			
			{
				String[] my_args = {	                
		                "-userSettings", path + "examples/zoo_faults/custom_zoo.txt",
		                "-files", path + "examples/zoo_faults"
		                };
				
				System.out.println("****************** Second run *********************");
				final Beetlz checker = new Beetlz(my_args, System.err, System.out);
			    checker.run();
			}
			
		} else {
			System.err.println("Error: classpath has wrong format.");
		}
		

	}

}
