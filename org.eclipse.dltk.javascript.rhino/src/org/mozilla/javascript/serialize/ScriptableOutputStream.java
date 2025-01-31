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
 * The Original Code is Rhino serialization code, released
 * Sept. 25, 2001.
 *
 * The Initial Developer of the Original Code is
 * Norris Boyd.
 * Portions created by the Initial Developer are Copyright (C) 2001
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Norris Boyd
 *   Attila Szegedi
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

package org.mozilla.javascript.serialize;

import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.io.*;

import org.mozilla.javascript.*;

/**
 * Class ScriptableOutputStream is an ObjectOutputStream used to serialize
 * JavaScript objects and functions. Note that compiled functions currently
 * cannot be serialized, only interpreted functions. The top-level scope
 * containing the object is not written out, but is instead replaced with
 * another top-level object when the ScriptableInputStream reads in this object.
 * Also, object corresponding to names added to the exclude list are not written
 * out but instead are looked up during deserialization. This approach avoids
 * the creation of duplicate copies of standard objects during deserialization.
 * 
 * @author Norris Boyd
 */

// API class

public class ScriptableOutputStream extends ObjectOutputStream {

	/**
	 * ScriptableOutputStream constructor. Creates a ScriptableOutputStream for
	 * use in serializing JavaScript objects. Calls excludeStandardObjectNames.
	 * 
	 * @param out
	 *            the OutputStream to write to.
	 * @param scope
	 *            the scope containing the object.
	 */
	public ScriptableOutputStream(OutputStream out, Scriptable scope)
			throws IOException {
		super(out);
		this.scope = scope;
		table = new HashMap<Object, String>();
		table.put(scope, "");
		enableReplaceObject(true);
		excludeStandardObjectNames(); // XXX
	}

	public void excludeAllIds(Object[] ids) {
		for (Object id : ids) {
			if (id instanceof String
					&& (scope.get((String) id, scope) instanceof Scriptable)) {
				this.addExcludedName((String) id);
			}
		}
	}

	/**
	 * Adds a qualified name to the list of object to be excluded from
	 * serialization. Names excluded from serialization are looked up in the new
	 * scope and replaced upon deserialization.
	 * 
	 * @param name
	 *            a fully qualified name (of the form "a.b.c", where "a" must be
	 *            a property of the top-level object). The object need not
	 *            exist, in which case the name is ignored.
	 * @throws IllegalArgumentException
	 *             if the object is not a {@link Scriptable}.
	 */
	public void addOptionalExcludedName(String name) {
		Object obj = lookupQualifiedName(scope, name);
		if (obj != null && obj != UniqueTag.NOT_FOUND) {
			if (!(obj instanceof Scriptable)) {
				throw new IllegalArgumentException("Object for excluded name "
						+ name + " is not a Scriptable, it is "
						+ obj.getClass().getName());
			}
			table.put(obj, name);
		}
	}

	/**
	 * Adds a qualified name to the list of objects to be excluded from
	 * serialization. Names excluded from serialization are looked up in the new
	 * scope and replaced upon deserialization.
	 * 
	 * @param name
	 *            a fully qualified name (of the form "a.b.c", where "a" must be
	 *            a property of the top-level object)
	 * @throws IllegalArgumentException
	 *             if the object is not found or is not a {@link Scriptable}.
	 */
	public void addExcludedName(String name) {
		Object obj = lookupQualifiedName(scope, name);
		if (!(obj instanceof Scriptable)) {
			throw new IllegalArgumentException("Object for excluded name "
					+ name + " not found.");
		}
		table.put(obj, name);
	}

	/**
	 * Returns true if the name is excluded from serialization.
	 */
	public boolean hasExcludedName(String name) {
		return table.get(name) != null;
	}

	/**
	 * Removes a name from the list of names to exclude.
	 */
	public void removeExcludedName(String name) {
		table.remove(name);
	}

	/**
	 * Adds the names of the standard objects and their prototypes to the list
	 * of excluded names.
	 */
	public void excludeStandardObjectNames() {
		String[] names = { "Object", "Object.prototype", "Function",
				"Function.prototype",
				"String",
				"String.prototype",
				"Math", // no Math.prototype
				"Array", "Array.prototype", "Error", "Error.prototype",
				"Number", "Number.prototype", "Date", "Date.prototype",
				"RegExp", "RegExp.prototype", "Script", "Script.prototype",
				"Continuation", "Continuation.prototype", };
		for (int i = 0; i < names.length; i++) {
			addExcludedName(names[i]);
		}

		String[] optionalNames = { "XML", "XML.prototype", "XMLList",
				"XMLList.prototype", };
		for (int i = 0; i < optionalNames.length; i++) {
			addOptionalExcludedName(optionalNames[i]);
		}
	}

	static Object lookupQualifiedName(Scriptable scope, String qualifiedName) {
		StringTokenizer st = new StringTokenizer(qualifiedName, ".");
		Object result = scope;
		while (st.hasMoreTokens()) {
			String s = st.nextToken();
			result = ScriptableObject.getProperty((Scriptable) result, s);
			if (result == null || !(result instanceof Scriptable))
				break;
		}
		return result;
	}

	static class PendingLookup implements Serializable {
		static final long serialVersionUID = -2692990309789917727L;

		PendingLookup(String name) {
			this.name = name;
		}

		String getName() {
			return name;
		}

		private String name;
	}

	@Override
	protected Object replaceObject(Object obj) throws IOException {
		if (false)
			throw new IOException(); // suppress warning
		String name = table.get(obj);
		if (name == null)
			return obj;
		return new PendingLookup(name);
	}

	private Scriptable scope;
	private Map<Object, String> table;
}
