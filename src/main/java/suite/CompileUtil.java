package suite;

import java.util.Arrays;
import java.util.List;

import suite.lp.doer.ProverConfig;
import suite.lp.kb.RuleSet;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;

public class CompileUtil {

	private RuleSet logicalRuleSet;
	private RuleSet funRuleSet;
	private RuleSet eagerFunRuleSet;
	private RuleSet lazyFunRuleSet;

	public synchronized RuleSet logicCompilerRuleSet() {
		if (logicalRuleSet == null)
			logicalRuleSet = createRuleSet(Arrays.asList("auto.sl", "lc.sl"));
		return logicalRuleSet;
	}

	public synchronized RuleSet funCompilerRuleSet() {
		if (funRuleSet == null)
			funRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl"));
		return funRuleSet;
	}

	public RuleSet funCompilerRuleSet(boolean isLazy) {
		return isLazy ? lazyFunCompilerRuleSet() : eagerFunCompilerRuleSet();
	}

	public boolean precompile(String libraryName, ProverConfig pc) {
		System.out.println("Pre-compiling " + libraryName + "... ");

		RuleSet rs = createRuleSet(Arrays.asList("auto.sl", "fc-precompile.sl"));
		Builder builder = new InterpretedProverBuilder(pc);
		boolean result = Suite.proveLogic(builder, rs, "fc-setup-precompile " + libraryName);

		if (result)
			System.out.println("Pre-compilation success\n");
		else
			System.out.println("Pre-compilation failed\n");

		return result;
	}

	private synchronized RuleSet eagerFunCompilerRuleSet() {
		if (eagerFunRuleSet == null)
			eagerFunRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl"));
		return eagerFunRuleSet;
	}

	private synchronized RuleSet lazyFunCompilerRuleSet() {
		if (lazyFunRuleSet == null)
			lazyFunRuleSet = createRuleSet(Arrays.asList("auto.sl", "fc.sl"));
		return lazyFunRuleSet;
	}

	private RuleSet createRuleSet(List<String> toImports) {
		return Suite.createRuleSet(toImports);
	}

}
