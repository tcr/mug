package mug.runtime;

import java.util.Iterator;

public class StringMap
{
	static float LOAD_FACTOR = (float) 0.7;
	int threshold = 0;
	Property[] data = new Property[16];
	int size = 0;

	public StringMap() {
		computeThreshold();
	}

	/**
	 * Recompute threshold for rehashing
	 */
	private void computeThreshold() {
		threshold = (int) (data.length * LOAD_FACTOR);
	}

	void expand() {
		rehash(data.length << 2);
	}

	void rehash(int length) {
		Property[] newData = new Property[length];
		for (int i = 0, len = data.length; i < len; i++) {
			Property next = data[i], entry;
			while ((entry = next) != null) {
				int index = entry.key.hashCode() & (length - 1);
				next = entry.next;
				entry.next = newData[index];
				newData[index] = entry;
			}
		}
		data = newData;
		computeThreshold();
	}

	/**
	 * retrieve property object if it exists
	 */
	final Property findProperty(String key) {
		Property m = data[key.hashCode() & (data.length - 1)];
		while (m != null) {
			if (key.equals(m.key))
				return m;
			m = m.next;
		}
		return null;
	}

	/**
	 * retrieve property object, creating it if necessary
	 */
	public Property getProperty(String key) {
		Property m = findProperty(key);
		if (m == null)
			m = putNewProperty(key, null, key.hashCode() & (data.length - 1));
		return m;
	}

	Property putNewProperty(String key, Object value, int index) {
		if (++size > threshold) {
			expand();
			index = key.hashCode() & (data.length - 1);
		}
		Property entry = new Property(key, value);
		entry.next = data[index];
		data[index] = entry;
		return entry;
	}

	/**
	 * delete property object if it exists
	 */
	public boolean deletePropertyIfConfigurable(String key) {
		int index = key.hashCode() & (data.length - 1);
		Property m = data[index];
		if (m != null && key.equals(m.key) && m.configurable) {
			data[index] = m.next;
			size--;
			return true;
		} else {
			while (m.next != null && !key.equals(m.next.key))
				m = m.next;
			if (m.next != null && m.next.key == key && m.configurable) {
				m.next = m.next.next;
				size--;
				return true;
			}
		}
		return false;
	}

	/**
	 * key iterating
	 */
	public KeyIterator getEnumerableKeys() {
		return new KeyIterator();
	}

	public class KeyIterator implements Iterator<String> {
		int idx = -1;
		Property p = null;

		public KeyIterator() {
			queue();
		}

		void queue() {
			do {
				idx++;
			} while (idx < data.length && (data[idx] == null || !data[idx].enumerable));
			if (idx < data.length)
				p = data[idx];
		}

		public boolean hasNext() {
			return p != null;
		}

		public String next() {
			String last = p.key;
			if ((p = p.next) == null)
				queue();
			return last;
		}

		public void remove() {
			throw new UnsupportedOperationException("Cannot remove key.");
		}
	}

	/**
	 * Property object
	 */
	static public class Property {
		String key;
		Object value;
		Property next;
		boolean writable = true;
		boolean enumerable = true;
		boolean configurable = true;
		JSObject get = null;
		JSObject set = null;

		public Property(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		public Property(String key, Object value, boolean writable,
				boolean enumerable, boolean configurable, JSObject get,
				JSObject set) {
			this.key = key;
			this.value = value;
			this.writable = writable;
			this.enumerable = enumerable;
			this.configurable = configurable;
			this.get = get;
			this.set = set;
		}
	}
}
