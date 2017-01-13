package org.bhavaya.db;

import junit.framework.TestCase;
import org.bhavaya.db.CatalogSchemaTable;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.1 $
 */
public class CatalogSchemaTableTest extends TestCase {
    public CatalogSchemaTableTest(String s) {
        super(s);
    }

    public void testOneComponent() {
        String string = "tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, null, true, true);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName() == null);
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName() == null);
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("tableC"));
    }

    public void testOneComponentDefaultCatalog() {
        String string = "tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, "catalogB", null, true, false);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName().equals("catalogB"));
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName() == null);
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("catalogB.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("catalogB.tableC"));
    }

    public void testOneComponentDefaultSchema() {
        String string = "tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, "schemaD", false, true);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName() == null);
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName().equals("schemaD"));
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("schemaD.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("schemaD.tableC"));
    }

    public void testOneComponentDefaultCatalogAndSchema() {
        String string = "tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, "catalogB", "schemaD", true, true);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName().equals("catalogB"));
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName().equals("schemaD"));
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("catalogB.schemaD.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("catalogB.schemaD.tableC"));
    }

    public void testTwoComponentsWithCatalog() {
        String string = "catalogA.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, null, true, false);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName().equals("catalogA"));
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName() == null);
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("catalogA.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("catalogA.tableC"));
    }

    public void testTwoComponentsWithSchema() {
        String string = "schemaB.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, null, false, true);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName() == null);
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName().equals("schemaB"));
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("schemaB.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("schemaB.tableC"));
    }

    public void testTwoComponentsNoCatalogSchemaSupport() {
        String string = "dummy.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, null, false, false);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName() == null);
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName() == null);
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("tableC"));
    }

    public void testTwoComponentsDefaultCatalog() {
        String string = "catalogB.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, "catalogC", null, true, false);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName().equals("catalogB"));
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName() == null);
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("catalogB.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("catalogB.tableC"));
    }

    public void testTwoComponentsDefaultSchema() {
        String string = "schemaB.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, "schemaD", false, true);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName() == null);
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName().equals("schemaB"));
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("schemaB.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("schemaB.tableC"));
    }

    public void testTwoComponentsDefaultCatalogAndSchema() {
        String string = "catalogB.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, "catalogC", "schemaD", true, true);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName().equals("catalogB"));
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName().equals("schemaD"));
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("catalogB.schemaD.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("catalogB.schemaD.tableC"));
    }



    public void testThreeComponentsNoCatalogSchemaSupport() {
        String string = "catalogA.schemaB.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, null, false, false);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName() == null);
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName() == null);
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("tableC"));
    }

    public void testThreeComponentsCatalogSchemaSupport() {
        String string = "catalogA.schemaB.tableC";
        CatalogSchemaTable catalogSchemaTable = CatalogSchemaTable.getInstance(string, null, null, true, true);
        assertTrue(catalogSchemaTable.getCatalogSchema().getCatalogName().equals("catalogA"));
        assertTrue(catalogSchemaTable.getCatalogSchema().getSchemaName().equals("schemaB"));
        assertTrue(catalogSchemaTable.getTableName().equals("tableC"));
        assertTrue(catalogSchemaTable.getRepresentation().equals("catalogA.schemaB.tableC"));
        assertTrue(catalogSchemaTable.getTableRepresentation().equals("catalogA.schemaB.tableC"));
    }
}
