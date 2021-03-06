/**
 * VC.java
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
package edu.clemson.cs.r2jt.rewriteprover;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import edu.clemson.cs.r2jt.rewriteprover.absyn.PExpVisitor;
import edu.clemson.cs.r2jt.typeandpopulate.MTType;

/**
 * <p>Represents an immutable <em>verification condition</em>, which takes the 
 * form of a mathematical implication.</p>
 * 
 * <p>This class is intended to supersede and eventually replace
 * <code>VerificationConditionCongruenceClosureImpl</code>.</p>
 */
public class VC {

    /**
     * <p>Name is a human-readable name for the VC used for debugging purposes.
     * </p>
     */
    private final String myName;

    /**
     * <p>myDerivedFlag is set to true to indicate that this VC is not the
     * original version of the VC with myName--rather it was derived from a
     * VC named myName (or derived from a VC derived from a VC named myName)</p>
     */
    private final boolean myDerivedFlag;

    private final Antecedent myAntecedent;
    private final Consequent myConsequent;

    public VC(String name, Antecedent antecedent, Consequent consequent) {
        this(name, antecedent, consequent, false);
    }

    public VC(String name, Antecedent antecedent, Consequent consequent,
            boolean derived) {

        myName = name;
        myAntecedent = antecedent;
        myConsequent = consequent;
        myDerivedFlag = derived;
    }

    public String getName() {
        String retval = myName;

        if (myDerivedFlag) {
            retval += " (modified)";
        }

        return retval;
    }

    public String getSourceName() {
        return myName;
    }

    public Antecedent getAntecedent() {
        return myAntecedent;
    }

    public Consequent getConsequent() {
        return myConsequent;
    }

    @Override
    public String toString() {

        String retval =
                "========== " + getName() + " ==========\n" + myAntecedent
                        + "  -->\n" + myConsequent;

        return retval;
    }

    public void processStringRepresentation(PExpVisitor visitor, Appendable a) {

        try {
            a.append("========== " + getName() + " ==========\n");
            myAntecedent.processStringRepresentation(visitor, a);
            a.append("  -->\n");
            myConsequent.processStringRepresentation(visitor, a);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toSMTLIB() {
        String rString = "";
        Map<String, MTType> constantTypes = new HashMap<String, MTType>();
        return rString;
    }
}
