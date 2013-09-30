package beast.evolution.alignment;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.ContinuousDataType;
import beast.evolution.tree.TreeTraitMap;



@Description("Creates and alignment of continuous data, such as a geographic location, from a trait set")
public class AlignmentFromTraitMap extends Alignment {
	
	public Input<TreeTraitMap> traitInput = new Input<TreeTraitMap>("traitMap", "trait map to be interpreted as single site alignment");

	TreeTraitMap traitMap;

	public AlignmentFromTraitMap() {
		dataTypeInput.setRule(Validate.OPTIONAL);
		stateCountInput.setRule(Validate.OPTIONAL);
		stripInvariantSitesInput.setRule(Validate.OPTIONAL);
		sequenceInput.setRule(Validate.OPTIONAL);
	}

	@Override
    public void initAndValidate() throws Exception {
    	traitMap = traitInput.get();
    	patternIndex = new int[0];
        counts = new ArrayList<List<Integer>>();
    	if (traitMap == null) { // assume we are in beauti
    		return;
    	}
    	m_dataType = userDataTypeInput.get();
        if (!(m_dataType instanceof ContinuousDataType)) {
        	throw new Exception("Data type must be a ContinuousDataType, not " + m_dataType.getClass().getName());
        }

        taxaNames = new ArrayList<String>();
        for (String name : traitMap.treeInput.get().getTaxonset().asStringList()) {
        	taxaNames.add(name);
        }
        
        if (traitMap.value.get() == null || traitMap.value.get().matches("^\\s*$")) {
        	// prevent initialisation when in beauti
        	patternIndex = new int[1];
            return;
        }
        

        stateCounts = new ArrayList<Integer>();
        for (String s : taxaNames) {
        	stateCounts.add(m_dataType.getStateCount());
        }

    }
	
	public TreeTraitMap getTraitMap() {
		return traitMap;
	}
	
	@Override
	public int getSiteCount() {
		return 1;
	}


}
