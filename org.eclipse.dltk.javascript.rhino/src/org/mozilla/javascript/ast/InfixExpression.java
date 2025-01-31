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
 * AST node representing an infix (binary operator) expression. The operator is
 * the node's {@link Token} type.
 */
public class InfixExpression extends AstNode {

	protected AstNode left;
	protected AstNode right;
	protected int operatorPosition = -1;

	public InfixExpression() {
	}

	public InfixExpression(int pos) {
		super(pos);
	}

	public InfixExpression(int pos, int len) {
		super(pos, len);
	}

	public InfixExpression(int pos, int len, AstNode left, AstNode right) {
		super(pos, len);
		setLeft(left);
		setRight(right);
	}

	/**
	 * Constructs a new {@code InfixExpression}. Updates bounds to include left
	 * and right nodes.
	 */
	public InfixExpression(AstNode left, AstNode right) {
		setLeftAndRight(left, right);
	}

	/**
	 * Constructs a new {@code InfixExpression}.
	 * 
	 * @param operatorPos
	 *            the <em>absolute</em> position of the operator
	 */
	public InfixExpression(int operator, AstNode left, AstNode right,
			int operatorPos) {
		setType(operator);
		setOperatorPosition(operatorPos - left.getPosition());
		setLeftAndRight(left, right);
	}

	public void setLeftAndRight(AstNode left, AstNode right) {
		assertNotNull(left);
		assertNotNull(right);
		// compute our bounds while children have absolute positions
		int beg = left.getPosition();
		int end = right.getPosition() + right.getLength();
		setBounds(beg, end);
		// this updates their positions to be parent-relative
		setLeft(left);
		setRight(right);
	}

	/**
	 * Returns operator token &ndash; alias for {@link #getType}
	 */
	public int getOperator() {
		return getType();
	}

	/**
	 * Sets operator token &ndash; like {@link #setType}, but throws an
	 * exception if the operator is invalid.
	 * 
	 * @throws IllegalArgumentException
	 *             if operator is not a valid token code
	 */
	public void setOperator(int operator) {
		if (!Token.isValidToken(operator))
			throw new IllegalArgumentException("Invalid token: " + operator);
		setType(operator);
	}

	/**
	 * Returns the left-hand side of the expression
	 */
	public AstNode getLeft() {
		return left;
	}

	/**
	 * Sets the left-hand side of the expression, and sets its parent to this
	 * node.
	 * 
	 * @param left
	 *            the left-hand side of the expression
	 * @throws IllegalArgumentException
	 *             if left is {@code null}
	 */
	public void setLeft(AstNode left) {
		assertNotNull(left);
		this.left = left;
		left.setParent(this);
	}

	/**
	 * Returns the right-hand side of the expression
	 * 
	 * @return the right-hand side. It's usually an {@link AstNode} node, but
	 *         can also be a {@link FunctionNode} representing Function
	 *         expressions.
	 */
	public AstNode getRight() {
		return right;
	}

	/**
	 * Sets the right-hand side of the expression, and sets its parent to this
	 * node.
	 * 
	 * @throws IllegalArgumentException
	 *             if right is {@code null}
	 */
	public void setRight(AstNode right) {
		assertNotNull(right);
		this.right = right;
		right.setParent(this);
	}

	/**
	 * Returns relative offset of operator token
	 */
	public int getOperatorPosition() {
		return operatorPosition;
	}

	/**
	 * Sets operator token's relative offset
	 * 
	 * @param operatorPosition
	 *            offset in parent of operator token
	 */
	public void setOperatorPosition(int operatorPosition) {
		this.operatorPosition = operatorPosition;
	}

	@Override
	public boolean hasSideEffects() {
		// the null-checks are for malformed expressions in IDE-mode
		switch (getType()) {
		case Token.COMMA:
			return right != null && right.hasSideEffects();
		case Token.AND:
		case Token.OR:
			return left != null && left.hasSideEffects()
					|| (right != null && right.hasSideEffects());
		default:
			return super.hasSideEffects();
		}
	}

	@Override
	public String toSource(int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(makeIndent(depth));
		sb.append(left.toSource());
		sb.append(" ");
		sb.append(operatorToString(getType()));
		sb.append(" ");
		sb.append(right.toSource());
		return sb.toString();
	}

	/**
	 * Visits this node, the left operand, and the right operand.
	 */
	@Override
	public void visit(NodeVisitor v) {
		if (v.visit(this)) {
			left.visit(v);
			right.visit(v);
		}
	}
}
