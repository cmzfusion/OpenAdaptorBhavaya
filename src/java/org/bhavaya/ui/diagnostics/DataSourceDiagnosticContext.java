package org.bhavaya.ui.diagnostics;

import org.bhavaya.ui.MenuGroup;
import org.bhavaya.db.DataSourceFactory;
import org.bhavaya.db.BhavayaDataSource;
import org.bhavaya.db.ConnectionPoolDataSource;

import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: brendon
 * Date: Dec 8, 2005
 * Time: 12:11:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataSourceDiagnosticContext extends DiagnosticContext {
    public DataSourceDiagnosticContext() {
        super("DataSource", null);
    }

    public Component createComponent() {
        return null;
    }

    public String createHTMLDescription() {
        StringBuffer buffer = new StringBuffer();
        DiagnosticUtilities.contextHeader(buffer, "DataSources");

        DiagnosticUtilities.tableHeader(buffer);
        DiagnosticUtilities.tableHeaderRow(buffer, new String[]{"Name", "Catalog", "Schema", "Connection Id"});

        BhavayaDataSource[] dataSources = DataSourceFactory.getInstances();
        for (int i = 0; i < dataSources.length; i++) {
            BhavayaDataSource dataSource = dataSources[i];
            String name = dataSource.getDataSourceName();
            String defaultCatalog = dataSource.getDefaultCatalog();
            String defaultSchema = dataSource.getDefaultSchema();
            String connectionId = dataSource.getConnectionId();
            DiagnosticUtilities.tableRow(buffer, new Object[]{name, defaultCatalog, defaultSchema, connectionId});
        }

        DiagnosticUtilities.tableFooter(buffer);

        return buffer.toString();
    }

    public MenuGroup[] createMenuGroups() {
        return new MenuGroup[0];
    }

    public Attachment[] createAttachments() {
        return new Attachment[0];
    }

    public Object createMBean() {
        return new DataSourceDiagnostic();
    }

    public interface DataSourceDiagnosticMBean {
        public String[] getDataSources();
    }

    public class DataSourceDiagnostic implements DataSourceDiagnosticMBean {
        public String[] getDataSources() {
            BhavayaDataSource[] dataSources = DataSourceFactory.getInstances();
            String[] ret = new String[dataSources.length];
            for (int i = 0; i < dataSources.length; i++) {
                BhavayaDataSource dataSource = dataSources[i];
                String name = dataSource.getDataSourceName();
                String defaultCatalog = dataSource.getDefaultCatalog();
                String defaultSchema = dataSource.getDefaultSchema();
                String connectionId = dataSource.getConnectionId();
                ret[i] = "Name: " + name + "; Catalog: " + defaultCatalog + "; Schema: " + defaultSchema + "; ConnectionId: " + connectionId;

                if (dataSource instanceof ConnectionPoolDataSource) {
                    ConnectionPoolDataSource connectionPoolDataSource = (ConnectionPoolDataSource) dataSource;
                    ret[i] += "; Idle connections (idle/total): (" + connectionPoolDataSource.getIdleCount() + "/"
                            + connectionPoolDataSource.getConnectionCount() + ")";
                }
            }
            return ret;
        }
    }
}
