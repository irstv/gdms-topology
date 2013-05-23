/**
 * GDMS-Topology is a library dedicated to graph analysis. It is based on the
 * JGraphT library available at <http://www.jgrapht.org/>. It enables computing
 * and processing large graphs using spatial and alphanumeric indexes.
 *
 * This version is developed at French IRSTV institut as part of the EvalPDU
 * project, funded by the French Agence Nationale de la Recherche (ANR) under
 * contract ANR-08-VILL-0005-01 and GEBD project funded by the French Ministery
 * of Ecology and Sustainable Development.
 *
 * GDMS-Topology is distributed under GPL 3 license. It is produced by the
 * "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR
 * 2488.
 *
 * Copyright (C) 2009-2012 IRSTV (FR CNRS 2488)
 *
 * GDMS-Topology is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * GDMS-Topology is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GDMS-Topology. If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please consult: <http://wwwc.orbisgis.org/> or contact
 * directly: info_at_ orbisgis.org
 */
package org.gdms.gdmstopology.centrality;

import org.gdms.data.DataSourceFactory;
import org.gdms.data.schema.Metadata;
import org.gdms.data.values.Value;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import org.gdms.gdmstopology.model.GraphException;
import org.gdms.gdmstopology.model.GraphSchema;
import org.gdms.gdmstopology.parse.GraphFunctionParser;
import org.gdms.gdmstopology.utils.ArrayConcatenator;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.executor.ExecutorFunctionSignature;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableArgument;
import org.orbisgis.progress.ProgressMonitor;

/**
 * SQL function to perform graph analysis on all nodes of a given graph.
 *
 * <p><i>Note</i>: This function use Dijkstra's algorithm to calculate, for each
 * node, all possible shortest paths to all the other nodes (we assume the graph
 * is connected). These calculations are intense and can take a long time to
 * complete.
 *
 * <p> Example usage: <center> {@code EXECUTE ST_GraphAnalysis(
 * input_table,
 * 'weights_column'
 * [,'output_table_prefix']
 * [,orientation]);} </center> or: <center> {@code EXECUTE ST_GraphAnalysis(
 * input_table,
 * 1,
 * [,'output_table_prefix']
 * [,orientation]);} </center>
 *
 * <p> Required parameters: <ul> <li> {@code input_table} - the input table.
 * Specifically, this is the {@code output_table_prefix.edges} table produced by
 * {@link ST_Graph}, with an optional additional column specifying the weight of
 * each edge. <li> {@code 'weights_column'} - either a string specifying the
 * name of the column of the input table that gives the weight of each edge, or
 * 1 if the graph is to be considered unweighted. </ul>
 *
 * <p> Optional parameters: <ul> <li> {@code 'output_table_prefix'} - a string
 * used to prefix the name of the output table. <li> {@code orientation} - an
 * integer specifying the orientation of the graph: <ul> <li> 1 if the graph is
 * directed, <li> 2 if it is directed and we wish to reverse the orientation of
 * the edges. <li> 3 if it is undirected. </ul> The default orientation is
 * directed. </ul>
 *
 * @author Adam Gouge
 */
public class ST_GraphAnalysis extends AbstractTableFunction {

    /**
     * The name of this function.
     */
    private static final String NAME = "ST_GraphAnalysis";
    /**
     * The SQL order of this function.
     */
    private static final String SQL_ORDER = "SELECT * FROM ST_GraphAnalysis("
            + "input_table, "
            + "'weights_column'"
            + "[, orientation]);";
    /**
     * Short description of this function.
     */
    private static final String SHORT_DESCRIPTION =
            "Performs graph analysis on all nodes of a given graph.";
    /**
     * Long description of this function.
     */
    private static final String LONG_DESCRIPTION =
            "<p><i>Note</i>: This function use Dijkstra's algorithm to "
            + "calculate, for each node, all possible shortest paths to all "
            + "the other nodes (we assume the graph is connected). These "
            + "calculations are intense and can take a long time to complete."
            + "<p> Example usage: "
            + "<center> "
            + "<code>EXECUTE ST_GraphAnalysis("
            + "input_table, "
            + "'weights_column'"
            + "[, 'output_table_prefix']"
            + "[, orientation]);</code> </center> "
            + "or: <center> "
            + "<code>EXECUTE ST_GraphAnalysis("
            + "input_table, "
            + "1"
            + "[, 'output_table_prefix']"
            + "[, orientation]);</code> </center>"
            + "<p> Required parameters: "
            + "<ul> "
            + "<li> <code>input_table</code> - the input table. Specifically, "
            + "this is the <code>output_table_prefix.edges</code> table "
            + "produced by <code>ST_Graph</code>, with an optional additional "
            + "column specifying the weight of each edge. "
            + "<li> <code>'weights_column'</code> - either a string specifying "
            + "the name of the column of the input table that gives the weight "
            + "of each edge, or 1 if the graph is to be considered unweighted. "
            + "</ul>"
            + "<p> Optional parameter: "
            + "<ul> "
            + "<li> <code>orientation</code> - an integer specifying the "
            + "orientation of the graph: "
            + "<ul> "
            + "<li> 1 if the graph is directed, "
            + "<li> 2 if it is directed and we wish to reverse the orientation "
            + "of the edges. "
            + "<li> 3 if the graph is undirected, "
            + "</ul> The default orientation is directed. </ul>";
    /**
     * Description of this function.
     */
    private static final String DESCRIPTION =
            SHORT_DESCRIPTION + LONG_DESCRIPTION;
    /**
     * An error message to be displayed when {@link #evaluate(
     * org.gdms.data.DataSourceFactory,
     * org.gdms.driver.DataSet[],
     * org.gdms.data.values.Value[],
     * org.orbisgis.progress.ProgressMonitor) evaluate} fails.
     */
    private static final String EVALUATE_ERROR =
            "Cannot perform graph analysis.";
    /*
     * The number of required arguments;
     */
    private static final int NUMBER_OF_REQUIRED_ARGUMENTS = 1;
    // REQUIRED ARGUMENT
    /**
     * Specifies the weight column (or 1 in the case of an unweighted graph).
     */
    private String weightsColumn;
    // OPTIONAL ARGUMENT
    /**
     * Specifies the orientation of the graph.
     */
    private int orientation = -1;

