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
package org.gdms.gdmstopology.process;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.indexes.DefaultAlphaQuery;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DataSet;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.gdmstopology.model.DWMultigraphDataSource;
import org.gdms.gdmstopology.model.EdgeReversedGraphDataSource;
import org.gdms.gdmstopology.model.GDMSValueGraph;
import org.gdms.gdmstopology.model.GraphEdge;
import org.gdms.gdmstopology.model.GraphException;
import org.gdms.gdmstopology.model.GraphMetadataFactory;
import org.gdms.gdmstopology.model.GraphSchema;
import org.gdms.gdmstopology.model.WMultigraphDataSource;
import org.jgrapht.Graphs;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.traverse.ClosestFirstIterator;
import org.orbisgis.progress.NullProgressMonitor;
import org.orbisgis.progress.ProgressMonitor;

/**
 *
 * @author Erwan Bocher
 */
public class GraphPath extends GraphAnalysis {

    /**
     * Some usefull methods to analyse a graph network.
     */
    private GraphPath() {
    }

    /**
     * Find the shortest path between two nodes using the Dijkstra algorithm. It
     * returns a list of {@link GraphEdge}s.
     *
     * @param dsf
     * @param graph
     * @param startNode
     * @param endNode
     * @return
     */
    public static List<GraphEdge> findPathBetween(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            Integer startNode, Integer endNode) {
        return DijkstraShortestPath.findPathBetween(graph, startNode, endNode);
    }

    /**
     * Find the shortest path between two nodes using the Dijkstra algorithm.
     * The list of {@link GraphEdge}s is stored in a datasource.
     *
     * @param dsf
     * @param graph
     * @param startNode
     * @param endNode
     * @return
     */
    public static DiskBufferDriver findPathBetween2Nodes(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            Integer sourceVertex, Integer targetVertex, ProgressMonitor pm) throws GraphException, DriverException {
        return findPathBetween2Nodes(dsf, graph, sourceVertex, targetVertex, Double.POSITIVE_INFINITY, pm);
    }

    /**
     * Find the shortest path between two nodes using Dijkstra's algorithm.
     *
     * <p> A radius can be used to constrain the analysis of the graph. The list
     * of {@link GraphEdge}s is stored in a datasource. For each path, the
     * method returns all the geometries of the input graph datasource.
     *
     * Note that this returns the path in backwards order.
     *
     * @param dsf
     * @param graph
     * @param startNode
     * @param endNode
     * @param radius
     * @return
     */
    public static DiskBufferDriver findPathBetween2Nodes(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            Integer sourceVertex, Integer targetVertex, double radius, ProgressMonitor pm) throws GraphException, DriverException {

        // PRELIMINARIES
        // Make sure the graph contains the target vertex.
        if (!graph.containsVertex(targetVertex)) {
            throw new GraphException("The graph must contain the target vertex");
        }
        // An iterator to find the closest neighboring vertex.
        ClosestFirstIterator<Integer, GraphEdge> cl = new ClosestFirstIterator<Integer, GraphEdge>(graph, sourceVertex, radius);
        // A DiskBufferDriver to store the shortest path.
        DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, GraphMetadataFactory.createEdgeMetadataShortestPath());

        // CALCULATION
        // A counter used to interrupt the calculation.
        int count = 0;
        // Track the progress of the shortest path calculation.
        pm.startTask("Find shortest path", 100);

