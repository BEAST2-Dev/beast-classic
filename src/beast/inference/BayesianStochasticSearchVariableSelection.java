package beast.inference;

import beast.core.parameter.Parameter;
import beast.core.parameter.RealParameter;
import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.substitutionmodel.GeneralSubstitutionModel;
import beast.evolution.substitutionmodel.Frequencies;
import beast.evolution.tree.Node;
import beast.util.Randomizer;
import cern.colt.bitvector.BitVector;

/**
 * @author dkuh004
 *         Date: Sep 18, 2011
 *         Time: 6:14:07 PM
 *
 * ported from beast1 - author: Marc Suchard
 */
@Description("BayesianStochasticSearchVariableSelection ported from BEAST1")
public interface BayesianStochasticSearchVariableSelection {

    public Parameter getIndicators();

    public boolean validState();

    public class Utils {

        public static boolean connectedAndWellConditioned(double[] probability, SubstitutionModel substModel) {
            if (probability == null) {
                int stateCount = substModel.getStateCount();
                probability = new double[stateCount*stateCount];
            }
            try {
//                public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {

                substModel.getTransitionProbabilities(null, defaultExpectedMutations, 0., 1.,probability);
                return connectedAndWellConditioned(probability);
            } catch (Exception e) { // Any numerical error is bad news
                return false;
            }
        }

        public static boolean connectedAndWellConditioned(double[] probability) {
            for(double prob : probability) {
                if(prob < tolerance || prob >= 1.0) {
                    return false;
                }
            }
            return true;
        }

        // unused methods - maybe implement as stateInitializer to make sure initial state is well connected
        public static void randomize(RealParameter indicators,int dim, boolean reversible) {
            do {
                for (int i = 0; i < indicators.getDimension(); i++)
                    indicators.setValue(i,
                            (Randomizer.nextDouble() < 0.5) ? 0.0 : 1.0);
            } while (!(isStronglyConnected(indicators.getValues(),
                    dim, reversible)));
        }

        public static void setTolerance(double newTolerance) {
            tolerance = newTolerance;
        }

        public static double getTolerance() {
            return tolerance;
        }

        public static void setScalar(double newScalar) {
            defaultExpectedMutations = newScalar;
        }
        public static double getScalar() {
            return defaultExpectedMutations;
        }

        /* Determines if the graph is strongly connected, such that there exists
        * a directed path from any vertex to any other vertex
        *
        */
        public static boolean isStronglyConnected(Double[] indicatorValues, int dim, boolean reversible) {
            BitVector visited = new BitVector(dim);
            boolean connected = true;
            for (int i = 0; i < dim && connected; i++) {
                visited.clear();
                depthFirstSearch(i, visited, indicatorValues, dim, reversible);
                connected = visited.cardinality() == dim;
            }
            return connected;
        }

        private static boolean hasEdge(int i, int j, Double[] indicatorValues,
                                       int dim, boolean reversible) {
            return i != j && indicatorValues[getEntry(i, j, dim, reversible)] == 1;
        }

        private static int getEntry(int i, int j, int dim, boolean reversible) {
            if (reversible) {
                if (j < i) {
                    return getEntry(j,i,dim,reversible);
                }
                int entry = i * dim - i * (i + 1) / 2 + j - 1 -i;
                return entry;
            }

            int entry = i * (dim - 1) + j;
            if (j > i)
                entry--;
            return entry;
        }

        private static void depthFirstSearch(int node, BitVector visited, Double[] indicatorValues,
                                             int dim, boolean reversible) {
            visited.set(node);
            for (int v = 0; v < dim; v++) {
                if (hasEdge(node, v, indicatorValues, dim, reversible) && !visited.get(v))
                    depthFirstSearch(v, visited, indicatorValues, dim, reversible);
            }
        }

        private static double defaultExpectedMutations = 1.0;
        private static double tolerance = 1E-20;
    }
    
}   // interface BSSVS

