/**
 * PTType.java
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
package edu.clemson.cs.r2jt.typeandpopulate2.programtypes;

import edu.clemson.cs.r2jt.typeandpopulate2.MTType;
import edu.clemson.cs.r2jt.typeandpopulate2.entry.FacilityEntry;
import edu.clemson.cs.r2jt.typereasoning2.TypeGraph;

import java.util.Map;

public abstract class PTType {

    private final TypeGraph myTypeGraph;

    public PTType(TypeGraph g) {
        myTypeGraph = g;
    }

    public final TypeGraph getTypeGraph() {
        return myTypeGraph;
    }

    public abstract MTType toMath();

    public abstract PTType instantiateGenerics(
            Map<String, PTType> genericInstantiations,
            FacilityEntry instantiatingFacility);

    /**
     * <p>Returns <code>true</code> <strong>iff</strong> an value of this type
     * would be acceptable where one of type <code>t</code> were required.</p>
     *
     * @param t The required type.
     *
     * @return <code>true</code> <strong>iff</strong> an value of this type
     *         would be acceptable where one of type <code>t</code> were
     *         required.
     */
    public boolean acceptableFor(PTType t) {
        return equals(t);
    }
}
