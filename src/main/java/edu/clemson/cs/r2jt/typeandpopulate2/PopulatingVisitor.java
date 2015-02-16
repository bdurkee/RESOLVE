/**
 * PopulatingVisitor.java
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

import edu.clemson.cs.r2jt.absynnew.*;
import edu.clemson.cs.r2jt.absynnew.ImportCollectionAST.ImportType;
import edu.clemson.cs.r2jt.absynnew.decl.MathDefinitionAST;
import edu.clemson.cs.r2jt.absynnew.decl.MathVariableAST;
import edu.clemson.cs.r2jt.absynnew.decl.MathDefinitionAST.DefinitionType;
import edu.clemson.cs.r2jt.absynnew.expr.ExprAST;
import edu.clemson.cs.r2jt.absynnew.expr.MathSymbolAST;
import edu.clemson.cs.r2jt.absynnew.expr.ProgExprAST;
import edu.clemson.cs.r2jt.misc.SrcErrorException;
import edu.clemson.cs.r2jt.typeandpopulate.ModuleIdentifier;
import edu.clemson.cs.r2jt.typeandpopulate2.entry.MathSymbolEntry;
import edu.clemson.cs.r2jt.typeandpopulate2.entry.SymbolTableEntry;
import edu.clemson.cs.r2jt.typeandpopulate2.query.MathFunctionNamedQuery;
import edu.clemson.cs.r2jt.typeandpopulate2.query.MathSymbolQuery;
import edu.clemson.cs.r2jt.typereasoning2.TypeComparison;
import edu.clemson.cs.r2jt.typereasoning2.TypeGraph;
import org.antlr.v4.runtime.Token;

import java.util.*;

public class PopulatingVisitor extends TreeWalkerVisitor {

    private static final boolean PRINT_DEBUG = false;

    private static final TypeComparison<MathSymbolAST, MTFunction> EXACT_DOMAIN_MATCH =
            new ExactDomainMatch();

    private static final Comparator<MTType> EXACT_PARAMETER_MATCH =
            new ExactParameterMatch();

    private final TypeComparison<MathSymbolAST, MTFunction> INEXACT_DOMAIN_MATCH =
            new InexactDomainMatch();

    private final TypeComparison<ExprAST, MTType> INEXACT_PARAMETER_MATCH =
            new InexactParameterMatch();

    private MathSymbolTableBuilder myBuilder;
    private ModuleScopeBuilder myCurModuleScope;

    private int myTypeValueDepth = 0;
    private int myExpressionDepth = 0;

    /**
     * <p>Any quantification-introducing syntactic node (like, e.g., a
     * QuantExp), introduces a level to this stack to reflect the quantification
     * that should be applied to named variables as they are encountered.  Note
     * that this may change as the children of the node are processed--for
     * example, MathVarDecs found in the declaration portion of a QuantExp
     * should have quantification (universal or existential) applied, while
     * those found in the body of the QuantExp should have no quantification
     * (unless there is an embedded QuantExp).  In this case, QuantExp should
     * <em>not</em> remove its layer, but rather change it to
     * MathSymbolTableEntry.None.</p>
     *
     * <p>This stack is never empty, but rather the bottom layer is always
     * MathSymbolTableEntry.None.</p>
     */
    private Deque<SymbolTableEntry.Quantification> myActiveQuantifications =
            new LinkedList<SymbolTableEntry.Quantification>();

    /**
     * <p>While we walk the children of a direct definition, this will be set
     * with a pointer to the definition declaration we are walking, otherwise
     * it will be null.  Note that definitions cannot be nested, so there's
     * no need for a stack.</p>
     */
    private MathDefinitionAST myCurrentDirectDefinition, myCurrentDefinition;

    private Map<String, MTType> myDefinitionSchematicTypes =
            new HashMap<String, MTType>();

    /**
     * <p>This simply enables an error check--as a definition uses named types,
     * we keep track of them, and when an implicit type is introduced, we make
     * sure that it hasn't been "used" yet, thus leading to a confusing scenario
     * where some instances of the name should refer to a type already in scope
     * as the definition is declared and other instance refer to the implicit
     * type parameter.</p>
     */
    private Set<String> myDefinitionNamedTypes = new HashSet<String>();

    /**
     * <p>A mapping from generic types that appear in the module to the math
     * types that bound their possible values.</p>
     */
    private Map<String, MTType> myGenericTypes = new HashMap<String, MTType>();

    private final TypeGraph myTypeGraph;

    public PopulatingVisitor(MathSymbolTableBuilder builder) {
        myActiveQuantifications.push(SymbolTableEntry.Quantification.NONE);
        myTypeGraph = builder.getTypeGraph();
        myBuilder = builder;
    }

    public TypeGraph getTypeGraph() {
        return myTypeGraph;
    }

    //-------------------------------------------------------------------
    //   Visitor methods
    //-------------------------------------------------------------------

    @Override
    public void preModuleAST(ModuleAST e) {
        PopulatingVisitor.emitDebug("----------------------\nModule: "
                + e.getName().getText() + "\n----------------------");
        myCurModuleScope = myBuilder.startModuleScope(e);
    }

    @Override
    public void postImportCollectionAST(ImportCollectionAST e) {
        for (Token importRequest : e
                .getImportsExcluding(ImportType.EXTERNAL)) {
            myCurModuleScope.addImport(new ModuleIdentifier(importRequest));
        }
    }

    @Override
    public void preMathDefinitionAST(MathDefinitionAST e) {
        myBuilder.startScope(e);

        if (!(e.getDefinitionType() == DefinitionType.INDUCTIVE)) {
            myCurrentDirectDefinition = e;
        }
        myCurrentDefinition = e;    // Keep track of the current def.
        myDefinitionSchematicTypes.clear();
        myDefinitionNamedTypes.clear();
    }

    @Override
    public void postMathDefinitionAST(MathDefinitionAST e) {
        myBuilder.endScope();

        MTType declaredType = e.getReturnType().getMathTypeValue();

        if (e.getReturnType() != null) {
            expectType(e.getDefinitionRightHandSide(), declaredType);
        }
        else if (e.getDefinitionType() == DefinitionType.INDUCTIVE) {
           // expectType(node.getBase(), myTypeGraph.BOOLEAN);
           // expectType(node.getHypothesis(), myTypeGraph.BOOLEAN);
        }

        List<MathVariableAST> parameters = e.getParameters();
        if (parameters != null) {
            declaredType = new MTFunction(myTypeGraph, e);
        }

        MTType typeValue = null;
        if (e.getDefinitionRightHandSide() != null) {
            typeValue = e.getDefinitionRightHandSide().getMathTypeValue();
        }

        //Note that, even if typeValue is null at this point, if declaredType
        //returns true from knownToContainOnlyMTypes(), a new type value will
        //still be created by the symbol table
        addBinding(e.getName(), e,
                declaredType, typeValue, myDefinitionSchematicTypes);

        PopulatingVisitor.emitDebug("New definition: "
                + e.getName().getText() + " of type " + declaredType
                + ((typeValue != null) ? " with type value " + typeValue : ""));

        myCurrentDirectDefinition = null;
        myDefinitionSchematicTypes.clear();
        e.setMathType(declaredType);
    }

    @Override
    public void postMathVariableAST(MathVariableAST e) {
        MTType mathTypeValue = e.getSyntaxType().getMathTypeValue();
        String varName = e.getName().getText();
        e.setMathType(mathTypeValue);
        SymbolTableEntry.Quantification q;

        if (withinDefinitionParameters(e) && myTypeValueDepth == 0) {
            q = SymbolTableEntry.Quantification.UNIVERSAL;
        }
        else {
            q = myActiveQuantifications.peek();
        }
    }

    @Override
    public void preExprAST(ExprAST e) {
        myExpressionDepth++;
    }

    @Override
    public void postExprAST(ExprAST e) {

        if (e.getMathType() == null) {
            throw new RuntimeException("expression " + e + " (" + e.getClass()
                    + ") has left population without a math type");
        }
        if (e instanceof ProgExprAST
                && ((ProgExprAST) e).getProgramType() == null) {
            throw new RuntimeException("program expression " + e
                    + " (" + e.getClass() + ") has left population without "
                    + "a program type");
        }
        myExpressionDepth--;
    }

    @Override
    public void postAny(ResolveAST e) {
        if (e instanceof TypeAST) {
            TypeAST eAsTypeNode = (TypeAST) e;
            if (eAsTypeNode.getMathTypeValue() == null) {
                throw new RuntimeException(
                        "TypeAST node " + e + "(" + e.getClass() + ") "
                                + "got through the populator with no "
                                + "math type value");
            }
            if (!(e instanceof MathTypeAST)
                    && eAsTypeNode.getProgramTypeValue() == null) {
                throw new RuntimeException("MathTypeAST node " + e
                        + " (" + e.getClass() + ") got through the "
                        + "populator with no program type value");
            }
        }
    }

    @Override
    public void postMathSymbolAST(MathSymbolAST e) {
        if (!e.isFunction()) {  // constant, variable, or literal.
            MathSymbolEntry intendedEntry =
                    postSymbolExp(e.getQualifier(), e.getName(), e);

            if (myTypeValueDepth > 0 && e.getQualifier() == null) {
                try {
                    intendedEntry.getTypeValue();
                    myDefinitionNamedTypes.add(intendedEntry.getName());
                }
                catch (SymbolNotOfKindTypeException snokte) {
                    //No problem, just don't need to add it
                }
            }
            e.setQuantification(intendedEntry.getQuantification());
        }
        else {  // if MathSymbolAST is a function (Powerset(.), Str(Entry), etc)
            MTFunction foundExpType;
            foundExpType =
                    e.getConservativePreApplicationType(myTypeGraph);

            PopulatingVisitor
                    .emitDebug("expression: " + e + " of type " + foundExpType);

            MathSymbolEntry intendedEntry = getIntendedFunction(e);

            MTFunction expectedType = (MTFunction) intendedEntry.getType();

            //We know we match expectedType--otherwise the above would have
            //thrown an exception.
            e.setMathType(expectedType.getRange());
            e.setQuantification(intendedEntry.getQuantification());

            if (myTypeValueDepth > 0) {
                //I had better identify a type
                MTFunction entryType = (MTFunction) intendedEntry.getType();

                List<MTType> arguments = new LinkedList<MTType>();
                MTType argTypeValue;
                for (ExprAST arg : e.getArguments()) {
                    argTypeValue = arg.getMathTypeValue();

                    if (argTypeValue == null) {
                        notAType(arg);
                    }
                    arguments.add(argTypeValue);
                }

                e.setMathTypeValue(entryType.getApplicationType(
                        intendedEntry.getName(), arguments));
            }
        }
    }

    private MathSymbolEntry postSymbolExp(Token qualifier, Token symbolName,
                                          ExprAST node) {
        MathSymbolEntry intendedEntry =
                getIntendedEntry(qualifier, symbolName, node);
        node.setMathType(intendedEntry.getType());

        setSymbolTypeValue(node, symbolName.getText(), intendedEntry);

        String typeValueDesc = "";

        if (node.getMathTypeValue() != null) {
            typeValueDesc =
                    ", referencing math type " + node.getMathTypeValue() + " ("
                            + node.getMathTypeValue().getClass() + ")";
        }

        PopulatingVisitor.emitDebug("processed symbol " + symbolName
                + " with type " + node.getMathType() + typeValueDesc);

        return intendedEntry;
    }

    /**
     * <p>For a given <code>AbstractFunctionExp</code>, finds the entry in the
     * symbol table to which it refers.  For a complete discussion of the
     * algorithm used, see
     * <a href="http://sourceforge.net/apps/mediawiki/resolve/index.php?title=Package_Search_Algorithm">
     * Package Search Algorithm</a>.</p>
     */
    private MathSymbolEntry getIntendedFunction(MathSymbolAST e) {

        //TODO : All this logic should be encapsulated into a SymbolQuery called
        //       MathFunctionQuery.

        MTFunction eType = e.getConservativePreApplicationType(myTypeGraph);

        Token eOperator = e.getName();
        String eOperatorString = eOperator.getText();

        List<MathSymbolEntry> sameNameFunctions =
                myBuilder.getInnermostActiveScope().query(
                        new MathFunctionNamedQuery(e.getQualifier(),
                                eOperator));

        if (sameNameFunctions.isEmpty()) {
            throw new SrcErrorException("no such function ", e.getStart());
        }

        MathSymbolEntry intendedEntry;
        try {
            intendedEntry = getExactDomainTypeMatch(e, sameNameFunctions);
        }
        catch (NoSolutionException nse) {
            try {
                intendedEntry = getInexactDomainTypeMatch(e, sameNameFunctions);
            }
            catch (NoSolutionException nsee2) {
                boolean foundOne = false;
                String errorMessage =
                        "no function applicable for " + "domain: "
                                + eType.getDomain() + "\n\ncandidates:\n";

                for (SymbolTableEntry entry : sameNameFunctions) {

                    if (entry instanceof MathSymbolEntry
                            && ((MathSymbolEntry) entry).getType() instanceof MTFunction) {
                        errorMessage +=
                                "\t" + entry.getName() + " : "
                                        + ((MathSymbolEntry) entry).getType()
                                        + "\n";

                        foundOne = true;
                    }
                }

                if (!foundOne) {
                    throw new SrcErrorException("no such function ", e
                            .getStart());
                }

                throw new SrcErrorException(errorMessage, e.getStart());
            }
        }

        if (intendedEntry.getDefiningElement() == myCurrentDirectDefinition) {
            throw new SrcErrorException("direct definition cannot "
                    + "contain recursive call ", e.getStart());
        }

        MTFunction intendedEntryType = (MTFunction) intendedEntry.getType();

        PopulatingVisitor.emitDebug("matching " + eOperator + " : " + eType
                + " to " + intendedEntry.getName() + " : " + intendedEntryType);

        return intendedEntry;
    }

    private void setSymbolTypeValue(ExprAST node, String symbolName,
                                    MathSymbolEntry intendedEntry) {

        try {
            if (intendedEntry.getQuantification() ==
                    SymbolTableEntry.Quantification.NONE) {
                node.setMathTypeValue(intendedEntry.getTypeValue());
            }
            else {
                if (intendedEntry.getType().isKnownToContainOnlyMTypes()) {
                    node.setMathTypeValue(new MTNamed(myTypeGraph, symbolName));
                }
            }
        }
        catch (SymbolNotOfKindTypeException snokte) {
            if (myTypeValueDepth > 0) {
                //I had better identify a type
                notAType(intendedEntry, node.getStart());
            }
        }
    }

    private MathSymbolEntry getIntendedEntry(Token qualifier, Token symbolName,
                                             ExprAST node) {

        MathSymbolEntry result;

        try {
            result =
                    myBuilder.getInnermostActiveScope().queryForOne(
                            new MathSymbolQuery(qualifier, symbolName));
        }
        catch (DuplicateSymbolException dse) {
            duplicateSymbol(symbolName);
            throw new RuntimeException(); //This will never fire
        }
        catch (NoSuchSymbolException nsse) {
            noSuchSymbol(qualifier, symbolName);
            throw new RuntimeException(); //This will never fire
        }
        return result;
    }

    private boolean withinDefinitionParameters(MathVariableAST p) {
        if (myCurrentDefinition == null) {
            return false;
        }
        return myCurrentDefinition.getParameters().contains(p);
    }

    @Override
    public void postModuleAST(ModuleAST e) {
        myBuilder.endScope();
        PopulatingVisitor
                .emitDebug("END MATH POPULATOR\n----------------------\n");
    }

    //-------------------------------------------------------------------
    //   Error handling
    //-------------------------------------------------------------------

    public void noSuchModule(Token qualifier) {
        throw new SrcErrorException(
                "module does not exist or is not in scope ", qualifier);
    }

    public void noSuchSymbol(Token qualifier, Token symbol) {
        String message;

        if (qualifier == null) {
            message = "no such symbol " + symbol.getText();
            throw new SrcErrorException(message, symbol);
        }
        else {
            message =
                    "no such symbol in module " + qualifier.getText() + "."
                            + symbol.getText();
            throw new SrcErrorException(message, qualifier);
        }
    }

    public <T extends SymbolTableEntry> void ambiguousSymbol(Token symbol,
                                                             List<T> candidates) {
        String message = "ambiguous symbol;  candidates: ";

        boolean first = true;
        for (SymbolTableEntry candidate : candidates) {
            if (first) {
                first = false;
            }
            else {
                message += ", ";
            }

            message +=
                    candidate.getSourceModuleIdentifier()
                            .fullyQualifiedRepresentation(symbol.getText());
        }
        throw new SrcErrorException(message, symbol);
    }

    public void notAType(SymbolTableEntry entry, Token t) {
        throw new SrcErrorException(entry.getSourceModuleIdentifier()
                .fullyQualifiedRepresentation(entry.getName())
                + " is not known to be a type.", t);
    }

    public void notAType(ExprAST e) {
        throw new SrcErrorException("Not known to be a type.", e.getStart());
    }

    public void expected(ExprAST e, MTType expectedType) {
        throw new SrcErrorException("Expected: " + expectedType
                + "\nFound: " + e.getMathType(), e.getStart());
    }

    public void duplicateSymbol(Token symbol) {
        throw new SrcErrorException("Duplicate symbol: " + symbol.getText(),
                symbol);
    }

    public void expectType(ExprAST e, MTType expectedType) {
        if (!myTypeGraph.isKnownToBeIn(e, expectedType)) {
            expected(e, expectedType);
        }
    }

    private SymbolTableEntry addBinding(Token name,
                                        SymbolTableEntry.Quantification q,
                                        ResolveAST definingElement, MTType type,
                                        MTType typeValue,
                                        Map<String, MTType> schematicTypes) {
        if (type == null) {
            throw new NullPointerException();
        }
        else {
            try {
                return myBuilder.getInnermostActiveScope().addBinding(
                        name.getText(), q, definingElement, type, typeValue,
                        schematicTypes, myGenericTypes);
            }
            catch (DuplicateSymbolException dse) {
                duplicateSymbol(name);
                throw new RuntimeException(); //This will never fire
            }
        }
    }

    private SymbolTableEntry addBinding(Token name,
                                        ResolveAST definingElement, MTType type,
                                        MTType typeValue,
                                        Map<String, MTType> schematicTypes) {
        return addBinding(name, SymbolTableEntry.Quantification.NONE,
                definingElement, type, typeValue, schematicTypes);
    }

    private SymbolTableEntry addBinding(Token name,
                                        SymbolTableEntry.Quantification q,
                                        ResolveAST definingElement, MTType type,
                                        Map<String, MTType> schematicTypes) {
        return addBinding(name, q, definingElement, type, null, schematicTypes);
    }

    private SymbolTableEntry addBinding(Token name,
                                        ResolveAST definingElement, MTType type,
                                        Map<String, MTType> schematicTypes) {
        return addBinding(name, SymbolTableEntry.Quantification.NONE,
                definingElement, type, null, schematicTypes);
    }

    private MathSymbolEntry getExactDomainTypeMatch(MathSymbolAST e,
                                                    List<MathSymbolEntry> candidates)
            throws NoSolutionException {
        return getDomainTypeMatch(e, candidates, EXACT_DOMAIN_MATCH);
    }

    private MathSymbolEntry getInexactDomainTypeMatch(MathSymbolAST e,
                                                      List<MathSymbolEntry> candidates)
            throws NoSolutionException {
        return getDomainTypeMatch(e, candidates, INEXACT_DOMAIN_MATCH);
    }

    private MathSymbolEntry getDomainTypeMatch(MathSymbolAST e,
                                               List<MathSymbolEntry> candidates,
                                               TypeComparison<MathSymbolAST,
                                                       MTFunction> comparison)
            throws NoSolutionException {
        MTFunction eType = e.getConservativePreApplicationType(myTypeGraph);

        MathSymbolEntry match = null;

        MTFunction candidateType;
        for (MathSymbolEntry candidate : candidates) {
            if (candidate.getType() instanceof MTFunction) {

                try {
                    candidate =
                            candidate.deschematize(e.getArguments(), myBuilder
                                            .getInnermostActiveScope(),
                                    myDefinitionSchematicTypes);
                    candidateType = (MTFunction) candidate.getType();
                    emitDebug(candidate.getType() + " deschematizes to "
                            + candidateType);

                    if (comparison.compare(e, eType, candidateType)) {

                        if (match != null) {
                            throw new SrcErrorException("multiple "
                                    + comparison.description() + " domain "
                                    + "matches; for example, "
                                    + match.getName() + " : " + match.getType()
                                    + " and " + candidate.getName() + " : "
                                    + candidate.getType()
                                    + " -- consider explicitly qualifying.", e
                                    .getStart());
                        }

                        match = candidate;
                    }
                }
                catch (NoSolutionException nse) {
                    //couldn't deschematize--try the next one
                    emitDebug(candidate.getType() + " doesn't deschematize "
                            + "against " + e.getArguments());
                }
            }
        }
        if (match == null) {
            throw NoSolutionException.INSTANCE;
        }
        return match;
    }

    public static void emitDebug(String msg) {
        if (PRINT_DEBUG) {
            System.out.println(msg);
        }
    }

    //-------------------------------------------------------------------
    //   Helper classes
    //-------------------------------------------------------------------

    private static class ExactDomainMatch
            implements
                TypeComparison<MathSymbolAST, MTFunction> {

        @Override
        public boolean compare(MathSymbolAST foundValue,
                               MTFunction foundType, MTFunction expectedType) {

            return foundType.parameterTypesMatch(expectedType,
                    EXACT_PARAMETER_MATCH);
        }

        @Override
        public String description() {
            return "exact";
        }
    }

    private class InexactDomainMatch
            implements
            TypeComparison<MathSymbolAST, MTFunction> {

        @Override
        public boolean compare(MathSymbolAST foundValue,
                               MTFunction foundType, MTFunction expectedType) {

            return expectedType.parametersMatch(foundValue.getArguments(),
                    INEXACT_PARAMETER_MATCH);
        }

        @Override
        public String description() {
            return "inexact";
        }
    }

    private static class ExactParameterMatch implements Comparator<MTType> {

        @Override
        public int compare(MTType o1, MTType o2) {
            int result;
            if (o1.equals(o2)) {
                result = 0;
            }
            else {
                result = 1;
            }
            return result;
        }

    }

    private class InexactParameterMatch
            implements
                TypeComparison<ExprAST, MTType> {

        @Override
        public boolean compare(ExprAST foundValue, MTType foundType,
                               MTType expectedType) {

            boolean result =
                    myTypeGraph.isKnownToBeIn(foundValue, expectedType);

            //Todo: I'm not currently considering lambdas.
            /*if (!result && foundValue instanceof LambdaExp
                    && expectedType instanceof MTFunction) {
                LambdaExp foundValueAsLambda = (LambdaExp) foundValue;
                MTFunction expectedTypeAsFunction = (MTFunction) expectedType;

                result =
                        myTypeGraph.isSubtype(foundValueAsLambda.getMathType()
                                .getDomain(), expectedTypeAsFunction
                                .getDomain())
                                && myTypeGraph.isKnownToBeIn(foundValueAsLambda
                                .getBody(), expectedTypeAsFunction
                                .getRange());
            }*/
            return result;
        }

        @Override
        public String description() {
            return "inexact";
        }
    }
}
