/**
 * ParameterAST.java
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
package edu.clemson.cs.r2jt.absynnew.decl;

import edu.clemson.cs.r2jt.absynnew.NamedTypeAST;
import edu.clemson.cs.r2jt.typeandpopulate2.entry.ProgramParameterEntry;
import org.antlr.v4.runtime.Token;

/**
 * <p>A <code>ParameterAST</code> represents a formal parameter to an
 * {@link OperationAST}.  A <em>passing mode</em> on each parameter indicates
 * the affect of a call to said operation.</p>
 */
public class ParameterAST extends DeclAST {

    private final NamedTypeAST myType;
    private final ProgramParameterEntry.ParameterMode myMode;

    public ParameterAST(Token start, Token stop, Token name, NamedTypeAST type,
            ProgramParameterEntry.ParameterMode mode) {
        super(start, stop, name);
        myType = type;
        myMode = mode;
    }

    public ProgramParameterEntry.ParameterMode getMode() {
        return myMode;
    }

    public NamedTypeAST getType() {
        return myType;
    }
}
