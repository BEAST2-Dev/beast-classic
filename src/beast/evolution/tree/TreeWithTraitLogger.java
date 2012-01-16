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
	public Input<List<Valuable>> parameters = new Input<List<Valuable>>("metadata","meta data to be logged with the tree nodes", new ArrayList<Valuable>());
	public Input<List<TreeTraitProvider>> traits = new Input<List<TreeTraitProvider>>("trait", "trait with branches of the tree", new ArrayList<TreeTraitProvider>());
	

	List<String> m_sMetaDataLabel;
	
	@Override
	public void initAndValidate() throws Exception {
		if (parameters.get().size() == 0 && traits.get().size() == 0) {
			throw new Exception("At least one of the metadata and branchratemodel inputs must be defined");
		}
		m_sMetaDataLabel = new ArrayList<String>();
		for (Valuable parameter : parameters.get()) {
			if (!(parameter instanceof Plugin)) {
				throw new Exception("Metadata input must be a Plugin");
			}
			m_sMetaDataLabel.add(((Plugin) parameter).getID() + "=");
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
        List<Valuable> metadatas = new ArrayList<Valuable>();
        for (Valuable metadata : parameters.get()) {
	        if (metadata != null && metadata instanceof StateNode) {
	        	metadata = ((StateNode) metadata).getCurrent();
	        }
	        metadatas.add(metadata);
        }
        List<TreeTrait<?>> treeTraits = new ArrayList<TreeTrait<?>>();
        for (TreeTraitProvider provider : traits.get()) {
            TreeTrait<?>[] treeTraits2 = provider.getTreeTraits();
            for (TreeTrait<?> treeTrait : treeTraits2) {
            	treeTraits.add(treeTrait);
            }
        }
        
        // write out the log tree with meta data
        out.print("tree STATE_" + nSample + " = ");
		tree.getRoot().sort();
		out.print(toNewick(tree.getRoot(), metadatas, treeTraits));
        //out.print(tree.getRoot().toShortNewick(false));
        out.print(";");
	}

	/** convert tree to Newick string annotated with meta-data provided through Valuables and treeTraits **/
	String toNewick(Node node, List<Valuable> metadatas, List<TreeTrait<?>> treeTraits) {
		StringBuffer buf = new StringBuffer();
		if (node.m_left != null) {
			buf.append("(");
			buf.append(toNewick(node.m_left, metadatas, treeTraits));
			if (node.m_right != null) {
				buf.append(',');
				buf.append(toNewick(node.m_right, metadatas, treeTraits));
			}
			buf.append(")");
		} else {
			buf.append(node.m_iLabel);
		}
		buf.append("[");
		if (metadatas.size() > 0) {
			for (int i = 0; i < metadatas.size(); i++) {
				buf.append(m_sMetaDataLabel.get(i));
				buf.append(metadatas.get(i).getArrayValue(node.m_iLabel));
				buf.append(',');
			}
			// remove last comma
			if (treeTraits.size() == 0) {
				buf.deleteCharAt(buf.length() - 1);
			}
		}
		if (treeTraits.size() > 0) {
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
