package beast.app.beauti;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.border.EmptyBorder;

import beastfx.app.inputeditor.BeautiAlignmentProvider;
import beastfx.app.inputeditor.BeautiDoc;
import beast.base.parser.PartitionContext;
import beast.continuous.SampledMultivariateTraitLikelihood;
import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.inference.State;
import beast.base.inference.StateNode;
import beast.base.evolution.alignment.Alignment;
import beast.base.evolution.tree.Tree;



@Description("Beauti Location Trait Provider")
public class BeautiLocationTraitProvider extends BeautiAlignmentProvider {

	@Override
	public List<BEASTInterface> getAlignments(BeautiDoc doc) {
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
            if (dlg.showDialog("Create new location")) {
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
	public int matches(Alignment alignment) {
		for (BEASTInterface output : alignment.getOutputs()) {
			if (output instanceof beast.continuous.SampledMultivariateTraitLikelihood) {
				return 10;
			}
		}
		return 0;
	}
	
	
	@Override
	public void editAlignment(Alignment alignment, BeautiDoc doc) {
		LocationInputEditor editor = new LocationInputEditor(doc);
		SampledMultivariateTraitLikelihood likelihood = null;
		for (BEASTInterface output : alignment.getOutputs()) {
			if (output instanceof SampledMultivariateTraitLikelihood) {
				likelihood = (SampledMultivariateTraitLikelihood) output;
				editor.initPanel(likelihood);
		        JOptionPane optionPane = new JOptionPane(editor, JOptionPane.PLAIN_MESSAGE,
		                JOptionPane.CLOSED_OPTION, null, new String[]{"Close"}, "Close");
		        optionPane.setBorder(new EmptyBorder(12, 12, 12, 12));

		        final JDialog dialog = optionPane.createDialog(Frame.getFrames()[0], "Location trait editor");
		    	dialog.setName("LocationTraitEditor");
		        // dialog.setResizable(true);
		        dialog.pack();

		        dialog.setVisible(true);
		        editor.convertTableDataToTrait();
		        try {
			        // TODO: any post-processing...
			        // AlignmentFromTraitMap traitData = (AlignmentFromTraitMap) likelihood.m_data.get();
			        
		        } catch (Exception e) {
					e.printStackTrace();
				}

				return;
			}
		}
	}

}
