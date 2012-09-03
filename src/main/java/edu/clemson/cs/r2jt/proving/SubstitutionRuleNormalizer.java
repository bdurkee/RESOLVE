package edu.clemson.cs.r2jt.proving;

import java.util.ArrayList;
import java.util.List;

import edu.clemson.cs.r2jt.analysis.MathExpTypeResolver;
import edu.clemson.cs.r2jt.proving.absyn.PExp;

/**
 * <p>A <code>RuleNormalizer</code> that filters out all rules but simple
 * substitutions, i.e. those rules of the form <code>x = y</code> and expresses
 * each as two different <code>VCTransformer</code>s, one substituting 
 * <code>y</code> for <code>x</code> and one substituting <code>x</code> for
 * <code>y</code>.  This substitution occurs either in the consequent of the VC
 * or as an extension to the antecedent.</p>
 * 
 * <p>Because other sorts of rules are generally mistakes as of this writing,
 * this class can be put into <em>noisy mode</em>, in which it will print
 * a warning when it filters out a rule.</p>
 */
public class SubstitutionRuleNormalizer extends AbstractEqualityRuleNormalizer {
	
	public SubstitutionRuleNormalizer(MathExpTypeResolver r, boolean noisy) {
		super(r, noisy);
	}
	
	public SubstitutionRuleNormalizer(MathExpTypeResolver r) {
		this(r, true);
	}
	
	@Override
	protected List<VCTransformer> doNormalize(PExp left, PExp right) {
		List<VCTransformer> retval = new ArrayList<VCTransformer>(2);
		
		//Substitute left expression for right
		retval.add(new MatchReplaceStep(new NewBindReplace(left, right)));
		
		//Substitute right expression for left
		retval.add(new MatchReplaceStep(new NewBindReplace(right, left)));
		
		return retval;
	}

}
