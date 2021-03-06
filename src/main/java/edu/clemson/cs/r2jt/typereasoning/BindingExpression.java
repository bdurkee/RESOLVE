/**
 * BindingExpression.java
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
package edu.clemson.cs.r2jt.typereasoning;

import java.util.HashMap;
import java.util.Map;

import edu.clemson.cs.r2jt.absyn.*;
import edu.clemson.cs.r2jt.collections.Iterator;
import edu.clemson.cs.r2jt.typeandpopulate.BindingException;
import edu.clemson.cs.r2jt.typeandpopulate.MTType;
import edu.clemson.cs.r2jt.typeandpopulate.TypeMismatchException;

public class BindingExpression {

    private final TypeGraph myTypeGraph;
    private Exp myExpression;

    public BindingExpression(TypeGraph g, Exp expression) {
        myExpression = expression;
        myTypeGraph = g;
    }

    public MTType getType() {
        return myExpression.getMathType();
    }

    public MTType getTypeValue() {
        return myExpression.getMathTypeValue();
    }

    public Map<String, Exp> bindTo(Exp expr, Map<String, MTType> typeBindings)
            throws TypeMismatchException,
                BindingException {

        Map<String, Exp> result = new HashMap<String, Exp>();

        bindTo(myExpression, expr, typeBindings, result);

        return result;
    }

    private MTType getTypeUnderBinding(MTType original,
            Map<String, MTType> typeBindings) {

        return original.getCopyWithVariablesSubstituted(typeBindings);
    }

    private void bindTo(Exp expr1, Exp expr2, Map<String, MTType> typeBindings,
            Map<String, Exp> accumulator)
            throws TypeMismatchException,
                BindingException {

        //TODO : Ultimately, in theory, one of the arguments THEMSELVES could 
        //       involve a reference to a named type.  We don't deal with that 
        //       case (only the case where the TYPE of the argument involves a 
        //       named type.)

        //Either type might actually be a named type that's already been mapped,
        //so perform the substitution if necessary
        MTType expr1Type =
                getTypeUnderBinding(expr1.getMathType(), typeBindings);
        MTType expr2Type =
                getTypeUnderBinding(expr2.getMathType(), typeBindings);

        if (!myTypeGraph.isSubtype(expr2Type, expr1Type)) {
            throw TypeMismatchException.INSTANCE;
        }

        if (expr1 instanceof VarExp) {
            VarExp e1AsVarExp = (VarExp) expr1;
            String e1Name = e1AsVarExp.getName().getName();

            if (e1AsVarExp.getQuantification() == VarExp.FORALL) {
                if (accumulator.containsKey(e1Name)) {
                    bindTo(accumulator.get(e1Name), expr2, typeBindings,
                            accumulator);
                }
                else {
                    accumulator.put(e1Name, expr2);
                }
            }
            else {
                if (expr2 instanceof VarExp) {
                    VarExp e2AsVarExp = (VarExp) expr2;

                    if (!e1Name.equals(e2AsVarExp.getName().getName())) {
                        throw new BindingException(expr1, expr2);
                    }
                }
                else {
                    throw new BindingException(expr1, expr2);
                }
            }
        }
        else if (expr1 instanceof AbstractFunctionExp
                && expr2 instanceof AbstractFunctionExp) {

            AbstractFunctionExp funExpr1 = (AbstractFunctionExp) expr1;
            String fun1Name = funExpr1.getOperatorAsString();

            AbstractFunctionExp funExpr2 = (AbstractFunctionExp) expr2;

            if (funExpr1.getQuantification() == VarExp.FORALL) {
                if (accumulator.containsKey(fun1Name)) {
                    fun1Name =
                            ((AbstractFunctionExp) accumulator.get(fun1Name))
                                    .getOperatorAsString();

                    if (!fun1Name.equals(funExpr2.getOperatorAsString())) {
                        throw new BindingException(expr1, expr2);
                    }
                }
                else {
                    accumulator.put(fun1Name, expr2);

                    /*if (myTypeGraph.isSubtype(expr2Type, expr1Type)) {
                    	accumulator.put(fun1Name, expr2);
                    }
                    else {
                    	throw new TypeMismatchException(expr1.getMathType(), 
                    			expr2.getMathType());
                    }*/
                }
            }
            else {
                if (!fun1Name.equals(funExpr2.getOperatorAsString())) {
                    throw new BindingException(expr1, expr2);
                }
            }

            /*if (!myTypeGraph.isSubtype(expr2Type, expr1Type)) {
            	throw new TypeMismatchException(expr1.getMathType(), 
            			expr2.getMathType());
            }*/

            Iterator<Exp> fun1Args = funExpr1.getParameters().iterator();
            Iterator<Exp> fun2Args = funExpr2.getParameters().iterator();

            //There must be the same number of parameters, otherwise the 
            //original typecheck would have failed
            while (fun1Args.hasNext()) {
                bindTo(fun1Args.next(), fun2Args.next(), typeBindings,
                        accumulator);
            }
        }
        else if (expr1 instanceof TupleExp) {

            TupleExp expr1AsTupleExp = (TupleExp) expr1;

            //TODO : Do we need to somehow "descend" (into what is in all 
            //likelihood a DummyExp) and match universal fields to sub 
            //components of expr2?

            //We checked earlier that it's a subtype.  So, if it's universally
            //quantified--we're done here.
            if (!expr1AsTupleExp.isUniversallyQuantified()) {
                Iterator<Exp> tuple1Fields =
                        ((TupleExp) expr1).getFields().iterator();
                Iterator<Exp> tuple2Fields =
                        ((TupleExp) expr2).getFields().iterator();

                //There must be the same number of fields, otherwise the above
                //typecheck would have failed
                while (tuple1Fields.hasNext()) {
                    bindTo(tuple1Fields.next(), tuple2Fields.next(),
                            typeBindings, accumulator);
                }
            }
        }
        else if (expr1 instanceof LambdaExp && expr2 instanceof LambdaExp) {
            LambdaExp expr1AsLambdaExp = (LambdaExp) expr1;
            LambdaExp expr2AsLambdaExp = (LambdaExp) expr2;

            //Note that we don't have to worry about parameters counts or types:
            //the original type check would have kicked us out if those didn't
            //match

            bindTo(expr1AsLambdaExp.getBody(), expr2AsLambdaExp.getBody(),
                    typeBindings, accumulator);

        }
        else {
            throw new BindingException(expr1, expr2);
        }
    }

    @Override
    public String toString() {
        return myExpression.toString();
    }
}
