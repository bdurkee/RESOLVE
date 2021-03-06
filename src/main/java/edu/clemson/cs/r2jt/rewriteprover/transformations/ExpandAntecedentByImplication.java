/**
 * ExpandAntecedentByImplication.java
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
package edu.clemson.cs.r2jt.rewriteprover.transformations;

import edu.clemson.cs.r2jt.rewriteprover.iterators.LazyMappingIterator;
import edu.clemson.cs.r2jt.rewriteprover.absyn.PExp;
import edu.clemson.cs.r2jt.rewriteprover.absyn.PSymbol;
import edu.clemson.cs.r2jt.rewriteprover.Utilities;
import edu.clemson.cs.r2jt.rewriteprover.applications.Application;
import edu.clemson.cs.r2jt.rewriteprover.justifications.TheoremApplication;
import edu.clemson.cs.r2jt.rewriteprover.model.AtLeastOneLocalTheoremBinder;
import edu.clemson.cs.r2jt.rewriteprover.model.Conjunct;
import edu.clemson.cs.r2jt.rewriteprover.model.LocalTheorem;
import edu.clemson.cs.r2jt.rewriteprover.model.PerVCProverModel;
import edu.clemson.cs.r2jt.rewriteprover.model.PerVCProverModel.BindResult;
import edu.clemson.cs.r2jt.rewriteprover.model.PerVCProverModel.Binder;
import edu.clemson.cs.r2jt.rewriteprover.model.Site;
import edu.clemson.cs.r2jt.rewriteprover.model.Theorem;
import edu.clemson.cs.r2jt.rewriteprover.proofsteps.IntroduceLocalTheoremStep;
import edu.clemson.cs.r2jt.misc.Utils.Mapping;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <p>A transformation that applies "A and B and C implies D" by first seeing
 * if all antecedents (A, B, and C) can be directly matched against givens or
 * global theorems, and if so, adding a new given matching the form of D, with 
 * variables appropriately replaced based on the matching of the antecedents.
 * </p>
 * 
 * <p><strong>Random Quirk:</strong> Because the intention of this class is to
 * extend available assumptions based on local contextual data, at least one
 * variable binding when matching the theorem antecedent against known facts
 * must come from the prover state's antecedent.  That is, the prover state will
 * not be extended with applications of this conditional theorem entirely to 
 * global facts--those "extensions" should themselves be listed as global 
 * theorems.</p>
 * 
 * <p><strong>Example:</strong>  Given the theorem 
 * <code>|S| &gt; 0 implies S /= Empty_String</code>, consider the following
 * VC:</p>
 * 
 * <pre>
 * |T| > 0
 * --->
 * |S o T| > |S|
 * </pre>
 * 
 * One (the only) application of this transformation would be:
 * 
 * <pre>
 * |T| > 0 and
 * T /= Empty_String
 * --->
 * |S o T| > |S|
 * </pre>
 */
public class ExpandAntecedentByImplication implements Transformation {

    private final BindResultToApplication BIND_RESULT_TO_APPLICATION =
            new BindResultToApplication();

    private final List<PExp> myAntecedents;
    private final int myAntecedentsSize;
    private final PExp myConsequent;
    private final Theorem myTheorem;

    public ExpandAntecedentByImplication(Theorem t, List<PExp> tAntecedents,
            PExp tConsequent) {
        myTheorem = t;
        myAntecedents = tAntecedents;
        myAntecedentsSize = myAntecedents.size();
        myConsequent = tConsequent;
    }

    @Override
    public String toString() {
        return Utilities.conjunctListToString(myAntecedents) + " implies "
                + myConsequent;
    }

    @Override
    public Iterator<Application> getApplications(PerVCProverModel m) {
        Set<Binder> binders = new HashSet<Binder>();
        for (PExp a : myAntecedents) {
            binders.add(new AtLeastOneLocalTheoremBinder(a, myAntecedentsSize));
        }

        return new LazyMappingIterator(m.bind(binders),
                BIND_RESULT_TO_APPLICATION);
    }

    @Override
    public boolean couldAffectAntecedent() {
        return true;
    }