        // Go through the closest neighbors.
        while (cl.hasNext()) {

            // Check if the calculation has been canceled.
            if (count >= 100 && count % 100 == 0) {
                if (pm.isCancelled()) {
                    break;
                }
            }
            count++;

            // Obtain the next closest neighbor.
            int nextClosestNeighbor = cl.next();

            // RECOVERING THE SHORTEST PATH
            // Once we've reached the target vertex, 
            // work backwards to recover the shortest path.
            if (nextClosestNeighbor == targetVertex) {

                // Start at the target vertex.
                int currentVertex = targetVertex;
                // A counter to sequentially label the edges in terms of 
                // their order in the shortest path.
                int edgeID = 0;

                while (true) {
                    // Get the last edge in the shortest known path between 
                    // the source vertex and the current vertex. Note that
                    // this method returns null when we reach the start
                    // vertex.
                    GraphEdge currentEdge = cl.getSpanningTreeEdge(currentVertex);

                    // The only way to get out of this infinite loop.
                    // is by having a null current edge, which corresponds
                    // to the current vertex being the source vertex.
                    if (currentEdge == null) {
                        // TODO: Should we add an if statement or change this 
                        // one to make sure we are actually at the start vertex?
                        break;
                    }

                    // Record the current edge in the shortest path.
                    diskBufferDriver.addValues(
                            new Value[]{
                                // the_geom
                                ValueFactory.createValue(graph.getGeometry(currentEdge)),
                                // The row ID of the edge in the data source.
                                ValueFactory.createValue(currentEdge.getRowId()),
                                // An id for this edge in the shortest path.
                                ValueFactory.createValue(edgeID),
                                // Start node
                                ValueFactory.createValue(currentEdge.getSource()),
                                // End node
                                ValueFactory.createValue(currentEdge.getTarget()),
                                // Weight
                                ValueFactory.createValue(currentEdge.getWeight())});
                    // Increment the edge ID.
                    edgeID++;
                    // Update the current vertex to be the one across 
                    // from the current edge.
                    currentVertex = Graphs.getOppositeVertex(graph, currentEdge, currentVertex);
                }

                break; // TODO: What does this break do?
            } // Finished writing the shortest path to the diskBufferDriver.

        } // Finished going through the iterator

