package beast.app.beauti;

import beast.app.draw.InputEditor;
import beast.core.Input;
import beast.core.Plugin;
import beast.evolution.likelihood.AncestralStateTreeLikelihood;

public class TraitPriorInputEditor extends InputEditor.Base {

	private static final long serialVersionUID = 1L;

	public TraitPriorInputEditor(BeautiDoc doc) {
		super(doc);
	}

	@Override
	public Class<?> type() {
		return AncestralStateTreeLikelihood.class;
	}
	
	@Override
	public void init(Input<?> input, Plugin plugin, int itemNr,
			ExpandOption bExpandOption, boolean bAddButtons) {
	}

}
