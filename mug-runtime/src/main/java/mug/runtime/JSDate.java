package mug.runtime;

import java.util.Calendar;

/**
 * 
 */

public class JSDate extends JSObject {
	Calendar value;
	
	public JSDate(JSEnvironment env, Calendar value) {
		super(env, env.getDatePrototype());
		this.value = value;
	}

	/**
	 * Environment
	 */

	public static JSObject createPrototype(JSEnvironment env)
	{
		return new JSObject(env) { {
			defineProperty("valueOf", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					return new Double(ths instanceof JSDate ? ((JSDate) ths).value.getTimeInMillis() : 0);
				}
			});
			
			defineProperty("getDate", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.DATE));
				}
			});
			
			defineProperty("getDay", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.DAY_OF_WEEK));
				}
			});
			
			defineProperty("getFullYear", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.YEAR));
				}
			});
			
			defineProperty("getHours", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.HOUR_OF_DAY));
				}
			});
			
			defineProperty("getMilliseconds", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.MILLISECOND));
				}
			});
			
			defineProperty("getMinutes", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.MINUTE));
				}
			});
			
			defineProperty("getMonth", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.MONTH));
				}
			});
			
			defineProperty("getSeconds", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.SECOND));
				}
			});
			
			defineProperty("getTime", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.getTimeInMillis());
				}
			});
			
			defineProperty("getTimezoneOffset", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.DST_OFFSET)/60);
				}
			});
			
			defineProperty("getDay", new JSFunction(env) {
				@Override
				public Object invoke(Object ths, int argc, Object l0, Object l1, Object l2, Object l3, Object l4, Object l5, Object l6, Object l7, Object[] rest) throws Exception
				{
					Calendar cal = ths instanceof JSDate ? ((JSDate) ths).value : Calendar.getInstance();
					return new Double(cal.get(Calendar.DAY_OF_WEEK));
				}
			});
		} };
	}
}