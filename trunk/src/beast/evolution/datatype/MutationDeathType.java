package beast.evolution.datatype;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;

/**
 * Package: MutationDeathType
 * Description:
 * Time: 1:09:40 PM
 */
@Description("Data type for mutation death models")
public class MutationDeathType extends DataType.Base {
	public Input<String> deathCharInput = new Input<String>("deathChar","character representing death state (default 0)","0"); 
	public Input<DataType.Base> dataTypeInput = new Input<DataType.Base>("dataType","base datatype, extended by death char");
	public Input<String> extantCodeInput = new Input<String>("extantCode","character representing live state if no existing datatype is extended",Validate.XOR, dataTypeInput); 
	
    protected static String DESCRIPTION = "MutationDeathType";
    
    public final static int DEATHSTATE = 0;

    @Override
    public void initAndValidate() throws Exception {
    	char deathCode = deathCharInput.get().charAt(0);
    	if (extantCodeInput.get() != null) {
    		char extantCode = extantCodeInput.get().charAt(0);
    		
    		int [][] x = {
    				{deathCode},  // 0
    				{extantCode},  // 1
    				{deathCode,extantCode}, // -
    				{deathCode,extantCode}, // ?
    				};
    		m_nStateCount = 2;
    		m_mapCodeToStateSet = x;
    		m_nCodeLength = 1;
    		m_sCodeMap = "" + deathCode + extantCode + GAP_CHAR + MISSING_CHAR;
    	} else {
    		DataType.Base dataType = dataTypeInput.get();
    		m_nStateCount = dataType.getStateCount() + 1;
    		m_mapCodeToStateSet = new int[dataType.m_mapCodeToStateSet.length + 1][];
    		m_mapCodeToStateSet[0] = new int[] {deathCode};
    		System.arraycopy(dataType.m_mapCodeToStateSet, 0, m_mapCodeToStateSet, 1, dataType.m_mapCodeToStateSet.length);
    		m_nCodeLength = 1;
    		m_sCodeMap = "" + deathCode + dataType.m_sCodeMap;
    	}
    }

}
