/**
 * NoSuchScopeException.java
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
package edu.clemson.cs.r2jt.typeandpopulate2;

import edu.clemson.cs.r2jt.absynnew.ResolveAST;

@SuppressWarnings("serial")
public class NoSuchScopeException extends RuntimeException {

    public final ResolveAST requestedScope;

    public NoSuchScopeException(ResolveAST e) {
        requestedScope = e;
    }
}
