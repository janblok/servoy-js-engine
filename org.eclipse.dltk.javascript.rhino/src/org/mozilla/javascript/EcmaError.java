/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Rhino code, released
 * May 6, 1999.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1997-1999
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Roger Lawrence
 *
 * Alternatively, the contents of this file may be used under the terms of
 * the GNU General Public License Version 2 or later (the "GPL"), in which
 * case the provisions of the GPL are applicable instead of those above. If
 * you wish to allow use of your version of this file only under the terms of
 * the GPL and not to allow others to use your version of this file under the
 * MPL, indicate your decision by deleting the provisions above and replacing
 * them with the notice and other provisions required by the GPL. If you do
 * not delete the provisions above, a recipient may use your version of this
 * file under either the MPL or the GPL.
 *
 * ***** END LICENSE BLOCK ***** */

// API class

package org.mozilla.javascript;

/**
 * The class of exceptions raised by the engine as described in ECMA edition 3.
 * See section 15.11.6 in particular.
 */
public class EcmaError extends RhinoException {
	static final long serialVersionUID = -6261226256957286699L;

	private String errorName;
	private String errorMessage;

	/**
	 * Create an exception with the specified detail message.
	 * 
	 * Errors internal to the JavaScript engine will simply throw a
	 * RuntimeException.
	 * 
	 * @param sourceName
	 *            the name of the source responsible for the error
	 * @param lineNumber
	 *            the line number of the source
	 * @param columnNumber
	 *            the columnNumber of the source (may be zero if unknown)
	 * @param lineSource
	 *            the source of the line containing the error (may be null if
	 *            unknown)
	 */
	EcmaError(String errorName, String errorMessage, String sourceName,
			int lineNumber, String lineSource, int columnNumber) {
		recordErrorOrigin(sourceName, lineNumber, lineSource, columnNumber);
		this.errorName = errorName;
		this.errorMessage = errorMessage;
	}

	/**
	 * @deprecated EcmaError error instances should not be constructed
	 *             explicitly since they are generated by the engine.
	 */
	public EcmaError(Scriptable nativeError, String sourceName, int lineNumber,
			int columnNumber, String lineSource) {
		this("InternalError", ScriptRuntime.toString(nativeError), sourceName,
				lineNumber, lineSource, columnNumber);
	}

	@Override
	public String details() {
		return errorName + ": " + errorMessage;
	}

	/**
	 * Gets the name of the error.
	 * 
	 * ECMA edition 3 defines the following errors: EvalError, RangeError,
	 * ReferenceError, SyntaxError, TypeError, and URIError. Additional error
	 * names may be added in the future.
	 * 
	 * See ECMA edition 3, 15.11.7.9.
	 * 
	 * @return the name of the error.
	 */
	public String getName() {
		return errorName;
	}

	/**
	 * Gets the message corresponding to the error.
	 * 
	 * See ECMA edition 3, 15.11.7.10.
	 * 
	 * @return an implementation-defined string describing the error.
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * @deprecated Use {@link RhinoException#sourceName()} from the super class.
	 */
	public String getSourceName() {
		return sourceName();
	}

	/**
	 * @deprecated Use {@link RhinoException#lineNumber()} from the super class.
	 */
	public int getLineNumber() {
		return lineNumber();
	}

	/**
	 * @deprecated Use {@link RhinoException#columnNumber()} from the super
	 *             class.
	 */
	public int getColumnNumber() {
		return columnNumber();
	}

	/**
	 * @deprecated Use {@link RhinoException#lineSource()} from the super class.
	 */
	public String getLineSource() {
		return lineSource();
	}

	/**
	 * @deprecated Always returns <b>null</b>.
	 */
	public Scriptable getErrorObject() {
		return null;
	}
}
