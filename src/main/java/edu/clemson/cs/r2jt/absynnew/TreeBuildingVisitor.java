/**
 * TreeBuildingVisitor.java
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
package edu.clemson.cs.r2jt.absynnew;

import edu.clemson.cs.r2jt.absynnew.ImportCollectionAST.ImportCollectionBuilder;
import edu.clemson.cs.r2jt.absynnew.ImportCollectionAST.ImportType;
import edu.clemson.cs.r2jt.absynnew.InitFinalAST.TypeFinalAST;
import edu.clemson.cs.r2jt.absynnew.InitFinalAST.TypeInitAST;
import edu.clemson.cs.r2jt.absynnew.ModuleAST.ConceptAST.ConceptBuilder;
import edu.clemson.cs.r2jt.absynnew.ModuleAST.PrecisAST.PrecisBuilder;
import edu.clemson.cs.r2jt.absynnew.ModuleBlockAST.ModuleBlockBuilder;
import edu.clemson.cs.r2jt.absynnew.decl.*;
import edu.clemson.cs.r2jt.absynnew.decl.MathDefinitionAST.DefinitionBuilder;
import edu.clemson.cs.r2jt.absynnew.decl.OperationImplAST.OperationImplBuilder;
import edu.clemson.cs.r2jt.absynnew.decl.TypeModelAST.TypeDeclBuilder;
import edu.clemson.cs.r2jt.absynnew.expr.*;
import edu.clemson.cs.r2jt.absynnew.expr.MathSymbolAST.MathSymbolExprBuilder;
import edu.clemson.cs.r2jt.absynnew.expr.MathSymbolAST.DisplayStyle;
import edu.clemson.cs.r2jt.misc.SrcErrorException;
import edu.clemson.cs.r2jt.parsing.ResolveBaseListener;
import edu.clemson.cs.r2jt.parsing.ResolveParser;
import edu.clemson.cs.r2jt.misc.Utils.Builder;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeProperty;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Constructs an ast representation of <tt>RESOLVE</tt> sourcecode from the
 * concrete syntax tree produced by <tt>Antlr v4.x</tt>.</p>
 *
 * <p>The ast is built over the course of a pre-post traversal of the concrete
 * syntax tree. Automatically generated <tt>Antlr v4.x</tt> nodes are annotated
 * with their custom abstract-syntax counterparts via an instance of
 * {@link TreeDecorator}, resulting in a tree with a similar, but sparser
 * structure.</p>
 *
 * <p>Note that this class is parameterized by <code>T</code> to indicate that
 * it can handle building of specific subtrees when used in combination with
 * {@link ResolveParserFactory} and the {@link TreeUtil#createASTNodeFrom}
 * method.</p>
 *
 * <p>References to the completed, AST can be acquired through
 * calls to {@link #build()}.</p>
 */