    @Override
    public boolean couldAffectConsequent() {
        return false;
    }

    @Override
    public int functionApplicationCountDelta() {
        int antecedentFunctionCount = 0;
        for (PExp a : myAntecedents) {
            antecedentFunctionCount += a.getFunctionApplications().size();
        }

        int consequentFunctionCount =
                myConsequent.getFunctionApplications().size();

        return consequentFunctionCount - antecedentFunctionCount;
    }

    @Override
    public boolean introducesQuantifiedVariables() {
        Set<PSymbol> antecedentQuantifiedVariables = new HashSet<PSymbol>();
        for (PExp a : myAntecedents) {
            antecedentQuantifiedVariables.addAll(a.getQuantifiedVariables());
        }

        Set<PSymbol> consequentQuantifiedVariables =
                new HashSet<PSymbol>(myConsequent.getQuantifiedVariables());

        consequentQuantifiedVariables.removeAll(antecedentQuantifiedVariables);

        return !consequentQuantifiedVariables.isEmpty();
    }

    @Override
    public Set<String> getPatternSymbolNames() {
        Set<String> antecedentSymbolNames = new HashSet<String>();

        for (PExp a : myAntecedents) {
            antecedentSymbolNames.addAll(a.getSymbolNames());
        }

        return antecedentSymbolNames;
    }

    @Override
    public Set<String> getReplacementSymbolNames() {
        return myConsequent.getSymbolNames();
    }

    @Override
    public Equivalence getEquivalence() {
        return Equivalence.WEAKER;
    }

    @Override
    public String getKey() {
        return myTheorem.getAssertion() + " " + this.getClass().getName();
    }

    public class BindResultToApplication
            implements
                Mapping<BindResult, Application> {

        @Override
        public Application map(BindResult input) {
            return new ExpandAntecedentByImplicationApplication(
                    input.freeVariableBindings, input.bindSites.values());
        }
    }

    private class ExpandAntecedentByImplicationApplication
            implements
                Application {

        private Map<PExp, PExp> myBindings;
        private Collection<Site> myBindSites;
        private Set<Theorem> myBindSiteTheorems = new HashSet<Theorem>();
        private Set<Conjunct> myAddedTheorems = new HashSet<Conjunct>();
        private Set<Site> myAddedSites = new HashSet<Site>();

        public ExpandAntecedentByImplicationApplication(
                Map<PExp, PExp> bindings, Collection<Site> bindSites) {
            myBindings = bindings;
            myBindSites = bindSites;

            for (Site s : bindSites) {
                myBindSiteTheorems.add((Theorem) s.conjunct);
            }
        }

        @Override
        public String description() {
            return "Add " + myConsequent.substitute(myBindings);
        }

        @Override
        public void apply(PerVCProverModel m) {
            List<PExp> newAntecedents =
                    myConsequent.substitute(myBindings).splitIntoConjuncts();

            for (PExp a : newAntecedents) {
                LocalTheorem t =
                        m.addLocalTheorem(a, new TheoremApplication(
                                ExpandAntecedentByImplication.this), false);

                myAddedSites.add(new Site(m, t, a));

                m.addProofStep(new IntroduceLocalTheoremStep(t,
                        myBindSiteTheorems, ExpandAntecedentByImplication.this,
                        this, myBindSites));

                myAddedTheorems.add(t);
            }
        }

        @Override
        public Set<Site> involvedSubExpressions() {
            Set<Site> result = new HashSet<Site>();

            for (Site s : myBindSites) {
                if (s.conjunct instanceof LocalTheorem) {
                    result.add(s);
                }
            }

            return result;
        }

        @Override
        public Set<Conjunct> getPrerequisiteConjuncts() {
            Set<Conjunct> result = new HashSet<Conjunct>(myBindSiteTheorems);

            result.add(myTheorem);

            return result;
        }

        @Override
        public Set<Conjunct> getAffectedConjuncts() {
            return myAddedTheorems;
        }

        @Override
        public Set<Site> getAffectedSites() {
            return myAddedSites;
        }
    }
}