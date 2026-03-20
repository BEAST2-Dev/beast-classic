package beastclassic.phylogeography;


import java.util.List;
import java.util.Arrays;
import java.util.Collections;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.StateNode;
import beast.base.inference.StateNodeInitialiser;
import beast.base.core.BEASTObject;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.evolution.substitutionmodel.SubstitutionModel;


/**
 * @author dkuh004
 *         Date: Oct 20, 2011
 *         Time: 12:20:27 PM
 */
@Description("RateIndicatorInitializer ported from BEAST1")
public class RateIndicatorInitializer extends BEASTObject implements StateNodeInitialiser{

    public Input<StateNode> indicator = new Input<StateNode>("rateIndicator",
            "rates to indicate the presence or absence of transition matrix entries", Input.Validate.REQUIRED);
    public Input<SubstitutionModel.Base> model =
            new Input<SubstitutionModel.Base>("substitutionModel", "The substitution model whose transition matrix shall be checked", Input.Validate.REQUIRED);


    public int stateCount;
    private Boolean[] rateIndicator;


    @Override
    public void initAndValidate(){

        stateCount = model.get().getStateCount();
        rateIndicator = new Boolean[indicator.get().getDimension()];
        Arrays.fill(rateIndicator, false);
        initStateNodes();

    }


    @Override
    public void initStateNodes() {

        for (int i =0; i < stateCount*(stateCount-1)/2; i+=stateCount){
            rateIndicator[i] = true;
            if (i>0) rateIndicator[i-1] = true;
        }
        rateIndicator[rateIndicator.length-1] = true;   // bottomleft corner of matrix

        indicator.get().assignFromWithoutID(new BooleanParameter(rateIndicator));

    }

    @Override
    public void getInitialisedStateNodes(List<StateNode> list) {
        initStateNodes();
        list.addAll(Collections.singletonList(indicator.get()));
    }
}
