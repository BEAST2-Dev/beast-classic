package beast.evolution.alignment;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.datatype.ContinuousDataType;
import beast.evolution.tree.TreeTraitMap;

@Description("Creates and alignment of continuous data, such as a geographic location, from a trait set")
public class AlignmentFromTraitMap extends Alignment {
	
	public Input<TreeTraitMap> traitInput = new Input<TreeTraitMap>("traitMap", "trait map to be interpreted as single site alignment");

	TreeTraitMap traitMap;

	public AlignmentFromTraitMap() {
		m_sDataType.setRule(Validate.OPTIONAL);
		m_nStateCount.setRule(Validate.OPTIONAL);
		m_bStripInvariantSites.setRule(Validate.OPTIONAL);
		m_pSequences.setRule(Validate.OPTIONAL);
	}

	@Override
    public void initAndValidate() throws Exception {
    	traitMap = traitInput.get();
    	m_nPatternIndex = new int[0];
        m_counts = new ArrayList<List<Integer>>();
    	if (traitMap == null) { // assume we are in beauti
    		return;
    	}
    	m_dataType = m_userDataType.get();
        if (!(m_dataType instanceof ContinuousDataType)) {
        	throw new Exception("Data type must be a ContinuousDataType, not " + m_dataType.getClass().getName());
        }

        m_sTaxaNames = new ArrayList<String>();
        for (String name : traitMap.treeInput.get().getTaxaNames()) {
        	m_sTaxaNames.add(name);
        }
        
        if (traitMap.value.get() == null || traitMap.value.get().matches("^\\s*$")) {
        	// prevent initialisation when in beauti
        	m_nPatternIndex = new int[1];
            return;
        }
        

        m_nStateCounts = new ArrayList<Integer>();
        for (String s : m_sTaxaNames) {
        	m_nStateCounts.add(m_dataType.getStateCount());
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
