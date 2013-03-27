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
package org.gdms.gdmstopology.functionhelpers;

import java.util.Map;
import org.gdms.data.DataSourceFactory;
import org.gdms.driver.DiskBufferDriver;
import org.gdms.driver.DriverException;
import org.orbisgis.progress.ProgressMonitor;

/**
 * A helper class to store the results calculated by an executor function.
 *
 * We assume that the results are stored in a {@link Map}.
 *
 * @author Adam Gouge
 */
public abstract class ExecutorFunctionHelper extends FunctionHelper {

    /**
     * Constructor.
     *
     * @param dsf The {@link DataSourceFactory} used to parse the data set.
     * @param pm  The progress monitor used to track the progress of the
     *            calculation.
     */
    public ExecutorFunctionHelper(DataSourceFactory dsf,
                                  ProgressMonitor pm) {
        super(dsf, pm);
    }

    /**
     * Computes and stores the results in a {@link DiskBufferDriver} and writes
     * them to a table.
     *
     * @param outputTablePrefix The output table prefix.
     *
     * @throws DriverException
     */
    public void doWork(String outputTablePrefix)
            throws DriverException {
        // Write the DiskBufferDriver containing the results to a table.
        writeToTable(doWork(), outputTablePrefix);
    }

    /**
     * Writes the results stored in the driver to a new table.
     *
     * @param driver            The driver.
     * @param outputTablePrefix The output table prefix.
     *
     * @return The name of the table.
     *
     * @throws DriverException
     */
    private String writeToTable(DiskBufferDriver driver,
                                String outputTablePrefix) throws DriverException {
        // TODO: Necessary?
        driver.open();
        // Register the result in a new output table.
        String outputTableName = dsf.getSourceManager().
                getUniqueName(
                outputTablePrefix
                + "." + getOutputTableSuffix());
        dsf.getSourceManager().register(
                outputTableName,
                driver.getFile());
        return outputTableName;
    }

    /**
     * Returns the suffix that will be appended to the output table name.
     *
     * @return The suffix that will be appended to the output table name.
     *
     * @see #writeToTable(org.gdms.driver.DiskBufferDriver, java.lang.String)
     */
    protected abstract String getOutputTableSuffix();
}
