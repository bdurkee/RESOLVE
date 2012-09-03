package edu.clemson.cs.r2jt.treewalk;

import java.util.Iterator;
import java.util.LinkedList;
import edu.clemson.cs.r2jt.absyn.*;

public abstract class TreeWalkerStackVisitor extends TreeWalkerVisitor {
	private LinkedList<ResolveConceptualElement> myVisitStack = new LinkedList<ResolveConceptualElement>();
	
	private void pushParent(ResolveConceptualElement e) {
		myVisitStack.push(e);
	}
	
	private void popParent() {
		myVisitStack.pop();
	}
	
	protected ResolveConceptualElement getParent() {
		return myVisitStack.peek();
	}
	
	protected ResolveConceptualElement getAncestor(int index) {
		return myVisitStack.get(index);
	}
	
	protected int getAncestorSize() {
		return myVisitStack.size();	
	}
	
	protected Iterator<ResolveConceptualElement> getAncestorInterator() {
		return myVisitStack.iterator();
	}
	
	public void preAnyStack(ResolveConceptualElement data) { }
	public void postAnyStack(ResolveConceptualElement data) { }
	
	public final void preAny(ResolveConceptualElement data) {
		preAnyStack(data);
		pushParent(data);
	}
	public final void postAny(ResolveConceptualElement data) {
		popParent();
		postAnyStack(data);
	}
}
