package net.zhuoweizhang.mcpelauncher;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

import org.mozilla.javascript.*;

public class CoffeeScriptCompiler {


	public static boolean isCoffeeScript(File input) {
		return input.getName().toLowerCase().endsWith(".coffee") || isLiterateCoffeeScript(input);
	}

	public static boolean isLiterateCoffeeScript(File input) {
		return input.getName().toLowerCase().endsWith(".litcoffee");
	}

	public static String outputName(String input) {
		return input.substring(0, input.lastIndexOf(".")) + ".js";
	}

	public static void compileFile(android.content.Context androidContext, File input, File output) throws IOException {
		//read the file
		//call compile with the string
		//write the string to the new file
		InputStream is = new FileInputStream(input);
		int length = (int) input.length();
		byte[] data = new byte[length];
		is.read(data);
		is.close();
		Charset utf8 = Charset.forName("UTF-8");
		String inputString = new String(data, utf8);
		String outputString = compile(androidContext, inputString, isLiterateCoffeeScript(input));
		OutputStream os = new FileOutputStream(output);
		os.write(outputString.getBytes(utf8));
		os.close();
	}

	public static String compile(android.content.Context androidContext, String input, boolean literate) {
		//set up Rhino
		//load coffee-script.js from assets and execute it
		//grab the compile function
		//invoke it with our parameters (bare, literate if appropriate, no header)
		//return the compiled string
		InputStream in;
		try {
			in = androidContext.getAssets().open("coffeescript/coffee-script.js");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			Context ctx = Context.enter();
			Script compilerScript = ctx.compileReader(reader, "coffeescript/coffee-script.js", 0, null);
			Scriptable scope = ctx.initStandardObjects();
			compilerScript.exec(ctx, scope);
			//at this point, the CoffeeScript compiler has been initialized in the Rhino context.

			Scriptable compilerParams = ctx.newObject(scope);
			ScriptableObject.putProperty(compilerParams, "bare", true); //bare mode
			ScriptableObject.putProperty(compilerParams, "literate", literate); //bare mode

			Scriptable coffeeScript = (Scriptable) ScriptableObject.getProperty(scope, "CoffeeScript");
			Function coffeeScriptCompile = (Function) ScriptableObject.getProperty(coffeeScript, "compile");

			String output = (String) coffeeScriptCompile.call(ctx, scope, scope, new Object[] {input, compilerParams});
			Context.exit();
			return output;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
