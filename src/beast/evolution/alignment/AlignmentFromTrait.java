package beast.evolution.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.alignment.Alignment;
import beast.evolution.datatype.DataType;
import beast.evolution.tree.TraitSet;
import beast.util.BEASTClassLoader;
import beast.util.PackageManager;



@Description("Treats trait on taxa as single site alignment")
public class AlignmentFromTrait extends Alignment {

		public Input<TraitSet> traitInput = new Input<TraitSet>("traitSet", "trait to be interpreted as single site alignment");
			
		TraitSet traitSet;
		
	    public AlignmentFromTrait() {
	        sequenceInput.setRule(Validate.OPTIONAL);
	    }

	    @Override
	    public void initAndValidate() {
	    	traitSet = traitInput.get();
	    	patternIndex = new int[0];
	        counts = new ArrayList<List<Integer>>();
	    	if (traitSet == null) { // assume we are in beauti
	    		return;
	    	}
	    	if (userDataTypeInput.get() != null) {
	            m_dataType = userDataTypeInput.get();
	        } else {
	            if (types.indexOf(dataTypeInput.get()) < 0) {
	                throw new IllegalArgumentException("data type + '" + dataTypeInput.get() + "' cannot be found. " +
	                        "Choose one of " + Arrays.toString(types.toArray(new String[0])));
	            }
	            List<String> sDataTypes = PackageManager.find(beast.evolution.datatype.DataType.class, IMPLEMENTATION_DIR);
	            for (String sDataType : sDataTypes) {
	                DataType dataType = null;
					try {
						dataType = (DataType) BEASTClassLoader.forName(sDataType).newInstance();
					} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
						throw new IllegalArgumentException(e);
					}
	                if (dataTypeInput.get().equals(dataType.getTypeDescription())) {
	                    m_dataType = dataType;
	                    break;
	                }
	            }
	        }

	        taxaNames = traitSet.taxaInput.get().taxaNames;
	        
	        if (traitSet.traitsInput.get() == null || traitSet.traitsInput.get().matches("^\\s*$")) {
	        	// prevent initialisation when in beauti
	        	patternIndex = new int[1];
	            return;
	        }
	        
	        for (int i = 0; i < taxaNames.size(); i++) {
	        	String sValue = traitSet.getStringValue(i);
	        	if (sValue == null) {
	        		throw new IllegalArgumentException("Trait not specified for " + i);
	        	}
	        	List<Integer> iStates = m_dataType.string2state(sValue);
	        	counts.add(iStates);
	        }
	        stateCounts = new ArrayList<Integer>();
	        for (String s : taxaNames) {
	        	stateCounts.add(m_dataType.getStateCount());
	        }

	        calcPatterns();
	    }
}