    /**
     * Evaluates the function to calculate the centrality indices.
     *
     * @param dsf    The {@link DataSourceFactory} used to parse the data set.
     * @param tables The input table. (This {@link DataSet} array will contain
     *               only one element since there is only one input table.)
     * @param values Array containing the other arguments.
     * @param pm     The progress monitor used to track the progress of the
     *               calculation.
     *
     * @return The {@link DataSet} containing the shortest path.
     *
     * @throws FunctionException
     */
    @Override
    public DataSet evaluate(
            DataSourceFactory dsf,
            DataSet[] tables,
            Value[] values,
            ProgressMonitor pm)
            throws FunctionException {
        try {
            // REQUIRED PARAMETERS

            // FIRST PARAMETER: Recover the DataSet. There is only one table.
            final DataSet dataSet = tables[0];

            // SECOND PARAMETER: Either 1 or weight column name.
            weightsColumn = GraphFunctionParser.parseWeight(values[0]);

            // OPTIONAL PARAMETER: [, orientation]
            orientation = (values.length == 2)
                    ? GraphFunctionParser.parseOrientation(values[1])
                    : GraphSchema.DIRECT;
            // Take all user-entered values into account when doing
            // graph analysis.
            return doAnalysisAccordingToUserInput(dsf, dataSet, pm);
        } catch (Exception ex) {
            throw new FunctionException(EVALUATE_ERROR, ex);
        }
    }

    /**
     * Registers closeness centrality according to the SQL arguments provided by
     * the user.
     *
     * @param dsf     The {@link DataSourceFactory} used to parse the data set.
     * @param dataSet The input table.
     * @param pm      The progress monitor used to track the progress of the
     *                calculation.
     *
     * @throws GraphException
     * @throws DriverException
     */
    private DataSet doAnalysisAccordingToUserInput(
            DataSourceFactory dsf,
            DataSet dataSet,
            ProgressMonitor pm)
            throws DriverException, GraphException {
        GraphAnalyzer analyzer = (weightsColumn == null)
                ? // Unweighted graph
                new UnweightedGraphAnalyzer(
                dsf, dataSet, pm, orientation)
                : // Weighted graph
                new WeightedGraphAnalyzer(
                dsf, dataSet, pm, orientation, weightsColumn);
        return analyzer.prepareDataSet();
    }

    /**
     * Returns the name of this function. This name will be used in SQL
     * statements.
     *
     * @return The name of this function.
     */
    @Override
    public String getName() {
        return NAME;
    }

    /**
     * Returns an example query using this function.
     *
     * @return An example query using this function.
     */
    @Override
    public String getSqlOrder() {
        return SQL_ORDER;
    }

    /**
     * Returns a description of this function.
     *
     * @return A description of this function.
     */
    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    /**
     * Returns an array of all possible signatures of this function. Multiple
     * signatures arise from some arguments being optional.
     *
     * @return An array of all possible signatures of this function.
     */
    @Override
    public FunctionSignature[] getFunctionSignatures() {
        return ArrayConcatenator.
                concatenate(unweightedFunctionSignatures(),
                            weightedFunctionSignatures());
    }

    /**
     * Returns all possible function signatures for unweighted graph analyzers.
     *
     * @return Unweighted function signatures.
     */
    private FunctionSignature[] unweightedFunctionSignatures() {
        return possibleFunctionSignatures(ScalarArgument.INT);
    }

    /**
     * Returns all possible function signatures for weighted graph analyzers.
     *
     * @return Weighted function signatures.
     */
    private FunctionSignature[] weightedFunctionSignatures() {
        return possibleFunctionSignatures(ScalarArgument.STRING);
    }

    /**
     * Returns all possible function signatures according to whether the graph
     * analyzer is weighted or unweighted.
     *
     * @return Possible function signatures according to weight.
     */
    private FunctionSignature[] possibleFunctionSignatures(
            ScalarArgument weight) {
        return new FunctionSignature[]{
            // (input_table, weight)
            new ExecutorFunctionSignature(
            TableArgument.GEOMETRY,
            weight),
            // (input_table, weight,
            //     'output_table_prefix')
            new ExecutorFunctionSignature(
            TableArgument.GEOMETRY,
            weight,
            ScalarArgument.STRING),
            // (input_table, weight,
            //     orientation)
            new ExecutorFunctionSignature(
            TableArgument.GEOMETRY,
            weight,
            ScalarArgument.INT),
            // (input_table, weight,
            //     'output_table_prefix', orientation)
            new ExecutorFunctionSignature(
            TableArgument.GEOMETRY,
            weight,
            ScalarArgument.STRING,
            ScalarArgument.INT),
            // (input_table, weight,
            //     orientation, 'output_table_prefix')
            new ExecutorFunctionSignature(
            TableArgument.GEOMETRY,
            weight,
            ScalarArgument.INT,
            ScalarArgument.STRING)};
    }

    @Override
    public Metadata getMetadata(Metadata[] tables) throws DriverException {
        return GraphAnalyzer.MD;
    }
}
