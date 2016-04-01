package beast.app.beauti;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import beast.app.beauti.BeautiAlignmentProvider;
import beast.app.beauti.BeautiDoc;
import beast.app.beauti.PartitionContext;
import beast.core.BEASTInterface;
import beast.core.Description;
import beast.core.State;
import beast.core.StateNode;
import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.alignment.AlignmentFromTrait;
import beast.evolution.datatype.UserDataType;
import beast.evolution.likelihood.AncestralStateTreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SVSGeneralSubstitutionModel;
import beast.evolution.tree.Tree;
import beast.math.distributions.ParametricDistribution;
import beast.math.distributions.Poisson;
import beast.math.distributions.Prior;



@Description("Beauti Discrete Trait Provider")
public class BeautiDiscreteTraitProvider extends BeautiAlignmentProvider {

	@Override
	protected List<BEASTInterface> getAlignments(BeautiDoc doc) {
		try {
            List<String> trees = new ArrayList<String>();
            doc.scrubAll(true, false);
            State state = (State) doc.pluginmap.get("state");
            for (StateNode node : state.stateNodeInput.get()) {
                if (node instanceof Tree) { // && ((Tree) node).m_initial.get() != null) {
                    trees.add(BeautiDoc.parsePartition(((Tree) node).getID()));
                }
            }
            TraitDialog dlg = new TraitDialog(doc, trees);
            if (dlg.showDialog("Create new trait")) {
            	String tree = dlg.tree;
            	String name = dlg.name;
            	PartitionContext context = new PartitionContext(name, name, name, tree);

            	Alignment alignment = (Alignment) doc.addAlignmentWithSubnet(context, template.get());
            	List<BEASTInterface> list = new ArrayList<BEASTInterface>();
            	list.add(alignment);
            	editAlignment(alignment, doc);
            	return list;
            }
		} catch (Exception e) {
			e.printStackTrace();
		}
        return null;
	}
	
	@Override
	protected int matches(Alignment alignment) {
		for (BEASTInterface output : alignment.getOutputs()) {
			if (output instanceof AncestralStateTreeLikelihood) {
				return 10;
			}
		}
		return 0;
	}
	
	
	@Override
	void editAlignment(Alignment alignment, BeautiDoc doc) {
		TraitInputEditor editor = new TraitInputEditor(doc);
		AncestralStateTreeLikelihood likelihood = null;
		for (BEASTInterface output : alignment.getOutputs()) {
			if (output instanceof AncestralStateTreeLikelihood) {
				likelihood = (AncestralStateTreeLikelihood) output;
				editor.initPanel(likelihood);
		        JOptionPane optionPane = new JOptionPane(editor, JOptionPane.PLAIN_MESSAGE,
		                JOptionPane.CLOSED_OPTION, null, new String[]{"Close"}, "Close");
		        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

		        final JDialog dialog = optionPane.createDialog(Frame.getFrames()[0], "Discrete trait editor");
		    	dialog.setName("DiscreteTraitEditor");
		        // dialog.setResizable(true);
		        dialog.pack();

		        dialog.setVisible(true);
		        try {
			        AlignmentFromTrait traitData = (AlignmentFromTrait) likelihood.dataInput.get();
			        int stateCount = ((UserDataType) traitData.userDataTypeInput.get()).stateCountInput.get();
			        SVSGeneralSubstitutionModel substModel = (SVSGeneralSubstitutionModel) 
			        		((SiteModel.Base) likelihood.siteModelInput.get()).substModelInput.get();
		        	substModel.indicator.get().dimensionInput.setValue(stateCount * (stateCount - 1) / 2, null);
		        	((Parameter.Base<?>) substModel.ratesInput.get()).dimensionInput.setValue(stateCount* (stateCount - 1) / 2, null);
		        	RealParameter freqs = substModel.frequenciesInput.get().frequenciesInput.get();
			        freqs.dimensionInput.setValue(stateCount, freqs);
			        freqs.valuesInput.setValue(1.0/stateCount + "", freqs);
			        // set offset on non-zero rate prior
			        PartitionContext context = new PartitionContext(likelihood);
			        Prior prior = (Prior) doc.pluginmap.get("nonZeroRatePrior.s:" + context.clockModel);
			        ParametricDistribution distr = prior.distInput.get();
			        Poisson poisson = (Poisson) distr;
			        poisson.offsetInput.setValue(stateCount - 1.0, poisson);
		        } catch (Exception e) {
					e.printStackTrace();
				}

				return;
			}
		}
	}

}
