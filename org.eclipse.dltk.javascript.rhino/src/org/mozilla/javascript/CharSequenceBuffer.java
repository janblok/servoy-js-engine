package org.mozilla.javascript;

import java.io.Serializable;

/**
 * @author jcompagner
 */
public class CharSequenceBuffer implements Serializable, CharSequence, Wrapper {
	private static final long serialVersionUID = 4284364553900666990L;
	private CharSequence cs1;
	private CharSequence cs2;
	private int length;

	public CharSequenceBuffer(CharSequence str1, CharSequence str2) {
		cs1 = str1;
		cs2 = str2;
		length = cs1.length() + cs2.length();
	}

	public CharSequence append(CharSequence object) {
		int length1 = this.length;
		int length2 = object.length();
		if (length1 < length2) {
			int tmp = length1;
			length1 = length2;
			length2 = tmp;
		}
		if (length1 / 4000 == (length1 + object.length()) / 4000) {
			return new CharSequenceBuffer(this, object);
		}
		// becomes to long
		StringBuilder sb = new StringBuilder(length1 + length2);
		appendString(cs1, sb);
		appendString(cs2, sb);
		appendString(object, sb);
		return sb.toString();
	}

	public int length() {
		return length;
	}

	/*
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (obj instanceof CharSequence) {
			CharSequence cs = (CharSequence) obj;
			if (cs.length() == length()) {
				for (int i = 0; i < length(); i++) {
					if (cs.charAt(i) != charAt(i))
						return false;
				}
				return true;
			}
		}
		return false;
	}

	/*
	 * @see java.lang.String#hashCode()
	 */
	public int hashCode() {
		int h = 0;
		int off = 0;
		int len = length;
		for (int i = 0; i < len; i++) {
			h = 31 * h + charAt(off++);
		}
		return h;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder(cs1.length() + cs2.length());
		appendString(cs1, sb);
		appendString(cs2, sb);
		return sb.toString();
	}

	private void appendString(final CharSequence cs, StringBuilder sb) {
		if (cs instanceof CharSequenceBuffer) {
			CharSequenceBuffer wsb = (CharSequenceBuffer) cs;
			appendString(wsb.cs1, sb);
			appendString(wsb.cs2, sb);
		} else {
			sb.append(cs.toString());
		}
	}

	public char charAt(int index) {
		if ((index < 0) || (index >= length())) {
			throw new StringIndexOutOfBoundsException(index);
		}
		if (index >= cs1.length()) {
			return cs2.charAt(index - cs1.length());
		}
		return cs1.charAt(index);
	}

	public CharSequence subSequence(int start, int end) {
		if (start < 0) {
			throw new StringIndexOutOfBoundsException(start);
		}
		if (end > length()) {
			throw new StringIndexOutOfBoundsException(end);
		}
		if (start > end) {
			throw new StringIndexOutOfBoundsException(end - start);
		}
		if (start > cs1.length()) {
			return cs2.subSequence(start - cs1.length(), end - cs1.length());
		} else if (end < cs1.length()) {
			return cs1.subSequence(start, end);
		} else {
			return new StringBuilder(end - start).append(
					cs1.subSequence(start, cs1.length())).append(
					cs2.subSequence(0, end - cs1.length()));
		}
	}

	/*
	 * @see org.mozilla.javascript.Wrapper#unwrap()
	 */
	public Object unwrap() {
		return toString();
	}
}
