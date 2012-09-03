/*
 * This software is released under the new BSD 2006 license.
 * 
 * Note the new BSD license is equivalent to the MIT License, except for the
 * no-endorsement final clause.
 * 
 * Copyright (c) 2007, Clemson University
 * 
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer. 
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution. 
 *   * Neither the name of the Clemson University nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * This sofware has been developed by past and present members of the
 * Reusable Sofware Research Group (RSRG) in the School of Computing at
 * Clemson University.  Contributors to the initial version are:
 * 
 *     Steven Atkinson
 *     Greg Kulczycki
 *     Kunal Chopra
 *     John Hunt
 *     Heather Keown
 *     Ben Markle
 *     Kim Roche
 *     Murali Sitaraman
 */

/*
 * ModuleKind.java
 *
 * The Resolve Software Composition Workbench Project
 *
 * Copyright (c) 1999-2005
 * Reusable Software Research Group
 * Department of Computer Science
 * Clemson University
 */

package edu.clemson.cs.r2jt.data;

/** Provides access to type checkable module types. */
public class ModuleKind {

    // ===========================================================
    // Variables 
    // ===========================================================

    private String name;

    // ===========================================================
    // Constructors
    // ===========================================================

    private ModuleKind(String name) {
        this.name = name;
    }

    // ===========================================================
    // Objects
    // ===========================================================

    public final static ModuleKind THEORY = new ModuleKind("Theory");
    public final static ModuleKind PROOFS = new ModuleKind("Proofs");
    public final static ModuleKind CONCEPT = new ModuleKind("Concept");
    public final static ModuleKind ENHANCEMENT = new ModuleKind("Enhancement");
    public final static ModuleKind REALIZATION = new ModuleKind("Realization");
    public final static ModuleKind CONCEPT_BODY
        = new ModuleKind("Concept Body");
    public final static ModuleKind ENHANCEMENT_BODY
        = new ModuleKind("Enhancement Body");
    public final static ModuleKind FACILITY = new ModuleKind("Facility");
    public final static ModuleKind LONG_FACILITY
        = new ModuleKind("Long Facility");
    public final static ModuleKind SHORT_FACILITY
        = new ModuleKind("Short Facility");
    public final static ModuleKind USES_ITEM = new ModuleKind("Uses Item");
    public final static ModuleKind PERFORMANCE = new ModuleKind("Performance Module");
    public final static ModuleKind UNDEFINED = new ModuleKind("Undefined");

    // ===========================================================
    // Public Methods
    // ===========================================================

    public String getExtension() {
        String str = "";
        if (this == ModuleKind.THEORY) {
            str = ".mt";
        } else if (this == ModuleKind.PROOFS) {
        	str = ".mt";
        } else if (this == ModuleKind.CONCEPT) {
            str = ".co";
        } else if (this == ModuleKind.ENHANCEMENT) {
            str = ".en";
        } else if (this == ModuleKind.REALIZATION ||
                   this == ModuleKind.CONCEPT_BODY ||
                   this == ModuleKind.ENHANCEMENT_BODY) {
            str = ".rb";
        } else if (this == ModuleKind.FACILITY ||
                   this == ModuleKind.SHORT_FACILITY ||
                   this == ModuleKind.LONG_FACILITY) {
            str = ".fa";
        } else if (this == ModuleKind.PERFORMANCE) {
         str = ".pp";
        }
     
        return str;
    }

    public String toString() {
        return name;
    }

}
