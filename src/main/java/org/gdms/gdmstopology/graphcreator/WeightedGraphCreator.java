/**
 * GDMS-Topology is a library dedicated to graph analysis. It is based on the
 * JGraphT library available at <http://www.jgrapht.org/>. It enables computing
 * and processing large graphs using spatial and alphanumeric indexes.
 *
 * This version is developed at French IRSTV Institute as part of the EvalPDU
 * project, funded by the French Agence Nationale de la Recherche (ANR) under
 * contract ANR-08-VILL-0005-01 and GEBD project funded by the French Ministry
 * of Ecology and Sustainable Development.
 *
 * GDMS-Topology is distributed under GPL 3 license. It is produced by the
 * "Atelier SIG" team of the IRSTV Institute <http://www.irstv.fr/> CNRS FR
 * 2488.
 *
 * Copyright (C) 2009-2013 IRSTV (FR CNRS 2488)
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
 * directly: info_at_orbisgis.org
 */
package org.gdms.gdmstopology.graphcreator;

import org.gdms.data.indexes.IndexException;
import org.gdms.data.schema.Metadata;
import org.gdms.data.values.Value;
import org.gdms.driver.DataSet;
import org.gdms.driver.DriverException;
import static org.gdms.gdmstopology.graphcreator.GraphCreator.METADATA_ERROR;
import org.javanetworkanalyzer.data.VId;
import static org.javanetworkanalyzer.graphcreators.GraphCreator.UNDIRECTED;
import org.javanetworkanalyzer.model.DirectedWeightedPseudoG;
import org.javanetworkanalyzer.model.Edge;
import org.javanetworkanalyzer.model.KeyedGraph;
import org.javanetworkanalyzer.model.WeightedKeyedGraph;
import org.javanetworkanalyzer.model.WeightedPseudoG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a weighted graph with a specified globalOrientation from the given
 * {@link DataSet}.
 *
 * @author Adam Gouge
 */
public class WeightedGraphCreator<V extends VId, E extends Edge>
        extends GraphCreator<V, E> {

    /**
     * The name of the weight column.
     */
    private final String weightColumnName;
    protected int weightFieldIndex = -1;
    private static final Logger LOGGER =
            LoggerFactory.getLogger(GraphCreator.class);

    /**
     * Constructs a new {@link WeightedGraphCreator}.
     *
     * @param dataSet           The data set.
     * @param globalOrientation The globalOrientation.
     *
     */
    public WeightedGraphCreator(DataSet dataSet,
                                int orientation,
                                String edgeOrientationColumnName,
                                Class<? extends V> vertexClass,
                                Class<? extends E> edgeClass,
                                String weightColumnName) {
        super(dataSet, orientation, edgeOrientationColumnName,
              vertexClass, edgeClass);
        this.weightColumnName = weightColumnName;
    }

    /**
     * Constructs a new {@link WeightedGraphCreator}.
     *
     * @param dataSet           The data set.
     * @param globalOrientation The globalOrientation.
     *
     */
    public WeightedGraphCreator(DataSet dataSet,
                                int orientation,
                                Class<? extends V> vertexClass,
                                Class<? extends E> edgeClass,
                                String weightColumnName) {
        this(dataSet, orientation, null, vertexClass,
             edgeClass, weightColumnName);
    }

    @Override
    public WeightedKeyedGraph<V, E> prepareGraph() {
        return (WeightedKeyedGraph<V, E>) super.prepareGraph();
    }

    /**
     * Recovers the indices from the metadata.
     *
     * @param weightColumnName
     */
    @Override
    protected Metadata initializeIndices() {
        Metadata md = null;
        try {
            md = super.initializeIndices();
            if (md != null) {
                weightFieldIndex = md.getFieldIndex(
                        weightColumnName);
                verifyIndex(weightFieldIndex, weightColumnName);
            } else {
                throw new IllegalStateException(METADATA_ERROR);
            }
        } catch (IndexException ex) {
            LOGGER.error("Problem with indices.", ex);
        } catch (DriverException ex) {
            LOGGER.error(METADATA_ERROR, ex);
        }
        return md;
    }

    @Override
    protected KeyedGraph<V, E> initializeGraph() {
        KeyedGraph<V, E> graph;
        if (globalOrientation != UNDIRECTED) {
            // Weighted Directed or Reversed
            graph = new DirectedWeightedPseudoG<V, E>(vertexClass, edgeClass);
        } else {
            // Weighted Undirected
            graph = new WeightedPseudoG<V, E>(vertexClass, edgeClass);
        }
        return graph;
    }

    @Override
    protected E loadEdge(Value[] row, KeyedGraph<V, E> graph) {
        E edge = super.loadEdge(row, graph);
        if (edge != null) {
            double weight = row[weightFieldIndex].getAsDouble();
            edge.setWeight(weight);
        }
        return edge;
    }

    @Override
    protected E loadDoubleEdge(Value[] row,
                               KeyedGraph<V, E> graph,
                               final int startNode,
                               final int endNode) {
        // In directed graphs, undirected edges are represented
        // by directed edges in both directions.
        E edgeTo = graph.addEdge(startNode, endNode);
        E edgeFrom = graph.addEdge(endNode, startNode);
        double weight = row[weightFieldIndex].getAsDouble();
        edgeTo.setWeight(weight);
        edgeFrom.setWeight(weight);
        return edgeFrom;
    }
}
