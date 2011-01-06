package mug;
import mug.js.JSModule;

public class Modules {
	static String[] modulepath;
	{
		if (System.getProperty("mug.module.path") == null)
			modulepath = new String[] { ".", "mug.modules" };
		else
			modulepath = System.getProperty("mug.module.path").split(":");
	}
	
	static public JSModule getModule(String path) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class mdClass = Modules.class.getClassLoader().loadClass("mug.modules." + path.replaceAll("-", "_"));
		return (JSModule) mdClass.newInstance();
	}
}
