package util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** tool for porting BEAST 1 classes to BEAST 2 by wrapping them in a Plugin **/
public class WrapperTool {

	private static void printUsageAndExit() {
		System.err.println("Usage: java util.WrapperTool <beast1 class> <beast1 src path>\n"
				+ "Creates a wrapper class that allows using beast1 classes in beast2\n"
				+ "<beast1 class> name of the class to create a wrapper for\n"
				+ "<beast1 src path> path of the source class\n"
				+ "Example: java util.WrapperTool dr.evomodel.clock.UniversalClock ../beast-mcmc\n"
				+ "Note that beast1 must be in the class path!");
		System.exit(0);
	}

	private static void processClass(String className, String srcPath) {
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
			String[] inputNames = guessInputNames(srcPath, className, pType.length);

			for (int i = 0; i < pType.length; i++) {
				Type[] gpType = constructorWithMostArguments.getGenericParameterTypes();

				for (int j = 0; j < gpType.length; j++) {
					String type = typeToString(gpType[j]);
					if (type.startsWith("class")) {
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
						imports.addAll(typeToClasses(gpType[j]));
						type = typeToShortString(gpType[j]);
					}
					inputs += "    public Input<" + type + "> " + inputNames[j] + " = new Input<" + type + ">(\""
							+ inputNames[j] + "\", \"description here\");\n";
				}
				break;
			}

			String methods = "";
			String objectName = shortClass.toLowerCase();

			Method[] pMethods = c.getDeclaredMethods();
			for (int i = 0; i < pMethods.length; i++) {
				Method method = pMethods[i];
				if (true || method.isAccessible()) { // TODO: test whether this
														// is a public method
														// that we want
					methods += "    " + typeToShortString(method.getGenericReturnType()) + " " + method.getName() + "(";
					imports.addAll(typeToClasses(method.getGenericReturnType()));
					String[] argNames = guessArgnames(method);
					Type[] args = method.getGenericParameterTypes();
					for (int j = 0; j < args.length; j++) {
						methods += typeToShortString(args[j]);
						imports.addAll(typeToClasses(args[j]));
						methods += " " + argNames[j];
						if (j < args.length - 1) {
							methods += ", ";
						}
					}
					methods += ") {\n";
					methods += "        ";
					if (!method.getGenericReturnType().toString().equals("void")) {
						methods += "return ";
					}
					methods += objectName + "." + method.getName() + "(";
					for (int j = 0; j < args.length; j++) {
						methods += argNames[j];
						if (j < args.length - 1) {
							methods += ", ";
						}
					}
					methods += ");\n";
					methods += "     }\n";
				}
			}

			System.out.println("package beast."
					+ className.substring(className.indexOf(".") + 1, className.lastIndexOf('.')) + ";\n\n");

			imports.remove("java.lang.Integer");
			imports.remove("java.lang.Object");
			imports.remove("java.lang.String");
			imports.remove(className);
			String[] s = imports.toArray(new String[0]);
			Arrays.sort(s);
			for (String importName : s) {
				if (importName.indexOf("[]") < 0 && importName.indexOf('.') > 0)
					System.out.println("import " + importName + ";");
			}
			System.out.println("\n\n@Description(\"...\")");
			System.out.println("class " + className.substring(className.lastIndexOf('.') + 1) + " extends Plugin {");
			System.out.println(inputs);
			System.out.println("\n");
			System.out.println("    " + className + " " + objectName + ";");
			System.out.println("\n");

			System.out.println("    @Override");
			System.out.println("    public void initAndValidate() throws Exception {");
			System.out.println("        " + objectName + " = new " + className + "(");
			for (int j = 0; j < pType.length; j++) {
				System.out.print("                             " + inputNames[j] + ".get()");
				if (j < pType.length - 1) {
					System.out.println(",");
				}
			}
			System.out.println(");");
			System.out.println("    }");
			System.out.println("\n");
			System.out.println(methods);
			System.out.println("}");
		} catch (ClassNotFoundException x) {
			throw new RuntimeException("Could not find class " + className + " in the classpath.");
		}

	}

	private static String[] guessArgnames(Method method) {
		Type[] args = method.getGenericParameterTypes();
		String[] argNames = new String[args.length];
		for (int i = 0; i < args.length; i++) {
			argNames[i] = "arg" + i;
		}
		try {
			// build regexp
			String regexp = ".*" + method.getName() + "\\s*\\(";
			for (int i = 0; i < args.length; i++) {
				regexp += "\\s*";
				regexp += "(" + typeToShortString(args[i]) + "[^,\\)]*)";
				if (i < args.length - 1) {
					regexp += ",";
				}
			}
			regexp += ".*\\).*";
			System.err.println(regexp);

			// match regexp
			Pattern pattern = Pattern.compile(regexp, Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(javatext);
			matcher.find();
			for (int i = 0; i < args.length; i++) {
				String param = matcher.group(i + 1);
				param = param.replaceAll("^\\s+", "");
				param = param.replaceAll("\\s+$", "");
				param = param.split(" ")[1];
				argNames[i] = param;
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}

		return argNames;
	}

	private static String typeToString(Type type) {
		String sType = type.toString();
		if (sType.indexOf(' ') >= 0) {
			String[] sTypes = sType.split(" ");
			if (sTypes[1].equals("[D")) {
				sType = "double []";
			} else if (sTypes[1].equals("[[D")) {
				sType = "double [][]";
			} else if (sTypes[1].equals("[I")) {
				sType = "int []";
			} else if (sTypes[1].equals("[S")) {
				sType = "String []";
			} else if (sTypes[sTypes.length - 1].charAt(0) == '[') {
				sType = sTypes[1].substring(2, sTypes[1].length() - 1) + " []";
			} else {
				sType = sTypes[1];
			}
		}
		sType = sType.replaceAll("\\$", ".");
		return sType;
	}

	private static String typeToShortString(Type type) {
		String sType = typeToString(type);
		if (sType.indexOf('.') >= 0) {
			String[] sTypes = sType.split("<");
			sType = sTypes[0].substring(sTypes[0].lastIndexOf('.') + 1);
			for (int i = 1; i < sTypes.length; i++) {
				sType += "<" + sTypes[i].substring(sTypes[i].lastIndexOf('.') + 1);
			}
		}
		return sType;
	}

	private static List<String> typeToClasses(Type type) {
		List<String> sPackages = new ArrayList<String>();
		String sType = typeToString(type);
		sType = sType.replaceAll(">", "");
		if (sType.indexOf('<') >= 0) {
			String[] sTypes = sType.split("<");
			sPackages.add(sTypes[0].replaceAll("\\[\\]", ""));
			for (int i = 1; i < sTypes.length; i++) {
				sPackages.add(sTypes[i].replaceAll("\\[\\]", ""));
			}
		} else {
			sPackages.add(sType);
		}
		return sPackages;
	}

	static String javatext;

	private static String[] guessInputNames(String srcPath, String className, int length) {
		String sFileName = srcPath + "/" + className.replaceAll("\\.", "/") + ".java";
		try {
			// grab file text
			BufferedReader fin = new BufferedReader(new FileReader(sFileName));
			StringBuffer buf = new StringBuffer();
			String sStr = null;
			while (fin.ready()) {
				sStr = fin.readLine();
				buf.append(sStr);
				buf.append('\n');
			}
			fin.close();
			javatext = buf.toString();

			// remove comments
			javatext = javatext.replaceAll("//[^\n]*\n", "");
			javatext = javatext.replaceAll("(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)", "");
			String shortClass = className.substring(1 + className.lastIndexOf('.'));
			javatext = javatext.replaceAll(".*" + shortClass, shortClass);

			// build regexp
			String[] inputNames = new String[length];
			String regexp = ".*" + className.substring(1 + className.lastIndexOf('.')) + "\\s*\\(";
			for (int i = 0; i < length; i++) {
				regexp += "([^,\\)]*)";
				if (i < length - 1) {
					regexp += ",";
				}
			}
			regexp += ".*\\).*";
			// System.err.println(regexp);

			// match regexp
			Pattern pattern = Pattern.compile(regexp, Pattern.MULTILINE);
			Matcher matcher = pattern.matcher(javatext);
			matcher.find();
			for (int i = 0; i < length; i++) {
				String param = matcher.group(i + 1);
				param = param.replaceAll("^\\s+", "");
				param = param.replaceAll("\\s+$", "");
				param = param.split(" ")[1];
				inputNames[i] = param;
			}
			return inputNames;
		} catch (Exception e) {
			System.err.println("Warning: " + e.getMessage());
			String[] inputNames = new String[length];
			for (int i = 0; i < length; i++) {
				inputNames[i] = "input" + i;
			}
			return inputNames;
		}
	}

	public static void main(String[] args) {
		if (args.length != 2) {
			printUsageAndExit();
		}
		String className = args[0];
		String srcPath = args[1];
		processClass(className, srcPath);
	}

}
