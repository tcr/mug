package mug.runtime;

public abstract class JSModule {
	public JSObject load() throws Exception {
		return load(new JSEnvironment());
	}
	public abstract JSObject load(JSEnvironment env) throws Exception;
}
