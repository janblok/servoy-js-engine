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
 *   Steve Yegge
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

package org.mozilla.javascript.ast;

import org.mozilla.javascript.Token;

/**
 * AST node for JavaScript 1.7 {@code yield} expression or statement. Node type
 * is {@link Token#YIELD}.
 * <p>
 * 
 * <pre>
 * <i>Yield</i> :
 *   <b>yield</b> [<i>no LineTerminator here</i>] [non-paren Expression] ;
 * </pre>
 */
public class Yield extends AstNode {

	private AstNode value;

	{
		type = Token.YIELD;
	}

	public Yield() {
	}

	public Yield(int pos) {
		super(pos);
	}

	public Yield(int pos, int len) {
		super(pos, len);
	}

	public Yield(int pos, int len, AstNode value) {
		super(pos, len);
		setValue(value);
	}

	/**
	 * Returns yielded expression, {@code null} if none
	 */
	public AstNode getValue() {
		return value;
	}

	/**
	 * Sets yielded expression, and sets its parent to this node.
	 * 
	 * @param expr
	 *            the value to yield. Can be {@code null}.
	 */
	public void setValue(AstNode expr) {
		this.value = expr;
		if (expr != null)
			expr.setParent(this);
	}

	@Override
	public String toSource(int depth) {
		return value == null ? "yield" : "yield " + value.toSource(0);
	}

	/**
	 * Visits this node, and if present, the yielded value.
	 */
	@Override
	public void visit(NodeVisitor v) {
		if (v.visit(this) && value != null) {
			value.visit(v);
		}
	}
}
