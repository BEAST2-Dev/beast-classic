package beast.evolution.tree;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;

import beast.core.Description;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Plugin;
import beast.core.Input.Validate;
import beast.core.StateNode;
import beast.core.Valuable;

@Description("Logs tree annotated with metadata and/or rates")
public class TreeWithTraitLogger extends Plugin implements Loggable {
	public Input<Tree> m_tree = new Input<Tree>("tree","tree to be logged",Validate.REQUIRED);
	// TODO: make this input a list of valuables
	public Input<Valuable> m_parameter = new Input<Valuable>("metadata","meta data to be logged with the tree nodes");
	public Input<List<TreeTraitProvider>> trait = new Input<List<TreeTraitProvider>>("trait", "trait with branches of the tree", new ArrayList<TreeTraitProvider>());
	

	String m_sMetaDataLabel;
	
	@Override
	public void initAndValidate() throws Exception {
		if (m_parameter.get() == null && trait.get() == null) {
			throw new Exception("At least one of the metadata and branchratemodel inputs must be defined");
		}
		if (m_parameter.get() != null) {
			m_sMetaDataLabel = ((Plugin) m_parameter.get()).getID() + "=";
		}
	}
	
	@Override
	public void init(PrintStream out) throws Exception {
		m_tree.get().init(out);
	}

	@Override
	public void log(int nSample, PrintStream out) {
		// make sure we get the current version of the inputs
        Tree tree = (Tree) m_tree.get().getCurrent();
        Valuable metadata = m_parameter.get();
        if (metadata != null && metadata instanceof StateNode) {
        	metadata = ((StateNode) metadata).getCurrent();
        }
        List<TreeTrait<?>> treeTraits = new ArrayList<TreeTrait<?>>();
        for (TreeTraitProvider provider : trait.get()) {
            TreeTrait<?>[] treeTraits2 = provider.getTreeTraits();
            for (TreeTrait<?> treeTrait : treeTraits2) {
            	treeTraits.add(treeTrait);
            }
        }
        
        // write out the log tree with meta data
        out.print("tree STATE_" + nSample + " = ");
		tree.getRoot().sort();
		out.print(toNewick(tree.getRoot(), metadata, treeTraits));
        //out.print(tree.getRoot().toShortNewick(false));
        out.print(";");
	}

	/** convert tree to Newick string annotated with meta-data provided through treeTraits **/
	String toNewick(Node node, Valuable metadata, List<TreeTrait<?>> treeTraits) {
		StringBuffer buf = new StringBuffer();
		if (node.m_left != null) {
			buf.append("(");
			buf.append(toNewick(node.m_left, metadata, treeTraits));
			if (node.m_right != null) {
				buf.append(',');
				buf.append(toNewick(node.m_right, metadata, treeTraits));
			}
			buf.append(")");
		} else {
			buf.append(node.m_iLabel);
		}
		buf.append("[");
		if (metadata != null) {
			buf.append(m_sMetaDataLabel);
			buf.append(metadata.getArrayValue(node.m_iLabel));
			if (treeTraits != null) {
				buf.append(",");
			}
		}
		if (treeTraits != null) {
			for (TreeTrait<?> trait : treeTraits) {
				buf.append(trait.getTraitName()).append('=');
				buf.append(trait.getTrait(node.m_tree, node));
				buf.append(',');
			}
			// remove last comma
			buf.deleteCharAt(buf.length() - 1);
		}
		buf.append(']');
	    buf.append(":").append(node.getLength());
		return buf.toString();
	}
	
	
	@Override
	public void close(PrintStream out) {
		m_tree.get().close(out);
	}

}
