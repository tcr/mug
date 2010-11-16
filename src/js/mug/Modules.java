package mug;
import mug.js.JSModule;

public class Modules {
	static public JSModule getModule(String path) throws ClassNotFoundException, InstantiationException, IllegalAccessException {		
		Class mdClass = Modules.class.getClassLoader().loadClass("mug.modules." + path.replaceAll("-", "_"));
		return (JSModule) mdClass.newInstance();
	}
}
