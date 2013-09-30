package beast.evolution.tree;

import java.util.ArrayList;
import java.util.List;
import java.io.PrintStream;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.StateNode;
import beast.core.BEASTObject;
import beast.core.Input.Validate;
import beast.core.parameter.Parameter;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;



@Description("Logs tree annotated with metadata and/or rates")
public class TreeWithTraitLogger extends BEASTObject implements Loggable {
	public Input<Tree> m_tree = new Input<Tree>("tree","tree to be logged",Validate.REQUIRED);
//	public Input<List<Valuable>> parameters = new Input<List<Valuable>>("metadata","meta data to be logged with the tree nodes", new ArrayList<Valuable>());
//	public Input<List<TreeTraitProvider>> traits = new Input<List<TreeTraitProvider>>("trait", "trait with branches of the tree", new ArrayList<TreeTraitProvider>());

	public Input<List<BEASTObject>> metadataInput = new Input<List<BEASTObject>>("metadata", "meta data to be logged with the tree nodes." +
			"If it is a trait associated with a node, the metadata will be stored with the nodes." +
			"Otherwise, the metadata will be listed before the Newick tree.", new ArrayList<BEASTObject>());

	public Input<Boolean> combineParametersInput = new Input<Boolean>("combine", "put all parameters in a single field", false);
	
	List<Parameter<?>> parameters;
	List<BranchRateModel> rates;
	List<TreeTraitProvider> traits;
	List<Function> valuables;
	Boolean combineParameters;
	
	@Override
	public void initAndValidate() throws Exception {
		parameters = new ArrayList<Parameter<?>>();
		rates = new ArrayList<BranchRateModel>();
		traits = new ArrayList<TreeTraitProvider>();
		valuables = new ArrayList<Function>();
		combineParameters = combineParametersInput.get();
		
		for (BEASTObject plugin : metadataInput.get()) {
			if (plugin instanceof Parameter) {
				parameters.add((Parameter) plugin);
			} else if (plugin instanceof TreeTraitProvider) {
				traits.add((TreeTraitProvider) plugin);
			} else if (plugin instanceof BranchRateModel) {
				rates.add((BranchRateModel) plugin);
			} else if (plugin instanceof Function){
				valuables.add((Function) plugin);
			} else {
				throw new Exception ("This entry (id=" + plugin.getID() + ") is not metadata that can be logged with a tree");
			}
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
        List<TreeTrait<?>> treeTraits = new ArrayList<TreeTrait<?>>();
        for (TreeTraitProvider provider : traits) {
            TreeTrait<?>[] treeTraits2 = provider.getTreeTraits();
            for (TreeTrait<?> treeTrait : treeTraits2) {
            	treeTraits.add(treeTrait);
            }
        }
        
        // write out the log tree with meta data
        out.print("tree STATE_" + nSample + " = ");
        if (valuables.size() > 0) {
        	out.print("[&");
        	if (combineParameters) {
	    		for (int j = 0; j < valuables.size(); j++) {
	    			out.print(((BEASTObject)valuables.get(j)).getID());
	    		}
	    		out.print("={");
	    		for (int j = 0; j < valuables.size(); j++) {
	    			Function valuable = valuables.get(j);
	        		for (int i = 0; i < valuable.getDimension(); i++) {
	        			out.print(valuable.getArrayValue(i));
	        			if (i < valuable.getDimension() - 1) {
	        				out.print(",");
	        			}
	        		}
	    			if (j < valuables.size() - 1) {
	    				out.print(",");
	    			}
	        	}
	    		out.print("}");
        		 
        	} else {
	    		for (int j = 0; j < valuables.size(); j++) {
	    			Function valuable = valuables.get(j);
	        		for (int i = 0; i < valuable.getDimension(); i++) {
	        			out.print(((BEASTObject) valuable).getID() + "=" +  valuable.getArrayValue(i));
	        			if (i < valuable.getDimension() - 1) {
	        				out.print(",");
	        			}
	        		}
	    			if (j < valuables.size() - 1) {
	    				out.print(",");
	    			}
	        	}
    		}
        	out.print("] ");
        }
		tree.getRoot().sort();
		out.print(toNewick(tree.getRoot(), treeTraits));
        //out.print(tree.getRoot().toShortNewick(false));
        out.print(";");
	}

	/** convert tree to Newick string annotated with meta-data provided through Valuables and treeTraits **/
	String toNewick(Node node, List<TreeTrait<?>> treeTraits) {
		StringBuffer buf = new StringBuffer();
		if (node.getLeft() != null) {
			buf.append("(");
			buf.append(toNewick(node.getLeft(), treeTraits));
			if (node.getRight() != null) {
				buf.append(',');
				buf.append(toNewick(node.getRight(), treeTraits));
			}
			buf.append(")");
		} else {
			buf.append(node.labelNr + 1);
		}
		buf.append("[&");
		
    	if (combineParameters) {
			if (parameters.size() > 0) {
				for (Parameter<?> parameter : parameters) {
					buf.append(parameter.getID());
				}
				buf.append("={");
				int k = 0;
				for (Parameter<?> parameter : parameters) {
					buf.append(parameter.getArrayValue(node.labelNr));
					k++;
					if (k < parameters.size()) {
						buf.append(',');
					}
				}
				buf.append("},");
			}
    	} else {
			if (parameters.size() > 0) {
				for (Parameter<?> parameter : parameters) {
					buf.append(parameter.getID()).append('=');
					buf.append(parameter.getArrayValue(node.labelNr));
					buf.append(',');
				}
			}
    	}
		if (treeTraits.size() > 0) {
			for (TreeTrait<?> trait : treeTraits) {
				buf.append(trait.getTraitName()).append('=');
				buf.append(trait.getTraitString(node.m_tree, node));
				buf.append(',');
			}
		}
		if (rates.size() > 0) {
			for (BranchRateModel rate : rates) {
				buf.append(((BEASTObject)rate).getID()).append('=');
				buf.append(rate.getRateForBranch(node));
				buf.append(',');
			}
		}
		// remove last comma
		if (parameters.size() > 0 || treeTraits.size() > 0 || rates.size() > 0) {
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
