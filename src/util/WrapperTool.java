package util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class WrapperTool {

	private static void printUsageAndExit() {
		System.err
				.println("Usage: java util.WrapperTool <beast1 class> "
						+ "Creates a wrapper class that allows using beast1 classes in beast2\n"
						+ "<beast1 class> name of the class to create a wrapper for\n"
						+ "Note that beast1 must be in the class path!");
		System.exit(0);
	}

	private static void processClass(String className) {
		String shortClass = className.substring(className.lastIndexOf('.') + 1);
		Set<String> constructorArgs = new HashSet<String>();
		try {
			Class<?> c = Class.forName(className);
			Constructor[] allConstructors = c.getDeclaredConstructors();
			Constructor constructorWithMostArguments = allConstructors[0];
			int nrOfArgs = constructorWithMostArguments.getParameterTypes().length;
			for (Constructor ctor : allConstructors) {
				Class<?>[] pType = ctor.getParameterTypes();
				if (pType.length > nrOfArgs) {
					nrOfArgs = pType.length;
					constructorWithMostArguments = ctor;
				}
			}
			Class<?>[] pType = constructorWithMostArguments.getParameterTypes();

			String inputs = "";
			Set<String> imports = new HashSet<String>();
			imports.add(className);
			imports.add("beast.core.*");
			
			for (int i = 0; i < pType.length; i++) {
				Type[] gpType = constructorWithMostArguments
						.getGenericParameterTypes();

				for (int j = 0; j < gpType.length; j++) {
					String type  = gpType[j].toString();
					if (type.contains(" ")) {
						type = type.split(" ")[1];
					}
					if (type.equals("boolean")) {
						type = "Boolean";
					}
					if (type.equals("int")) {
						type = "Integer";
					}
					if (type.equals("double")) {
						type = "Double";
					}
					if (type.contains(".")) {
						if (type.contains("<")) {
							imports.add(type.substring(0, type.indexOf('<')));
							type = type.substring(type.lastIndexOf('.', type.indexOf('<')) + 1);
						} else {
							imports.add(type);							
							type = type.substring(type.lastIndexOf('.') + 1);
						}

					}
					inputs += "    new Input<" + type + "> input" + j + 
							" = new Input<" + type + ">(\"input" + j + "\", \"description here\");\n";
				}
				break;
			}

			System.out.println("package beast." + className.substring(3) + ";\n\n");

			String [] s = imports.toArray(new String[0]);
			Arrays.sort(s);
			for (String importName : s) {
				System.out.println("import " + importName + ";");
			}
			System.out.println("\n\n@Description(\"...\")");
			System.out.println("class " + className.substring(className.lastIndexOf('.') + 1) + " extends Plugin {");
			System.out.println(inputs);
			System.out.println("\n");
			String objectName = shortClass.toLowerCase();
			System.out.println("    " + shortClass + " " + objectName);
			System.out.println("\n");

			System.out.println("    @Override");
			System.out.println("    public void initAndValidate() throws Exception {");
			System.out.println("        " + objectName + " = new " + shortClass +"(");
			for (int j = 0; j < pType.length; j++) {
				System.out.print("                             input" + j + ".get()");
				if (j < pType.length - 1) {
					System.out.println(",");
				}
			}
			System.out.println(");");
			System.out.println("    }");
			System.out.println("\n");
			
			System.out.println("}");
		} catch (ClassNotFoundException x) {
			throw new RuntimeException("Could not find class " + className
					+ " in the classpath.");
		}

	}

	public static void main(String[] args) {
		if (args.length != 1) {
			printUsageAndExit();
		}
		String className = args[0];
		processClass(className);
	}

}
