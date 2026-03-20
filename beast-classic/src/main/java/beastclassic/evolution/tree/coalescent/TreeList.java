package beastclassic.evolution.tree.coalescent;

import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.evolution.tree.Tree;
import beast.base.inference.CalculationNode;

@Description("Collection of trees -- utility for GMRFMultilocusSkyrideLikelihood")
public class TreeList extends CalculationNode {
	public Input<List<Tree>> treesInput = new Input<>("tree", "? ? ? ?", new ArrayList<>());

	@Override
	public void initAndValidate() {
	}

}
