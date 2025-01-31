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
 * AST node for let statements and expressions. Node type is {@link Token#LET}
 * or {@link Token#LETEXPR}.
 * <p>
 * 
 * <pre>
 * <i>LetStatement</i>:
 *     <b>let</b> ( VariableDeclarationList ) Block
 * <i>LetExpression</i>:
 *     <b>let</b> ( VariableDeclarationList ) Expression
 * </pre>
 * 
 * Note that standalone let-statements with no parens or body block, such as
 * {@code let x=6, y=7;}, are represented as a {@link VariableDeclaration} node
 * of type {@code Token.LET}, wrapped with an {@link ExpressionStatement}.
 * <p>
 */
public class LetNode extends Scope {

	private VariableDeclaration variables;
	private AstNode body;
	private int lp = -1;
	private int rp = -1;

	{
		type = Token.LETEXPR;
	}

	public LetNode() {
	}

	public LetNode(int pos) {
		super(pos);
	}

	public LetNode(int pos, int len) {
		super(pos, len);
	}

	/**
	 * Returns variable list
	 */
	public VariableDeclaration getVariables() {
		return variables;
	}

	/**
	 * Sets variable list. Sets list parent to this node.
	 * 
	 * @throws IllegalArgumentException
	 *             if variables is {@code null}
	 */
	public void setVariables(VariableDeclaration variables) {
		assertNotNull(variables);
		this.variables = variables;
		variables.setParent(this);
	}

	/**
	 * Returns body statement or expression. Body is {@code null} if the form of
	 * the let statement is similar to a VariableDeclaration, with no
	 * curly-brace. (This form is used to define let-bound variables in the
	 * scope of the current block.)
	 * <p>
	 * 
	 * @return the body form
	 */
	public AstNode getBody() {
		return body;
	}

	/**
	 * Sets body statement or expression. Also sets the body parent to this
	 * node.
	 * 
	 * @param body
	 *            the body statement or expression. May be {@code null}.
	 */
	public void setBody(AstNode body) {
		this.body = body;
		if (body != null)
			body.setParent(this);
	}

	/**
	 * Returns left paren position, -1 if missing
	 */
	public int getLp() {
		return lp;
	}

	/**
	 * Sets left paren position
	 */
	public void setLp(int lp) {
		this.lp = lp;
	}

	/**
	 * Returns right paren position, -1 if missing
	 */
	public int getRp() {
		return rp;
	}

	/**
	 * Sets right paren position
	 */
	public void setRp(int rp) {
		this.rp = rp;
	}

	/**
	 * Sets both paren positions
	 */
	public void setParens(int lp, int rp) {
		this.lp = lp;
		this.rp = rp;
	}

	@Override
	public String toSource(int depth) {
		String pad = makeIndent(depth);
		StringBuilder sb = new StringBuilder();
		sb.append(pad);
		sb.append("let (");
		printList(variables.getVariables(), sb);
		sb.append(") ");
		if (body != null) {
			sb.append(body.toSource(depth));
		}
		return sb.toString();
	}

	/**
	 * Visits this node, the variable list, and if present, the body expression
	 * or statement.
	 */
	@Override
	public void visit(NodeVisitor v) {
		if (v.visit(this)) {
			variables.visit(v);
			if (body != null) {
				body.visit(v);
			}
		}
	}
}
