/**
 * AssignAST.java
 * ---------------------------------
 * Copyright (c) 2014
 * RESOLVE Software Research Group
 * School of Computing
 * Clemson University
 * All rights reserved.
 * ---------------------------------
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */
package edu.clemson.cs.r2jt.absynnew.stmt;

import edu.clemson.cs.r2jt.absynnew.expr.ProgExprAST;
import org.antlr.v4.runtime.Token;

public class AssignAST extends AbstractInfixStmtAST {

    public AssignAST(Token start, Token stop, ProgExprAST left,
            ProgExprAST right) {
        super(start, stop, left, right);
    }

    @Override
    public String getOperand() {
        return ":=";
    }
}
