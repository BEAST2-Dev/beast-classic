package beast.evolution.likelihood;

import java.io.PrintStream;

import beast.core.Description;
import beast.core.Function;
import beast.core.Input;
import beast.core.Loggable;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;

@Description("Reconstructs sequences at internal nodes and logs them in NEXUS format")
public class AncestralSequenceLogger extends AncestralStateTreeLikelihood implements Function, Loggable {
	final public Input<Boolean> logIndividualInput = new Input<>("logIndividualSites", "if true, tree log gets one entry for every site, "
			+ "if false complete sequence is logged", false);
	
	
	int [] siteStates;
	
	@Override
	public void init(PrintStream out) {
		((Tree)treeInput.get()).init(out);
		
		dataType =  dataInput.get().getDataType();
		siteStates = new int[dataInput.get().getSiteCount()];	
	}
	
	@Override
	public void log(long sample, PrintStream out) {
		calculateLogP();
		redrawAncestralStates();
        out.print("tree STATE_" + sample + " = ");
        TreeInterface tree = treeInput.get();
        tree.getRoot().sort();
        out.print(toNewick(tree.getRoot()));
        out.print(";");
	}

	
    String toNewick(Node node) {
        StringBuffer buf = new StringBuffer();
        if (node.getLeft() != null) {
            buf.append("(");
            buf.append(toNewick(node.getLeft()));
            if (node.getRight() != null) {
                buf.append(',');
                buf.append(toNewick(node.getRight()));
            }
            buf.append(")");
        } else {
            buf.append(node.getNr() + 1);
        }

	    int [] patternstates = getStatesForNode(treeInput.get(), node);
	    for (int i = 0; i < siteStates.length; i++) {
	    	siteStates[i] = patternstates[dataInput.get().getPatternIndex(i)];
	    }
	    String seq = dataType.encodingToString(siteStates);
	    if (logIndividualInput.get()) {
	    	buf.append("[&");
	    	for (int k = 0; k < seq.length(); k++) {
	    		buf.append((k > 0 ? "," : "") + tagInput.get()
	    		+ (k < 10 ? "0":"")
	    		+ (k < 100 ? "0":"")
	    		+ k + "=\"" + seq.charAt(k) + "\"");
	    	}
	    	buf.append("]");
	    	
	    } else {
	    	buf.append("[&" + tagInput.get() + "=\"" + seq + "\"]");
	    }

	    buf.append(':');
        buf.append(node.getLength());
        return buf.toString();
    }
    
	@Override
	public void close(PrintStream out) {
		((Tree)treeInput.get()).close(out);
	}	

}


/**

# Logging ancestral sequences

To log complete ancestral sequences at all internal nodes of a tree, you can use the beast-classic package, version 1.4.1 or better.

Simply add a logger to the XML that looks similar to this: 

``` XML
<logger id="AncestralSequenceLogger" fileName="ancestral.trees" logEvery="10000" mode="tree">
	<log id="atreeLikelihood" 
		spec="beast.evolution.likelihood.AncestralSequenceLogger" 
		data="@data" 
    	siteModel="@SiteModel.s:data"
		branchRateModel="@branchRates.c:data
		tree="@Tree.t:data" 
		tag="seq"
		/>
</logger>
```

You can copy/paste the fragment just before the closing run tag `</run>` and update the attributes as follows:

* make `data`, `siteModel`, `branchRateModel` and `tree` point to the same objects as the treeLikelihood you want to log.
If you set up the XML in BEAUti, changing 'data' to the partition name should do the trick most of the time.
* set `fileName` and `logEvery` to your taste.

and it should run.

In more detail:

* `id` is a unique identifier. It does not matter what you call it, as long as it is unique for the XML file.
* `fileName` is the file used for logging the trees to.
* `logEvery` is the frequency of logging; once every `logEvery` MCMC samples, an entry is added to the log.
* `mode` must be `tree`.
* `spec` must be `beast.evolution.likelihood.AncestralSequenceLogger`.
* `data`, `siteModel`, `branchRateModel` must point to the same as for the treelikelihood that you would like to log ancestral sequences for.
* `tree` can refer to the tree of the treelikelihood, but you can also specify a Newick tree (e.g., an MCC tree for the analysis).
* `tag` is the label used in the tree file for labeling the sequence.

The log file is a tree file that has a single meta-data item, the reconstructed sequence, which could look something like this (for a short alignment of just 10 sites):

```
tree STATE_10000 = (((((((((1[&seq="ATGCGTTTGG"]:0.02979784503786547,4[&seq="ATGCGTTTGG"]:0.02979784503786547)[&seq="ATGCGTTTGG"]:0.0013316887030746238,((2[&seq="ATGCGTTTGG"]:0.005244509868025032,19[&seq="ATGCGTTTGG"]:0.005244509868025032)[&seq="ATGCGTTTGG"]:8.764871247808198E-4,((21[&seq="ATGCGTTTGG"]:...

```


There is an optional flag on `AncestralSequenceLogger`:

* `useMAP` this is an optional flag (set to true of the default false), which determines whether to use maximum aposteriori assignments or sample from the distribution.

## Logging on a fixed tree

Though the BEAST analysis typically estimates the tree, so topologies change through the MCMC analysis, it may be useful to do the reconstruction on a fixed tree. You can specify a tree in Newick format and add the following fragment to the XML, just before the closing beast tag `</beast>`, but after the closing run tag `</run>`:

```XML
<tree spec='beast.util.TreeParser' id='NewickTree' 
  taxa='@data' IsLabelledNewick="true" 
  newick="((your,(tree,goes)),here)"/>
```

If you point `tree` attribute of the AncestralSequenceLogger to NewikcTree, so it reads as `tree="@NewickTree"` assuming you keep the id as above, the logger will use the Newick tree for logging.

**/