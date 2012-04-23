package beast.evolution.alignment;

import java.util.ArrayList;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.TraitSet;
import beast.util.AddOnManager;

@Description("Treats trait on taxa as single site alignment")
public class AlignmentFromTrait extends Alignment {

		public Input<TraitSet> traitInput = new Input<TraitSet>("traitSet", "trait to be interpreted as single site alignment", Validate.REQUIRED);
			
		TraitSet traitSet;
		
	    public AlignmentFromTrait() {
	        m_pSequences.setRule(Validate.OPTIONAL);
	    }

	    @Override
	    public void initAndValidate() throws Exception {
	    	traitSet = traitInput.get();
	    	if (m_userDataType.get() != null) {
	            m_dataType = m_userDataType.get();
	        } else {
	            if (m_sTypes.indexOf(m_sDataType.get()) < 0) {
	                throw new Exception("data type + '" + m_sDataType.get() + "' cannot be found. " +
	                        "Choose one of " + m_sTypes.toArray(new String[0]));
	            }
	            List<String> sDataTypes = AddOnManager.find(beast.evolution.datatype.DataType.class, IMPLEMENTATION_DIR);
	            for (String sDataType : sDataTypes) {
	                DataType dataType = (DataType) Class.forName(sDataType).newInstance();
	                if (m_sDataType.get().equals(dataType.getDescription())) {
	                    m_dataType = dataType;
	                    break;
	                }
	            }
	        }

	        m_sTaxaNames = traitSet.m_taxa.get().m_taxonList;
	        m_counts = new ArrayList<List<Integer>>();
	        
	        if (traitSet.m_traits.get() == null || traitSet.m_traits.get().matches("^\\s*$")) {
	        	// prevent initialisation when in beauti
	        	m_nPatternIndex = new int[0];
	            return;
	        }
	        
	        for (int i = 0; i < m_sTaxaNames.size(); i++) {
	        	String sValue = traitSet.getStringValue(i);
	        	if (sValue == null) {
	        		throw new Exception("Trait not specified for " + i);
	        	}
	        	List<Integer> iStates = m_dataType.string2state(sValue);
	        	m_counts.add(iStates);
	        }
	        m_nStateCounts = new ArrayList<Integer>();
	        for (String s : m_sTaxaNames) {
	        	m_nStateCounts.add(m_dataType.getStateCount());
	        }

	        calcPatterns();
	    }
}
