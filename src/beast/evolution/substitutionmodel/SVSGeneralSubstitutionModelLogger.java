/*
 * Copyright (C) 2015 Tim Vaughan <tgvaughan@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package beast.evolution.substitutionmodel;

import beast.base.core.BEASTObject;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Loggable;
import beast.base.evolution.datatype.UserDataType;

import java.io.PrintStream;

/**
 * @author Tim Vaughan <tgvaughan@gmail.com>
 */
@Description("SVS General Substitution Model Logger")
public class SVSGeneralSubstitutionModelLogger extends BEASTObject implements Loggable{

    public Input<SVSGeneralSubstitutionModel> modelInput = new Input<>(
            "model",
            "BSSVS general substitution model.",
            Input.Validate.REQUIRED);

    public Input<UserDataType> dataTypeInput = new Input<>(
            "dataType",
            "User data type for the location data.  Used to generate " +
                    "more readable logs.",
            Input.Validate.REQUIRED);

    public Input<Boolean> useLocationNamesInput = new Input<>(
            "useLocationNames",
            "When true, use full names of locations in log rather than " +
                    "rate matrix indices.",
            true);

    protected SVSGeneralSubstitutionModel model;

    public SVSGeneralSubstitutionModelLogger() { }

    @Override
    public void initAndValidate() {
        model = modelInput.get();
    }

    /**
     * If available, retrieve string representation of location, otherwise
     * simply return the index as a string.
     *
     * @param i index of location
     * @return string representation
     */
    private String getLocationString(int i) {
        if (useLocationNamesInput.get())
            return dataTypeInput.get().getCode(i);
        else
            return String.valueOf(i);
    }

    @Override
    public void init(PrintStream out) {
        String mainID = (getID() == null || getID().matches("\\s*"))
                ? "geoSubstModel"
                : getID();
        String relRatePrefix = mainID + ".relGeoRate_";

        UserDataType dataType = dataTypeInput.get();

        for (int i=0; i<model.getStateCount(); i++) {
            String iStr = getLocationString(i);

            for (int j=model.isSymmetricInput.get() ? i+1 : 0 ; j<model.getStateCount(); j++) {
                if (j==i)
                    continue;

                String jStr = getLocationString(j);

                out.print(relRatePrefix + iStr + "_" + jStr + "\t");
            }
        }
    }

    @Override
    public void log(long nSample, PrintStream out) {
        int count = 0;
        for (int i=0; i<model.getStateCount(); i++) {
            for (int j=model.isSymmetricInput.get() ? i+1 : 0; j<model.getStateCount(); j++) {
                if (j==i)
                    continue;

                out.print(model.ratesInput.get().getArrayValue(count)
                        *model.indicator.get().getArrayValue(count) + "\t");

                count += 1;
            }
        }
    }

    @Override
    public void close(PrintStream out) { }
}
