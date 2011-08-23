package org.gdms.gdmstopology.function;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.gdms.data.DataSource;
import org.gdms.data.DataSourceFactory;
import org.gdms.data.SQLDataSourceFactory;
import org.gdms.data.SpatialDataSourceDecorator;
import org.gdms.data.schema.DefaultMetadata;
import org.gdms.data.schema.Metadata;
import org.gdms.data.types.Type;
import org.gdms.data.types.TypeFactory;
import org.gdms.data.values.Value;
import org.gdms.data.values.ValueFactory;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.gdms.driver.ObjectDriver;
import org.gdms.driver.ReadAccess;
import org.gdms.gdmstopology.model.DWMultigraphDataSource;
import org.gdms.gdmstopology.model.GraphEdge;
import org.gdms.gdmstopology.model.GraphSchema;
import org.gdms.gdmstopology.model.WMultigraphDataSource;
import org.gdms.sql.function.FunctionException;
import org.gdms.sql.function.FunctionSignature;
import org.gdms.sql.function.ScalarArgument;
import org.gdms.sql.function.table.AbstractTableFunction;
import org.gdms.sql.function.table.TableDefinition;
import org.gdms.sql.function.table.TableFunctionSignature;
import org.orbisgis.progress.ProgressMonitor;
import org.jgrapht.traverse.ClosestFirstIterator;

/**
 *
 * @author ebocher
 */
public class ST_ShortestPathLength extends AbstractTableFunction {

        @Override
        public ReadAccess evaluate(SQLDataSourceFactory dsf, ReadAccess[] tables, Value[] values, ProgressMonitor pm) throws FunctionException {
                int source = values[0].getAsInt();

                SpatialDataSourceDecorator sdsEdges = new SpatialDataSourceDecorator(tables[0]);

                if (values.length == 2) {
                        if (values[1].getAsBoolean()) {
                                return computeWMPath(dsf, sdsEdges, source, pm);
                        } else {
                                return computeDWMPath(dsf, sdsEdges, source,pm);
                        }

                } else {
                        return computeDWMPath(dsf, sdsEdges, source,pm);
                }


        }

        @Override
        public String getName() {
                return "ST_ShortestPathLength";
        }

        @Override
        public String getDescription() {
                return "Return the shortest path length beetwen one vertex to all other.";
        }

        @Override
        public String getSqlOrder() {
                return "SELECT ST_ShortestPathLength(12[,true]) ) from data;";
        }

        @Override
        public Metadata getMetadata(Metadata[] tables) throws DriverException {
                Metadata md = new DefaultMetadata(
                        new Type[]{TypeFactory.createType(Type.INT), TypeFactory.createType(Type.INT), TypeFactory.createType(Type.DOUBLE)},
                        new String[]{GraphSchema.ID, GraphSchema.END_NODE, GraphSchema.WEIGTH});
                return md;
        }

        @Override
        public FunctionSignature[] getFunctionSignatures() {
                return new FunctionSignature[]{
                        new TableFunctionSignature(TableDefinition.ANY, ScalarArgument.INT), 
                        new TableFunctionSignature(TableDefinition.ANY, ScalarArgument.INT, ScalarArgument.BOOLEAN)
                };
        }

        private ObjectDriver computeWMPath(DataSourceFactory dsf, SpatialDataSourceDecorator sds, int source, ProgressMonitor pm) {
                WMultigraphDataSource wMultigraphDataSource = new WMultigraphDataSource(sds, pm);
                try {
                        wMultigraphDataSource.open();

                        ClosestFirstIterator<Integer, GraphEdge> cl = new ClosestFirstIterator<Integer, GraphEdge>(
                                wMultigraphDataSource, source);

                        DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, getMetadata(null));

                        while (cl.hasNext()) {
                                Integer node = cl.next();
                                if (node != source) {
                                        double length = cl.getShortestPathLength(node);
                                        diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(source), ValueFactory.createValue(node), ValueFactory.createValue(length)});

                                }
                        }
                        diskBufferDriver.writingFinished();
                        wMultigraphDataSource.close();
                        return diskBufferDriver;

                } catch (DriverException ex) {
                        Logger.getLogger(ST_ShortestPathLength.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
        }

        private ObjectDriver computeDWMPath(DataSourceFactory dsf, SpatialDataSourceDecorator sds, int source, ProgressMonitor pm) {
                DWMultigraphDataSource dwMultigraphDataSource = new DWMultigraphDataSource(sds, pm);
                try {
                        dwMultigraphDataSource.open();

                        ClosestFirstIterator<Integer, GraphEdge> cl = new ClosestFirstIterator<Integer, GraphEdge>(
                                dwMultigraphDataSource, source);

                        DiskBufferDriver diskBufferDriver = new DiskBufferDriver(dsf, getMetadata(null));

                        while (cl.hasNext()) {
                                Integer node = cl.next();
                                if (node != source) {
                                        double length = cl.getShortestPathLength(node);
                                        diskBufferDriver.addValues(new Value[]{ValueFactory.createValue(source), ValueFactory.createValue(node), ValueFactory.createValue(length)});

                                }
                        }
                        diskBufferDriver.writingFinished();
                        dwMultigraphDataSource.close();
                        return diskBufferDriver;

                } catch (DriverException ex) {
                        Logger.getLogger(ST_ShortestPathLength.class.getName()).log(Level.SEVERE, null, ex);
                }
                return null;
        }
}
