/* Copyright (C) 2000-2003 The Software Conservancy as Trustee.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 *
 * Nothing in this notice shall be deemed to grant any rights to trademarks,
 * copyrights, patents, trade secrets or any other intellectual property of the
 * licensor or any contributor except as expressly stated herein. No patent
 * license is granted separate from the Software, for code that you delete from
 * the Software, or for combinations of the Software with other software or
 * hardware.
 */

package org.bhavaya.beans;

import org.bhavaya.util.Log;
import org.openadaptor.dataobjects.DOAttribute;
import org.openadaptor.dataobjects.DOType;
import org.openadaptor.dataobjects.DataObject;
import org.openadaptor.dataobjects.InvalidParameterException;
import org.openadaptor.util.DateHolder;
import org.openadaptor.util.DateTimeHolder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Description.
 *
 * @author Parwinder Sekhon
 * @version $Revision: 1.3 $
 */
public class DataObjectTabularData implements TabularData {
    private static final Log log = Log.getCategory(DataObjectTabularData.class);

    private Map columnsByName = new LinkedHashMap();
    private Column[] columns;
    private DataObject[] dataObjects;
    private int index;

    public DataObjectTabularData(DataObject[] dataObjects) {
        this.dataObjects = dataObjects;
        this.index = -1;

        DOType type = dataObjects[0].getType();
        DOAttribute[] doAttributes = type.getAttributes();
        columns = new Column[doAttributes.length];

        DataObjectMapper dataObjectMapper = DataObjectMapper.getInstance();
        for (int i = 0; i < doAttributes.length; i++) {
            DOAttribute doAttribute = doAttributes[i];
            String name = doAttribute.getName();
            Class columnType = dataObjectMapper.mapDOTypeToJavaClass(doAttribute.getType());
            columns[i] = new Column(name, columnType);
            columnsByName.put(name, columns[i]);
        }
    }

    public TabularData.Row next() {
        if (index + 1 >= dataObjects.length) return null;
        index++;
        Row row = new DataObjectRow(index);
        return row;
    }

    public int getColumnCount() {
        return columns.length;
    }

    public Column getColumn(int columnIndex) {
        return columns[columnIndex];
    }

    public Column[] getColumns() {
        return columns;
    }

    public void close() {
    }

    private class DataObjectRow implements TabularData.Row {
        private int index;

        public DataObjectRow(int index) {
            this.index = index;
        }

        public int getRowType() {
            return TabularData.ROW_TYPE_SELECT;
        }

        public Object getColumnValue(Column column) {
            return getColumnValue(column, null);
        }

        public Object getColumnValue(Column column, Class expectedType) {
            try {
                Object attributeValue = dataObjects[index].getAttributeValue(column.getName());
                if (attributeValue instanceof DateTimeHolder) { // do this before DateHolder, as DateTimeHolder is a subclass of DateHolder
                    attributeValue = DataObjectMapper.getDateTime((DateTimeHolder) attributeValue);
                } else if (attributeValue instanceof DateHolder) {
                    attributeValue = DataObjectMapper.getDate((DateHolder) attributeValue);
                }
                return attributeValue;
            } catch (InvalidParameterException e) {
                log.error(e);
                return null;
            }
        }

        public Object getColumnValue(int columnIndex) {
            return getColumnValue(columnIndex, null);
        }

        public Object getColumnValue(int columnIndex, Class expectedType) {
            Column column = columns[columnIndex];
            return getColumnValue(column);
        }

        public TabularData getTabularData() {
            return DataObjectTabularData.this;
        }

        public boolean isModified(Column column) {
            return true;
        }
    }
}