public class TreeBuildingVisitor<T extends ResolveAST>
        extends
            ResolveBaseListener implements Builder<T> {

    private final TreeDecorator myDecorator = new TreeDecorator();

    /**
     * <p>Collects all imports. This builder must be global as it is added to by
     * various contexts encountered throughout the parsetree.</p>
     */
    private ImportCollectionAST.ImportCollectionBuilder myImportBuilder =
            new ImportCollectionBuilder();

    /**
     * <p>All the various signature styles a definition can take on requires
     * us to break from our usual post-oriented tree traversal decoration
     * pattern by declaring this particular builder global. Anyways, this should
     * be initialized in the appropriate top level definition rule, and reset to
     * <code>null</code> after being built and put into an annotation.</p>
     */
    private DefinitionBuilder myDefinitionBuilder = null;

    private final ParseTree myRootTree;

    public TreeBuildingVisitor(ParseTree tree) {
        myRootTree = tree;
    }

    @Override
    public T build() {
        ResolveAST result = get(ResolveAST.class, myRootTree);
        if (result == null) {
            throw new IllegalStateException("ast builder result-tree is null");
        }
        return (T) result;
    }

    @Override
    public void enterConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override
    public void exitUsesList(@NotNull ResolveParser.UsesListContext ctx) {
        myImportBuilder =
                new ImportCollectionBuilder(ctx.getStart(), ctx.getStop())
                        .imports(ImportType.EXPLICIT, ctx.Identifier());
        put(ctx, myImportBuilder.build());
    }

    @Override
    public void exitModule(@NotNull ResolveParser.ModuleContext ctx) {
        put(ctx, get(ModuleAST.class, ctx.getChild(0)));
    }

    @Override
    public void exitConceptModule(
            @NotNull ResolveParser.ConceptModuleContext ctx) {

        ConceptBuilder builder =
                new ConceptBuilder(ctx.getStart(), ctx.getStop(), ctx.name)//
                        .requires(get(ExprAST.class, ctx.requiresClause()))//
                        .block(get(ModuleBlockAST.class, ctx.conceptItems()))//
                        .imports(myImportBuilder.build());

        if (ctx.moduleParameterList() != null) {
            builder.parameters(getAll(ModuleParameterAST.class, ctx
                    .moduleParameterList().moduleParameterDecl()));
        }
        put(ctx, builder.build());
    }

    @Override
    public void exitConceptItems(@NotNull ResolveParser.ConceptItemsContext ctx) {
        ModuleBlockBuilder blockBuilder =
                new ModuleBlockBuilder(ctx.getStart(), ctx.getStop())
                        .generalElements(getAll(ResolveAST.class, ctx
                                .conceptItem()));
        put(ctx, blockBuilder.build());
    }

    @Override
    public void exitConceptItem(@NotNull ResolveParser.ConceptItemContext ctx) {
        put(ctx, get(ResolveAST.class, ctx.getChild(0)));
    }

    @Override
    public void enterPrecisModule(@NotNull ResolveParser.PrecisModuleContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override
    public void exitPrecisModule(@NotNull ResolveParser.PrecisModuleContext ctx) {
        PrecisBuilder builder =
                new PrecisBuilder(ctx.getStart(), ctx.getStop(), ctx.name)//
                        .block(get(ModuleBlockAST.class, ctx.precisItems()))//
                        .imports(myImportBuilder.build());

        put(ctx, builder.build());
    }

    @Override
    public void exitPrecisItem(@NotNull ResolveParser.PrecisItemContext ctx) {
        put(ctx, get(ResolveAST.class, ctx.getChild(0)));
    }

    @Override
    public void exitPrecisItems(@NotNull ResolveParser.PrecisItemsContext ctx) {
        ModuleBlockBuilder blockBuilder =
                new ModuleBlockBuilder(ctx.getStart(), ctx.getStop())
                        .generalElements(getAll(ResolveAST.class, ctx
                                .precisItem()));
        put(ctx, blockBuilder.build());
    }

    @Override
    public void exitOperationDecl(
            @NotNull ResolveParser.OperationDeclContext ctx) {

        OperationSigAST.OperationDeclBuilder builder =
                new OperationSigAST.OperationDeclBuilder(ctx) //
                        .type(get(NamedTypeAST.class, ctx.type())) //
                        .requires(get(ExprAST.class, ctx.requiresClause())) //
                        .ensures(get(ExprAST.class, ctx.ensuresClause())) //
                        .params(
                                getAll(ParameterAST.class, ctx
                                        .operationParameterList()
                                        .parameterDecl()));

        put(ctx, builder.build());
    }

    @Override
    public void enterFacilityOperationDecl(
            @NotNull ResolveParser.FacilityOperationDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override
    public void exitFacilityOperationDecl(
            @NotNull ResolveParser.FacilityOperationDeclContext ctx) {
        List<ParameterAST> formalParams =
                getAll(ParameterAST.class, ctx.operationParameterList()
                        .parameterDecl());

        OperationImplBuilder builder =
                new OperationImplBuilder(ctx.getStart(), ctx.getStop(),
                        ctx.name) //
                        .recursive(ctx.recursive != null) //
                        .parameters(formalParams);

        for (ResolveParser.VariableDeclGroupContext grp : ctx
                .variableDeclGroup()) {
            builder.localVariables(getAll(VariableAST.class, grp.Identifier()));
        }
        put(ctx, builder.build());
    }

    @Override
    public void enterProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        sanityCheckBlockEnds(ctx.name, ctx.closename);
    }

    @Override
    public void exitProcedureDecl(
            @NotNull ResolveParser.ProcedureDeclContext ctx) {
        OperationImplBuilder builder =
                new OperationImplBuilder(ctx.getStart(), ctx.getStop(),
                        ctx.name) //
                        .returnType(get(NamedTypeAST.class, ctx.type())) //
                        .recursive(ctx.recursive != null) //
                        .implementsContract(true) //
                        .parameters(
                                getAll(ParameterAST.class, ctx
                                        .operationParameterList()
                                        .parameterDecl()));

        //Variable lists are a pain in the ass. It'd be easier if we just kept
        //them list-ifed.
        for (ResolveParser.VariableDeclGroupContext grp : ctx
                .variableDeclGroup()) {
            builder.localVariables(getAll(VariableAST.class, grp.Identifier()));
        }
        put(ctx, builder.build());
    }

    @Override
    public void exitVariableDeclGroup(
            @NotNull ResolveParser.VariableDeclGroupContext ctx) {
        NamedTypeAST groupType = get(NamedTypeAST.class, ctx.type());

        for (TerminalNode t : ctx.Identifier()) {
            put(t, new VariableAST(ctx.getStart(), ctx.getStop(),
                    t.getSymbol(), groupType));
        }
    }

    @Override
    public void exitTypeModelDecl(
            @NotNull ResolveParser.TypeModelDeclContext ctx) {
        TypeDeclBuilder builder =
                new TypeDeclBuilder(ctx.getStart(), ctx.getStop(), ctx.name,
                        ctx.exemplar)//
                        .model(get(MathTypeAST.class, ctx.mathTypeExp()))//
                        .init(get(TypeInitAST.class, ctx.specTypeInit()))//
                        .finalize(get(TypeFinalAST.class, ctx.specTypeFinal()))//
                        .constraint(get(ExprAST.class, ctx.constraintClause()));

        put(ctx, builder.build());
    }

    @Override
    public void exitMathTypeTheoremDecl(
            @NotNull ResolveParser.MathTypeTheoremDeclContext ctx) {
        List<MathVariableAST> universals = new ArrayList<MathVariableAST>();

        for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            universals.addAll(getAll(MathVariableAST.class, grp.Identifier()));
        }
        MathTypeTheoremAST theorem =
                new MathTypeTheoremAST(ctx.getStart(), ctx.getStop(), ctx.name,
                        universals, get(ExprAST.class, ctx.mathExp()));
        put(ctx, theorem);
    }

    @Override
    public void exitMathVariableDecl(
            @NotNull ResolveParser.MathVariableDeclContext ctx) {
        put(ctx, new MathVariableAST(ctx.getStart(), ctx.getStop(), ctx
                .Identifier().getSymbol(), get(MathTypeAST.class, ctx
                .mathTypeExp())));
    }

    @Override
    public void exitMathVariableDeclGroup(
            @NotNull ResolveParser.MathVariableDeclGroupContext ctx) {
        MathTypeAST groupType = get(MathTypeAST.class, ctx.mathTypeExp());

        for (TerminalNode t : ctx.Identifier()) {
            put(t, new MathVariableAST(ctx.getStart(), ctx.getStop(), t
                    .getSymbol(), groupType));
        }
    }

    @Override
    public void exitModuleParameterDecl(
            @NotNull ResolveParser.ModuleParameterDeclContext ctx) {
        put(ctx, new ModuleParameterAST(get(DeclAST.class, ctx.getChild(0))));
    }

    @Override
    public void exitTypeParameterDecl(
            @NotNull ResolveParser.TypeParameterDeclContext ctx) {
        put(ctx, new TypeParameterAST(ctx.getStart(), ctx.getStop(), ctx.name));
    }

    @Override
    public void exitParameterDecl(
            @NotNull ResolveParser.ParameterDeclContext ctx) {
        NamedTypeAST type = get(NamedTypeAST.class, ctx.type());
        ParameterAST param =
                new ParameterAST(ctx.getStart(), ctx.getStop(), ctx.name, type);
        put(ctx, param);
    }

    @Override
    public void exitSpecTypeInit(@NotNull ResolveParser.SpecTypeInitContext ctx) {
        TypeInitAST initialization =
                new TypeInitAST(ctx.getStart(), ctx.getStop(), get(
                        ExprAST.class, ctx.requiresClause()), get(
                        ExprAST.class, ctx.ensuresClause()));

        put(ctx, initialization);
    }

    @Override
    public void exitSpecTypeFinal(
            @NotNull ResolveParser.SpecTypeFinalContext ctx) {
        TypeFinalAST finalization =
                new TypeFinalAST(ctx.getStart(), ctx.getStop(), get(
                        ExprAST.class, ctx.requiresClause()), get(
                        ExprAST.class, ctx.ensuresClause()));

        put(ctx, finalization);
    }

    @Override
    public void enterMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        myDefinitionBuilder =
                new MathDefinitionAST.DefinitionBuilder(ctx.getStart(), ctx
                        .getStop());
    }

    @Override
    public void exitMathDefinitionDecl(
            @NotNull ResolveParser.MathDefinitionDeclContext ctx) {
        put(ctx, get(MathDefinitionAST.class, ctx.getChild(0)));
        myDefinitionBuilder = null;
    }

    @Override
    public void exitMathStandardDefinitionDecl(
            @NotNull ResolveParser.MathStandardDefinitionDeclContext ctx) {
        myDefinitionBuilder.body(get(ExprAST.class, ctx.mathAssertionExp()))
                .type(MathDefinitionAST.DefinitionType.STANDARD);

        //Even though we're dealing with a global builder, we still decorate
        //this with what we've built so far in case someone requests it.
        //Also since there is no direct RESOLVE ast equivalent of a definition's
        //signature, for completeness, just stick the finished definition into
        //the signature rules' slots too.
        MathDefinitionAST finished = myDefinitionBuilder.build();
        put(ctx, finished);
        put(ctx.definitionSignature(), finished); //set top level sig rule
        put(ctx.definitionSignature().getChild(0), finished); //set particular
    }

    @Override
    public void exitStandardPrefixSignature(
            @NotNull ResolveParser.StandardPrefixSignatureContext ctx) {
        myDefinitionBuilder//
                .name(ctx.prefixOp().getStart())//
                .returnType(get(MathTypeAST.class, ctx.mathTypeExp()));
        //We've already set the annotation via the top level rule.
        //see exitMathStandardDefinitionDecl.
    }

    @Override
    public void exitStandardOutfixSignature(
            @NotNull ResolveParser.StandardOutfixSignatureContext ctx) {
        myDefinitionBuilder//
                .name(new ResolveToken(ctx.lOp + "..." + ctx.rOp))//
                .returnType(get(MathTypeAST.class, ctx.mathTypeExp()))//
                .parameters(get(MathVariableAST.class, ctx.mathVariableDecl()));
    }

    @Override
    public void exitStandardInfixSignature(
            @NotNull ResolveParser.StandardInfixSignatureContext ctx) {
        myDefinitionBuilder//
                .name(ctx.infixOp().getStart())//
                .returnType(get(MathTypeAST.class, ctx.mathTypeExp()))//
                .parameters(
                        getAll(MathVariableAST.class, ctx.mathVariableDecl()));
        //We've already set the annotation via the top level rule.
        //see exitMathStandardDefinitionDecl.
    }

    @Override
    public void exitDefinitionParameterList(
            @NotNull ResolveParser.DefinitionParameterListContext ctx) {
        for (ResolveParser.MathVariableDeclGroupContext grp : ctx
                .mathVariableDeclGroup()) {
            myDefinitionBuilder.parameters(getAll(MathVariableAST.class, grp
                    .Identifier()));
        }
        put(ctx, myDefinitionBuilder.build());
    }

    @Override
    public void exitType(@NotNull ResolveParser.TypeContext ctx) {
        put(ctx, new NamedTypeAST(ctx));
    }

    @Override
    public void exitProgApplicationExp(
            @NotNull ResolveParser.ProgApplicationExpContext ctx) {
        ProgOperationRefAST call =
                new ProgOperationRefAST(ctx.getStart(), ctx.getStop(), null,
                        ctx.op, getAll(ProgExprAST.class, ctx.progExp()));
        put(ctx, call);
    }

    @Override
    public void exitMathTypeAssertExp(
            @NotNull ResolveParser.MathTypeAssertExpContext ctx) {
        ExprAST lhs = get(ExprAST.class, ctx.mathExp(0));
        ExprAST rhs = get(ExprAST.class, ctx.mathExp(1));

        MathTypeAssertionAST typeAssertion =
                new MathTypeAssertionAST(ctx.getStart(), ctx.getStop(), lhs,
                        new MathTypeAST(rhs));
        put(ctx, typeAssertion);
    }

    @Override
    public void exitProgNestedExp(
            @NotNull ResolveParser.ProgNestedExpContext ctx) {
        put(ctx, get(ProgExprAST.class, ctx.progExp()));
    }

    @Override
    public void exitProgPrimaryExp(
            @NotNull ResolveParser.ProgPrimaryExpContext ctx) {
        put(ctx, get(ProgExprAST.class, ctx.getChild(0)));
    }

    @Override
    public void exitProgPrimary(@NotNull ResolveParser.ProgPrimaryContext ctx) {
        put(ctx, get(ProgExprAST.class, ctx.getChild(0)));
    }

    @Override
    public void exitProgParamExp(@NotNull ResolveParser.ProgParamExpContext ctx) {
        ProgOperationRefAST param =
                new ProgOperationRefAST(ctx.getStart(), ctx.getStop(),
                        ctx.qualifier, ctx.name, getAll(ProgExprAST.class, ctx
                                .progExp()));
        put(ctx, param);
    }

    @Override
    public void exitProgRecordDotExp(
            @NotNull ResolveParser.ProgRecordDotExpContext ctx) {
        throw new UnsupportedOperationException("program record dot "
                + "expressions not yet supported by the compiler.");
    }

    @Override
    public void exitProgNamedExp(@NotNull ResolveParser.ProgNamedExpContext ctx) {
        put(ctx, get(ProgExprAST.class, ctx.progNamedVarExp()));
    }

    @Override
    public void exitProgNamedVarExp(
            @NotNull ResolveParser.ProgNamedVarExpContext ctx) {
        put(ctx, new ProgNameRefAST(ctx.getStart(), ctx.getStop(),
                ctx.qualifier, ctx.name));
    }

    @Override
    public void exitProgIntegerExp(
            @NotNull ResolveParser.ProgIntegerExpContext ctx) {
        put(ctx, new ProgLiteralRefAST.ProgIntegerRefAST(ctx.getStart(), ctx
                .getStop(), Integer.valueOf(ctx.IntegerLiteral().getText())));
    }

    @Override
    public void exitProgStringExp(
            @NotNull ResolveParser.ProgStringExpContext ctx) {
        put(ctx, new ProgLiteralRefAST.ProgStringRefAST(ctx.getStart(), ctx
                .getStop(), String.valueOf(ctx.StringLiteral().getText())));
    }

    @Override
    public void exitMathTypeExp(@NotNull ResolveParser.MathTypeExpContext ctx) {
        put(ctx, new MathTypeAST(get(ExprAST.class, ctx.mathExp())));
    }

    @Override
    public void exitRequiresClause(
            @NotNull ResolveParser.RequiresClauseContext ctx) {
        put(ctx, get(ExprAST.class, ctx.mathAssertionExp()));
    }

    @Override
    public void exitEnsuresClause(
            @NotNull ResolveParser.EnsuresClauseContext ctx) {
        put(ctx, get(ExprAST.class, ctx.mathAssertionExp()));
    }

    @Override
    public void exitMathAssertionExp(
            @NotNull ResolveParser.MathAssertionExpContext ctx) {
        put(ctx, get(ExprAST.class, ctx.mathExp()));
    }

    @Override
    public void exitMathPrimeExp(@NotNull ResolveParser.MathPrimeExpContext ctx) {
        put(ctx, get(ExprAST.class, ctx.mathPrimaryExp()));
    }

    @Override
    public void exitMathNestedExp(
            @NotNull ResolveParser.MathNestedExpContext ctx) {
        put(ctx, get(ExprAST.class, ctx.mathAssertionExp()));
    }

    @Override
    public void exitMathPrimaryExp(
            @NotNull ResolveParser.MathPrimaryExpContext ctx) {
        put(ctx, get(ExprAST.class, ctx.getChild(0)));
    }

    @Override
    public void exitMathBooleanExp(
            @NotNull ResolveParser.MathBooleanExpContext ctx) {
        put(ctx, buildFunctionApplication(ctx.BooleanLiteral(), ctx).literal(
                true).build());
    }

    @Override
    public void exitMathIntegerExp(
            @NotNull ResolveParser.MathIntegerExpContext ctx) {
        put(ctx, buildFunctionApplication(ctx.IntegerLiteral(), ctx).literal(
                true).build());
    }

    @Override
    public void exitMathFunctionApplicationExp(
            @NotNull ResolveParser.MathFunctionApplicationExpContext ctx) {
        put(ctx, get(ExprAST.class, ctx.mathCleanFunctionExp()));
    }

    @Override
    public void exitMathFunctionExp(
            @NotNull ResolveParser.MathFunctionExpContext ctx) {
        put(ctx, buildFunctionApplication(ctx.name, ctx, ctx.mathExp())
                .incoming(ctx.getParent().getStart().getText().equals("#"))
                .build());
    }

    @Override
    public void exitMathVariableExp(
            @NotNull ResolveParser.MathVariableExpContext ctx) {
        put(ctx, buildFunctionApplication(ctx.name, ctx).incoming(
                ctx.getParent().getStart().getText().equals("#")).build());
    }

    @Override
    public void exitMathInfixExp(@NotNull ResolveParser.MathInfixExpContext ctx) {
        put(ctx, buildFunctionApplication(ctx.op, ctx, ctx.mathExp(0),
                ctx.mathExp(1)).style(DisplayStyle.INFIX).build());
    }

    @Override
    public void exitMathUnaryExp(@NotNull ResolveParser.MathUnaryExpContext ctx) {
        put(ctx, buildFunctionApplication(ctx.op, ctx, ctx.mathExp()).build());
    }

    @Override
    public void exitMathOutfixExp(
            @NotNull ResolveParser.MathOutfixExpContext ctx) {
        put(ctx, buildFunctionApplication(ctx.lop, ctx.rop, ctx, ctx.mathExp())
                .style(DisplayStyle.OUTFIX).build());
    }

    private MathSymbolExprBuilder buildFunctionApplication(Token lname,
            Token rname, ParserRuleContext t,
            ResolveParser.MathExpContext... args) {
        return buildFunctionApplication(lname, rname, t, Arrays.asList(args));
    }

    private MathSymbolExprBuilder buildFunctionApplication(Token name,
            ParserRuleContext t, List<ResolveParser.MathExpContext> args) {
        return buildFunctionApplication(name, null, t, args);
    }

    private MathSymbolExprBuilder buildFunctionApplication(Token name,
            ParserRuleContext t, ResolveParser.MathExpContext... args) {
        return buildFunctionApplication(name, t, Arrays.asList(args));
    }

    private MathSymbolExprBuilder buildFunctionApplication(TerminalNode term,
            ParserRuleContext t) {
        return buildFunctionApplication(term.getSymbol(), t,
                new ArrayList<ResolveParser.MathExpContext>());
    }

    private MathSymbolExprBuilder buildFunctionApplication(Token lname,
            Token rname, ParserRuleContext t,
            List<ResolveParser.MathExpContext> args) {
        MathSymbolExprBuilder result =
                new MathSymbolExprBuilder(t, lname, rname).arguments(getAll(
                        ExprAST.class, args));
        return result;
    }

    @Override
    public void exitMathTupleExp(@NotNull ResolveParser.MathTupleExpContext ctx) {
        put(ctx, new MathTupleAST(ctx.getStart(), ctx.getStop(), getAll(
                ExprAST.class, ctx.mathExp())));
    }

    private void put(ParseTree parseTree, ResolveAST ast) {
        myDecorator.putProp(parseTree, ast);
    }

    /**
     * <p>Returns <code>true</code> <strong>iff</strong> the string text within
     * <code>topName</code> equals <code>endName</code></p>.
     *
     * @param topName The name at the top that introduces a block.
     *
     * @param endName The {@link ResolveToken} following the <tt>end</tt>
     *                portion of a named block.
     *
     * @throws SrcErrorException If the provided top and bottom names don't
     *      match.
     */
    private void sanityCheckBlockEnds(Token topName, Token endName) {
        if (!topName.equals(endName)) {
            throw new SrcErrorException("block end name " + endName + " != "
                    + topName, endName);
        }
    }

    /**
     * <p>Shortcut methods to ease interaction with <code>TreeDecorator</code>;
     * for example it's somewhat shorter to say <pre>get(x.class, t)</pre>
     * than <pre>myDecorator.getProp(x.class, t)</pre>.</p>
     *
     * @param type A class within the {@link ResolveAST} hierarchy indicating
     *             expected-type.
     * @param t    A {@link ParseTree} indicating which subtree to draw the
     *             annotation from.
     *
     * @param <T>  A type.
     * @return
     */
    private <T extends ResolveAST> T get(Class<T> type, ParseTree t) {
        return myDecorator.getProp(type, t);
    }

    private <T extends ResolveAST> List<T> getAll(Class<T> type,
            List<? extends ParseTree> t) {
        return myDecorator.collect(type, t);
    }

    private static class TreeDecorator {

        private final ParseTreeProperty<ResolveAST> visitedCtxs =
                new ParseTreeProperty<ResolveAST>();

        public <T extends ResolveAST> List<T> collect(Class<T> type,
                List<? extends ParseTree> parseTrees) {
            List<T> result = new ArrayList<T>();
            for (ParseTree tree : parseTrees) {
                result.add(type.cast(visitedCtxs.get(tree)));
            }
            return result;
        }

        public void putProp(ParseTree parseTree, ResolveAST e) {
            visitedCtxs.put(parseTree, e);
        }

        public <T extends ResolveAST> T getProp(Class<T> type,
                ParseTree parseTree) {
            return type.cast(visitedCtxs.get(parseTree));
        }
    }
}
