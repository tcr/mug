package mug.js;

public class CopyOfJSArray extends JSObject {
	public CopyOfJSArray(JSObject proto) {
		super(proto);
	}
	
	/*
	 * API functions
	 */
	
	public void push(JSPrimitive value) {
		super.set(String.valueOf(_length), value);
		_length++;
	}
	
	public JSPrimitive pop() {
		JSPrimitive value = super.get(String.valueOf(_length));
		super.set(String.valueOf(_length), null);
		_length--;
		return value;
	}
	
	public int getLength() {
		return _length;
	}
	
	public void append(JSPrimitive[] arr) {
		for (JSPrimitive value : arr)
			push(value);
	}
	
	/*
	 * length property
	 */
	
	int _length = 0;
	
	public JSPrimitive get(String key) {
		return key.equals("length") ? new JSNumber(_length) : super.get(key);
	}
	
	public void set(String key, JSPrimitive value) {
		if (key == "length") {
			double dbl = JSUtils.asNumber(value);
			int len = (int) dbl;
			if (dbl == Double.NaN || len != dbl)
				return;
			// to contract, delete hash values
			if (_length > len)
				for (int i = len; i < _length; i++)
					hash.remove(String.valueOf(i));
			_length = len;
			return;
		}
		super.set(key, value);
		
		// for setting higher indices, 
		try {
			int index = ((int) Double.parseDouble(key));
			if (index + 1 > _length)
				_length = index+1;
		} catch (Exception e) { }
	}
}