        // CLEAN UP
        // All the contents have been added to the file.
        diskBufferDriver.writingFinished();
        // The task is done.
        pm.endTask();
        // Close the file.
        diskBufferDriver.close();
        // And return it.
        return diskBufferDriver;
    }

    /**
     * Compute all shortest distances between one node. The distances are stored
     * in a datasource.
     *
     * @param dsf
     * @param graph
     * @param startNode
     * @param endNode
     * @return
     */
    public static DiskBufferDriver computeDistancesBetweenOneNode(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            Integer sourceVertex, ProgressMonitor pm) throws GraphException, DriverException {
        return computeDistancesBetweenOneNode(dsf, graph, sourceVertex, Double.POSITIVE_INFINITY, pm);
    }

    /**
     * Compute all shortest distancs between one node. A radius can be used to
     * constrain the analysis of the graph. The distances are stored in a
     * datasource.
     *
     * @param dsf
     * @param graph
     * @param startNode
     * @param endNode
     * @param radius
     * @return
     */
    public static DiskBufferDriver computeDistancesBetweenOneNode(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            Integer sourceVertex, double radius, ProgressMonitor pm) throws GraphException, DriverException {

        if (!graph.containsVertex(sourceVertex)) {
            throw new GraphException(
                    "The graph must contain the target vertex");
        }

        DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, GraphMetadataFactory.createDistancesMetadataGraph());

        ClosestFirstIterator<Integer, GraphEdge> cl = new ClosestFirstIterator<Integer, GraphEdge>(graph, sourceVertex, radius);

        int count = 0;
        pm.startTask("Calculate distances path", 100);
        while (cl.hasNext()) {
            if (count >= 100 && count % 100 == 0) {
                if (pm.isCancelled()) {
                    break;
                }
            }
            count++;
            Integer node = cl.next();
            if (node != sourceVertex) {
                double length = cl.getShortestPathLength(node);
                diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(node),
                            ValueFactory.createValue(length)});

            }
        }
        diskBufferDriver.writingFinished();
        pm.endTask();
        diskBufferDriver.close();

        return diskBufferDriver;

    }

    /**
     * Find the shortest path between sereval nodes using the Dijkstra
     * algorithm. A radius can be used to constrain the analysis of the graph. A
     * datasource that contains all destinations must be used following the
     * schema : id :: int, source::int, target::int. The algorithm iters all
     * paths (source, target) and returns the results in a datasource. Duplicate
     * paths (ie rows with the same source and target values) are computed once
     * again.
     *
     * @param dsf
     * @param graph
     * @param nodes
     * @param radius
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver findPathBetweenSeveralNodes(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            DataSet nodes, ProgressMonitor pm) throws GraphException, DriverException {
        return findPathBetweenSeveralNodes(dsf, graph, nodes, Double.POSITIVE_INFINITY, pm);
    }

    /**
     * Find the shortest path between sereval nodes using the Dijkstra
     * algorithm. A datasource that contains all destinations must be used
     * following the schema : id :: int, source::int, target::int. The algorithm
     * iters all paths (source, target) and returns the results in a datasource.
     * Duplicate paths (ie rows with the same source and target values) are
     * computed once again.
     *
     * @param dsf
     * @param graph
     * @param nodes
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver findPathBetweenSeveralNodes(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            DataSet nodes, double radius, ProgressMonitor pm) throws GraphException, DriverException {
        initIndex(dsf, nodes, pm);
        DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, GraphMetadataFactory.createEdgeMetadataShortestPath());

        Iterator<Value[]> it = nodes.iterator();
        ClosestFirstIterator<Integer, GraphEdge> cl = null;
        HashSet<Integer> visitedSources = new HashSet<Integer>();
        int count = 0;
        pm.startTask("Processing input nodes", 100);
        while (it.hasNext()) {
            Value[] values = it.next();
            int source = values[SOURCE_FIELD_INDEX].getAsInt();
            HashMap<Integer, Integer> targets = null;
            if (count >= 100 && count % 100 == 0) {
                if (pm.isCancelled()) {
                    break;
                }
            }
            count++;

            if (!visitedSources.contains(source)) {
                cl = new ClosestFirstIterator<Integer, GraphEdge>(graph, source);
                targets = getTargets(dsf, nodes, source);
                int targetsNumber = targets.size();
                int targetVisisted = 0;
                boolean isAllTargetsDone = false;
                while (cl.hasNext() && (!isAllTargetsDone)) {
                    int vertex = cl.next();
                    if (targets.containsKey(vertex)) {
                        targetVisisted++;
                        isAllTargetsDone = targetsNumber - targetVisisted == 0;
                        int v = vertex;
                        int idNodes = targets.get(vertex);
                        int k = 0;
                        while (true) {
                            GraphEdge edge = cl.getSpanningTreeEdge(v);
                            if (edge == null) {
                                break;
                            }
                            diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(graph.getGeometry(edge)),
                                        ValueFactory.createValue(idNodes),
                                        ValueFactory.createValue(k),
                                        ValueFactory.createValue(edge.getSource()),
                                        ValueFactory.createValue(edge.getTarget()),
                                        ValueFactory.createValue(edge.getWeight())});
                            k++;
                            v = Graphs.getOppositeVertex(graph, edge, v);
                        }
                    }
                }
                visitedSources.add(source);
            }

        }
        diskBufferDriver.writingFinished();
        diskBufferDriver.close();
        pm.endTask();
        return diskBufferDriver;
    }

    /**
     *
     * @param dsf
     * @param graph
     * @param nodes
     * @param pm
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver computeDistanceBetweenSeveralNodes(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            DataSet nodes, ProgressMonitor pm) throws GraphException, DriverException {
        return computeDistanceBetweenSeveralNodes(dsf, graph, nodes, Double.POSITIVE_INFINITY, pm);

    }

    /**
     *
     * @param dsf
     * @param graph
     * @param nodes
     * @param radius
     * @param pm
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver computeDistanceBetweenSeveralNodes(DataSourceFactory dsf, GDMSValueGraph<Integer, GraphEdge> graph,
            DataSet nodes, double radius, ProgressMonitor pm) throws GraphException, DriverException {
        initIndex(dsf, nodes, new NullProgressMonitor());
        DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, GraphMetadataFactory.createDistancesMetadataGraph());

        Iterator<Value[]> it = nodes.iterator();
        ClosestFirstIterator<Integer, GraphEdge> cl = null;
        HashSet<Integer> visitedSources = new HashSet<Integer>();
        int count = 0;
        pm.startTask("Compute distance from nodes", 100);
        while (it.hasNext()) {
            Value[] values = it.next();
            if (count >= 100 && count % 100 == 0) {
                if (pm.isCancelled()) {
                    break;
                }
            }
            count++;

            int source = values[SOURCE_FIELD_INDEX].getAsInt();
            HashMap<Integer, Integer> targets = null;
            if (!visitedSources.contains(source)) {
                cl = new ClosestFirstIterator<Integer, GraphEdge>(graph, source);
                targets = getTargets(dsf, nodes, source);
                int targetsNumber = targets.size();
                int targetVisisted = 0;
                boolean isAllTargetsDone = false;
                while (cl.hasNext() && (!isAllTargetsDone)) {
                    int vertex = cl.next();
                    if (targets.containsKey(vertex)) {
                        targetVisisted++;
                        isAllTargetsDone = targetsNumber - targetVisisted == 0;
                        int v = vertex;
                        int idNodes = targets.get(vertex);
                        int k = 0;
                        double sum = 0;
                        while (true) {
                            GraphEdge edge = cl.getSpanningTreeEdge(v);
                            if (edge == null) {
                                break;
                            }
                            sum += edge.getWeight();
                            k++;
                            v = Graphs.getOppositeVertex(graph, edge, v);
                        }
                        diskBufferDriver.addValues(new Value[]{
                                    ValueFactory.createValue(idNodes),
                                    ValueFactory.createValue(sum)});

                    }

                }
                visitedSources.add(source);
            }

        }
        diskBufferDriver.writingFinished();
        diskBufferDriver.close();
        pm.endTask();
        return diskBufferDriver;
    }

    /**
     * Return as set of geometries that represent the shortest path between two
     * nodes.
     *
     * @param dsf
     * @param dataSet
     * @param source
     * @param target
     * @param costField
     * @param graphType
     * @param pm
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver getShortestPath(DataSourceFactory dsf, DataSet dataSet, int source, int target, String costField, int graphType, ProgressMonitor pm) throws GraphException, DriverException {
        if (graphType == GraphSchema.DIRECT) {
            DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
            dwMultigraphDataSource.setWeightFieldIndex(costField);
            return findPathBetween2Nodes(dsf, dwMultigraphDataSource, source, target, pm);
        } else if (graphType == GraphSchema.DIRECT_REVERSED) {
            DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
            dwMultigraphDataSource.setWeightFieldIndex(costField);
            EdgeReversedGraphDataSource edgeReversedGraph = new EdgeReversedGraphDataSource(dwMultigraphDataSource);
            return findPathBetween2Nodes(dsf, edgeReversedGraph, source, target, pm);
        } else if (graphType == GraphSchema.UNDIRECT) {
            WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(dsf, dataSet, pm);
            wMultigraphDataSource.setWeightFieldIndex(costField);
            return findPathBetween2Nodes(dsf, wMultigraphDataSource, source, target, pm);
        } else {
            throw new GraphException("Only three types of graphs "
                    + "are allowed: enter 1 if the graph is "
                    + "directed, 2 if it is directed and you wish to reverse the "
                    + "orientation of the edges, and 3 if the graph is undirected. "
                    + "If no orientation is specified, the graph is assumed "
                    + "to be directed.");
        }
    }

    /**
     * Return all shortest paths from a set of start and target nodes. The
     * dataset that contains all nodes must following the schema : id (int or
     * long), source (int or long) ,target(int or long)
     *
     * @param dsf
     * @param dataSet
     * @param nodes
     * @param costField
     * @param graphType
     * @param pm
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver getMShortestPath(DataSourceFactory dsf, DataSet dataSet, DataSet nodes, String costField, int graphType, ProgressMonitor pm) throws GraphException, DriverException {
        if (checkMetadata(nodes)) {
            if (graphType == GraphSchema.DIRECT) {
                DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                dwMultigraphDataSource.setWeightFieldIndex(costField);
                return findPathBetweenSeveralNodes(dsf, dwMultigraphDataSource, nodes, pm);
            } else if (graphType == GraphSchema.DIRECT_REVERSED) {
                DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                dwMultigraphDataSource.setWeightFieldIndex(costField);
                EdgeReversedGraphDataSource edgeReversedGraph = new EdgeReversedGraphDataSource(dwMultigraphDataSource);
                return findPathBetweenSeveralNodes(dsf, edgeReversedGraph, nodes, pm);
            } else if (graphType == GraphSchema.UNDIRECT) {
                WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(dsf, dataSet, pm);
                wMultigraphDataSource.setWeightFieldIndex(costField);
                return findPathBetweenSeveralNodes(dsf, wMultigraphDataSource, nodes, pm);
            } else {
                throw new GraphException("Only 3 type of graphs are allowed."
                        + "1 if the path is computing using a directed graph.\n"
                        + "2 if the path is computing using a directed graph and edges are reversed\n"
                        + "3 if the path is computing using a undirected.");
            }
        } else {
            throw new GraphException("The table nodes must contains the field id, source and target");
        }
    }

    /**
     * Compute the shortest path distance between two nodes.
     *
     * @param dsf
     * @param dataSet
     * @param source
     * @param target
     * @param costField
     * @param graphType
     * @param pm
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver getShortestPathLength(DataSourceFactory dsf, DataSet dataSet, int source, String costField, int graphType, ProgressMonitor pm) throws GraphException, DriverException {
        if (graphType == GraphSchema.DIRECT) {
            DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
            dwMultigraphDataSource.setWeightFieldIndex(costField);
            return computeDistancesBetweenOneNode(dsf, dwMultigraphDataSource, source, pm);
        } else if (graphType == GraphSchema.DIRECT_REVERSED) {
            DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
            dwMultigraphDataSource.setWeightFieldIndex(costField);
            EdgeReversedGraphDataSource edgeReversedGraph = new EdgeReversedGraphDataSource(dwMultigraphDataSource);
            return computeDistancesBetweenOneNode(dsf, edgeReversedGraph, source, pm);
        } else if (graphType == GraphSchema.UNDIRECT) {
            WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(dsf, dataSet, pm);
            wMultigraphDataSource.setWeightFieldIndex(costField);
            return computeDistancesBetweenOneNode(dsf, wMultigraphDataSource, source, pm);
        } else {
            throw new GraphException("Only 3 type of graphs are allowed."
                    + "1 if the path is computing using a directed graph.\n"
                    + "2 if the path is computing using a directed graph and edges are reversed\n"
                    + "3 if the path is computing using a undirected.");
        }
    }

    /**
     * Compute the shortest path distance between several nodes.
     *
     * @param dsf
     * @param dataSet
     * @param source
     * @param target
     * @param costField
     * @param graphType
     * @param pm
     * @return
     * @throws GraphException
     * @throws DriverException
     */
    public static DiskBufferDriver getMShortestPathLength(DataSourceFactory dsf, DataSet dataSet, DataSet nodes, String costField, int graphType, ProgressMonitor pm) throws GraphException, DriverException {
        if (checkMetadata(nodes)) {
            if (graphType == GraphSchema.DIRECT) {
                DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                dwMultigraphDataSource.setWeightFieldIndex(costField);
                return computeDistanceBetweenSeveralNodes(dsf, dwMultigraphDataSource, nodes, pm);
            } else if (graphType == GraphSchema.DIRECT_REVERSED) {
                DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(dsf, dataSet, pm);
                dwMultigraphDataSource.setWeightFieldIndex(costField);
                EdgeReversedGraphDataSource edgeReversedGraph = new EdgeReversedGraphDataSource(dwMultigraphDataSource);
                return computeDistanceBetweenSeveralNodes(dsf, edgeReversedGraph, nodes, pm);
            } else if (graphType == GraphSchema.UNDIRECT) {
                WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(dsf, dataSet, pm);
                wMultigraphDataSource.setWeightFieldIndex(costField);
                return computeDistanceBetweenSeveralNodes(dsf, wMultigraphDataSource, nodes, pm);
            } else {
                throw new GraphException("Only 3 type of graphs are allowed."
                        + "1 if the path is computing using a directed graph.\n"
                        + "2 if the path is computing using a directed graph and edges are reversed\n"
                        + "3 if the path is computing using a undirected.");
            }
        } else {
            throw new GraphException("The table nodes must contains the field id, source and target");
        }
    }

    /**
     * Query the dataset using an alphanumeric index
     *
     * @param fieldToQuery
     * @param valueToQuery
     * @return a map where key = target value and value = id value
     * @throws DriverException
     */
    public static HashMap<Integer, Integer> getTargets(DataSourceFactory dsf, DataSet nodes, Integer valueToQuery) throws DriverException {
        DefaultAlphaQuery defaultAlphaQuery = new DefaultAlphaQuery(
                GraphSchema.SOURCE_NODE, ValueFactory.createValue(valueToQuery));
        Iterator<Integer> iterator = nodes.queryIndex(dsf, defaultAlphaQuery);
        HashMap<Integer, Integer> targets = new HashMap<Integer, Integer>();
        while (iterator.hasNext()) {
            Integer integer = iterator.next();
            targets.put(nodes.getInt(integer, TARGET_FIELD_INDEX), nodes.getInt(integer, ID_FIELD_INDEX));
        }
        return targets;
    }
}
