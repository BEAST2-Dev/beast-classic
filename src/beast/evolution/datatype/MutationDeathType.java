package beast.evolution.datatype;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;

/**
 * Package: MutationDeathType
 * Description:
 * Time: 1:09:40 PM
 */
@Description("Data type for mutation death models including Multi-State Stochastic Dollo")
public class MutationDeathType extends DataType.Base {
	public Input<String> deathCharInput = new Input<String>("deathChar","character representing death state (default 0)","0"); 
	public Input<DataType.Base> dataTypeInput = new Input<DataType.Base>("dataType","base datatype, extended by death char");
	public Input<String> extantCodeInput = new Input<String>("extantCode","character representing live state if no existing datatype is extended",Validate.XOR, dataTypeInput); 
	
    protected static String DESCRIPTION = "MutationDeathType";
    
    public static int DEATHSTATE = 0;

    @Override
    public void initAndValidate() throws Exception {
    	char deathCode = deathCharInput.get().charAt(0);
    	if (extantCodeInput.get() != null) {
    		char extantCode = extantCodeInput.get().charAt(0);
    		
    		int [][] x = {
    				{0},  // 0
    				{1},  // 1
    				{0,1}, // -
    				{0,1}, // ?
    				};
    		m_nStateCount = 2;
    		m_mapCodeToStateSet = x;
    		m_nCodeLength = 1;
    		m_sCodeMap = "" + extantCode + deathCode + GAP_CHAR + MISSING_CHAR;
    		DEATHSTATE = 1;
    	} else {
    		DataType.Base dataType = dataTypeInput.get();
    		m_nStateCount = dataType.getStateCount() + 1;
    		m_mapCodeToStateSet = new int[dataType.m_mapCodeToStateSet.length + 1][];
    		System.arraycopy(dataType.m_mapCodeToStateSet, 0, m_mapCodeToStateSet, 0, dataType.m_mapCodeToStateSet.length);
    		m_mapCodeToStateSet[m_nStateCount - 1] = new int[] {deathCode};
    		m_nCodeLength = 1;
    		m_sCodeMap = "" + dataType.m_sCodeMap + deathCode;
    		DEATHSTATE = m_nStateCount - 1;
    	}
    }

}
