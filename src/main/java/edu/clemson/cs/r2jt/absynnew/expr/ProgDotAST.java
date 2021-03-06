/**
 * ProgDotAST.java
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
package edu.clemson.cs.r2jt.absynnew.expr;

import edu.clemson.cs.r2jt.absynnew.TreeUtil;
import org.antlr.v4.runtime.Token;
import org.stringtemplate.v4.ST;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProgDotAST extends ProgExprAST {

    private final List<ProgNameRefAST> mySegments =
            new ArrayList<ProgNameRefAST>();

    public ProgDotAST(Token start, Token stop, List<ProgNameRefAST> segs) {
        super(start, stop);
        mySegments.addAll(segs);
    }

    public List<ProgNameRefAST> getSegments() {
        return mySegments;
    }

    @Override
    public List<? extends ExprAST> getSubExpressions() {
        return mySegments;
    }

    @Override
    public boolean isLiteral() {
        return false;
    }

    @Override
    public void setSubExpression(int index, ExprAST e) {
        mySegments.set(index, (ProgNameRefAST) e);
    }

    @Override
    protected ExprAST substituteChildren(Map<ExprAST, ExprAST> substitutions) {
        ExprAST retval;

        List<ProgNameRefAST> newSegments = new ArrayList<ProgNameRefAST>();
        for (ExprAST e : mySegments) {
            newSegments.add((ProgNameRefAST) substitute(e, substitutions));
        }
        retval = new ProgDotAST(getStart(), getStop(), newSegments);
        return retval;
    }

    @Override
    public String toString() {
        return TreeUtil.join(mySegments, ".");
    }
}