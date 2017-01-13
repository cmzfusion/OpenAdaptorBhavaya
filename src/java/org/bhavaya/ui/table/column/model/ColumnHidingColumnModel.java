package org.bhavaya.ui.table.column.model;

import javax.swing.table.TableColumn;
import java.util.Enumeration;
import java.util.List;
import java.util.LinkedList;

/**
 * Created by IntelliJ IDEA.
 * User: Nick Ebbutt
 * Date: 18-Sep-2009
 * Time: 13:27:25
 *
 * A column model which can hide and show groups of columns.
 *
 * This model supports only columns which are instances of HidableTableColumn, so the framework should guarantee to create
 * only HidableTableColumn instances. n.b. There is support for loading standard TableColumn instances from legacy configs,
 * - these will be converted when we add the column to the column model.
 *
 * This model maintains a reference to the 'hidden' columns. Hidden columns are no longer in the columnModel so
 * far as the JTable is concerned - they are not visible via the standard TableColumnModel interface.
 * Extra methods are provided to access the 'hidden' columns, and to hide and show column groups.
 *
 * In the current design we keep track of hidden columns by logically associating them with the visible column on their left.
 * So each visible column may have linked list of associated hidden columns, which would appear after it in the view if all column groups were shown.
 * If a visible column is moved, any hidden columns associated with it are relinked to the nearest visible column on the moved column's left
 * This maintains the order when visible columns are moved, so that moving a visible column does not move hidden columns
 */
public class ColumnHidingColumnModel extends ObservableWidthColumnModel {

    //a dummy column which is used to hold a reference to any hidden columns which are
    //logically leftmost in the model. The anchor column itself is never visible.
    private HidableTableColumn dummyAnchorColumn = new HidableTableColumn();

    public void addColumn(TableColumn aColumn) {
        //old configs and view configs may deserialize raw TableColumn instances.
        //To handle this we convert them here
        if ( ! (aColumn instanceof HidableTableColumn)) {
            aColumn = new HidableTableColumn(aColumn);    
        }
        super.addColumn(aColumn);
    }

    public void removeColumn(TableColumn column) {
        //handle removing hidden columns as well as visible
        visitColumns(new RemoveColumnVisitor(column));
    }

    //move visible columns, moving columns while hidden is not supported
    public void moveColumn(int columnIndex, int newIndex) {
        //when we move a visible column, we want any associated hidden columns to stay put
        //so add them to the end of the previous visible column's hidden col list instead
        HidableTableColumn c = (HidableTableColumn)getColumn(columnIndex);
        c.relinkHiddenColumns(getPreviousVisibleColumnOrAnchor(columnIndex));
        super.moveColumn(columnIndex, newIndex);
    }
    
    public TableColumn getColumn(int columnIndex) {
	    return super.getColumn(columnIndex);
    }

    public boolean hideColumn(TableColumn h) {
        return visitColumns(new HideColumnVisitor(h)).isSuccessful();
    }

    public void hideColumnsInGroup(String groupName) {
        hideColumnsInGroup(new ColumnGroup(groupName));
    }

    public void hideColumnsInGroup(ColumnGroup columnGroup) {
        visitColumns(new HideColumnsInGroupVisitor(columnGroup));
    }

    public boolean showColumn(TableColumn h) {
        return visitColumns(new ShowColumnVisitor(h)).isSuccessful();
    }

    public boolean showColumnsInGroup(String groupName) {
        return showColumnsInGroup(new ColumnGroup(groupName));
    }

    public boolean showColumnsInGroup(ColumnGroup columnGroup) {
        return visitColumns(new ShowColumnInGroupVisitor(columnGroup)).isSuccessful();
    }

    public void showAllColumns() {
        visitColumns(new ShowAllColumnsVisitor()).isSuccessful();
    }

    public HidableTableColumn getColumnByIdentifier(Object identifier) {
        return visitColumns(new FindColumnByIdVisitor(identifier)).getResult();
    }

    public HidableTableColumn getColumnByHeader(Object header) {
        return visitColumns(new FindColumnByHeaderVisitor(header)).getResult();
    }

    public List<HidableTableColumn> getVisibleColumns() {
        return visitColumns(new FindVisibleColumnVisitor()).getResult();
    }

    public List<HidableTableColumn> getHiddenColumns() {
        return visitColumns(new FindHiddenColumnVisitor()).getResult();
    }

    public List<Object> getVisibleColumnHeaders() {
        return visitColumns(new FindVisibleHeaderVisitor()).getResult();
    }

    public int indexOf(TableColumn c) {
        return tableColumns.indexOf(c);
    }

    //visit all the columns in the model, whether visible or hidden
    private <E> HidableTableColumn.HidableColumnVisitor<E> visitColumns(HidableTableColumn.HidableColumnVisitor<E> v) {
        List<HidableTableColumn> l = getAllColumns();
        for (HidableTableColumn h : l) {
            h.acceptVisitor(v);
            if ( v.isFinished()) {
                break;
            }
        }
        return v;
    }

    /**
     * @return a list of all columns in the model, including both visible and hidden columns
     */
    public List<HidableTableColumn> getAllColumns() {
        //add any hidden columns linked with the dummy anchor column, which is never included
        List<HidableTableColumn> l = new LinkedList<HidableTableColumn>();
        addHiddenColumnsFromAnchorColumn(l);

        //add all visible columns with any associated hidden ones
        Enumeration<TableColumn> e = getColumns();
        while ( e.hasMoreElements()) {
            l.addAll(((HidableTableColumn)e.nextElement()).getAllColumns());
        }
        return l;
    }

    private void addHiddenColumnsFromAnchorColumn(List<HidableTableColumn> l) {
        List<HidableTableColumn> hiddenAnchorColumns = dummyAnchorColumn.getHiddenColumns();
        if ( hiddenAnchorColumns.size() > 0) {
            l.addAll(hiddenAnchorColumns);
        }
    }

    //to insert a column we seem to have to add it to the end and then move it!
    private void insertColumn(HidableTableColumn h, int insertIndex) {
        ColumnHidingColumnModel.super.addColumn(h);
        ColumnHidingColumnModel.super.moveColumn(getColumnCount() - 1, insertIndex);
    }

    /**
     * @return the visible column at the previous index in the TableModel, or the anchor column
     * if there are no such columns
     */
    private HidableTableColumn getPreviousVisibleColumnOrAnchor(int visibleColIndex) {
        return visibleColIndex > 0 ? (HidableTableColumn) tableColumns.get(visibleColIndex - 1) : dummyAnchorColumn;
    }


    class FindColumnByHeaderVisitor extends AbstractFindColumnVistor {
        private Object header;

        public FindColumnByHeaderVisitor(Object header) {
            this.header = header;
        }

        protected boolean isColumnMatching(HidableTableColumn h) {
            return h.getHeaderValue().equals(header);
        }
    }

    class FindColumnByIdVisitor extends AbstractFindColumnVistor {
        private Object id;

        public FindColumnByIdVisitor(Object id) {
            this.id = id;
        }

        protected boolean isColumnMatching(HidableTableColumn h) {
            return h.getIdentifier().equals(id);
        }
    }

    class RemoveColumnVisitor extends AbstractHidableColumnVisitor {
        private TableColumn column;

        public RemoveColumnVisitor(TableColumn column) {
            this.column = column;
        }

        protected void doVisited(HidableTableColumn h) {
            if ( h == column) {
                if ( h.isHidden()) {
                    h.removeFromHiddenColsList();
                } else {
                    h.relinkHiddenColumns(getLastVisibleColOrAnchor());
                    ColumnHidingColumnModel.super.removeColumn(h);
                }
                setSuccess(true).setFinished(true);
            }
        }
    }


    abstract class AbstractHideColumnVisitor extends AbstractHidableColumnVisitor<Object> {

        protected void doVisited(HidableTableColumn h) {
            if (shouldHideColumn(h)) {
                if (! h.isHidden()) {
                    ColumnHidingColumnModel.super.removeColumn(h);
                    HidableTableColumn columnToLink = getLastVisibleColOrAnchor();
                    columnToLink.addToHiddenColumnList(h);
                }
            }
        }

        protected abstract boolean shouldHideColumn(HidableTableColumn h);
    }

    class HideColumnsInGroupVisitor extends AbstractHideColumnVisitor {
        private ColumnGroup columnGroup;

        public HideColumnsInGroupVisitor(ColumnGroup columnGroup) {
            this.columnGroup = columnGroup;
        }

        protected boolean shouldHideColumn(HidableTableColumn h) {
            return columnGroup.equals(h.getColumnGroup());
        }
    }

    class HideColumnVisitor extends AbstractHideColumnVisitor {
        private TableColumn columnToHide;

        public HideColumnVisitor(TableColumn columnToHide) {
            this.columnToHide = columnToHide;
        }

        protected boolean shouldHideColumn(HidableTableColumn h) {
            boolean result = h == columnToHide;
            if ( result ) {
                setSuccess(true).setFinished(true);
            }
            return result;
        }
    }

    abstract class AbstractShowColumnVisitor extends AbstractHidableColumnVisitor<Object> {

        protected void doVisited(HidableTableColumn h) {
            if (shouldShowColumn(h)) {
                if ( h.isHidden() ) {
                    h.detachToMakeVisible();
                    int insertIndex = getLastVisibleIndex() + 1;
                    insertColumn(h, insertIndex);
                }
            }
        }

        protected abstract boolean shouldShowColumn(HidableTableColumn h);

    }

    class ShowColumnVisitor extends AbstractShowColumnVisitor {
        private TableColumn columnToShow;

        public ShowColumnVisitor(TableColumn columnToShow) {
            this.columnToShow = columnToShow;
        }

        protected boolean shouldShowColumn(HidableTableColumn h) {
            boolean result = h == columnToShow;
            if ( result ) {
                setSuccess(true).setFinished(true);
            }
            return result;
        }
    }

    class ShowColumnInGroupVisitor extends AbstractShowColumnVisitor {
        private ColumnGroup columnGroup;

        public ShowColumnInGroupVisitor(ColumnGroup columnGroup) {
            this.columnGroup = columnGroup;
        }

        protected boolean shouldShowColumn(HidableTableColumn h) {
            return columnGroup.equals(h.getColumnGroup());
        }
    }

    class ShowAllColumnsVisitor extends AbstractShowColumnVisitor {

        protected boolean shouldShowColumn(HidableTableColumn h) {
            return h.isHidden();
        }
    }

    class FindVisibleColumnVisitor extends AbstractFindColumnListVisitor<HidableTableColumn> {
        protected void doVisited(HidableTableColumn h) {
            if ( ! h.isHidden()) {
                getResult().add(h);
            }
        }
    }
    
    class FindHiddenColumnVisitor extends AbstractFindColumnListVisitor<HidableTableColumn> {
        protected void doVisited(HidableTableColumn h) {
            if ( h.isHidden()) {
                getResult().add(h);
            }
        }
    }

    class FindVisibleHeaderVisitor extends AbstractFindColumnListVisitor<Object> {

        protected void doVisited(HidableTableColumn h) {
           if ( ! h.isHidden()) {
                getResult().add(h.getHeaderValue());
            }
        }
    }

    abstract class AbstractFindColumnListVisitor<E> extends AbstractHidableColumnVisitor<List<E>> {

        protected AbstractFindColumnListVisitor() {
            setResult(new LinkedList<E>());
        }
    }

    abstract class AbstractFindColumnVistor extends AbstractHidableColumnVisitor<HidableTableColumn> {

        protected void doVisited(HidableTableColumn h) {
            if ( isColumnMatching(h) ) {
                setResult(h);
                setSuccess(true).setFinished(true);
            }
        }

        protected abstract boolean isColumnMatching(HidableTableColumn h);
    }

    abstract class AbstractHidableColumnVisitor<E> implements HidableTableColumn.HidableColumnVisitor<E> {
        private boolean success;
        private boolean finished;
        private int lastVisibleIndex = -1;
        private HidableTableColumn lastVisibleColumn;
        private E result;

        public final void visited(HidableTableColumn h) {
            doBeforeVisited(h);
            doVisited(h);
            doAfterVisited(h);
        }

        protected void doBeforeVisited(HidableTableColumn h) {}

        protected abstract void doVisited(HidableTableColumn h);

        protected void doAfterVisited(HidableTableColumn h) {
            if (! h.isHidden()) {
                lastVisibleIndex++;
                lastVisibleColumn = h;
            }
        }

        public boolean isFinished() {
            return finished;
        }

        public AbstractHidableColumnVisitor setFinished(boolean finished) {
            this.finished = finished;
            return this;
        }

        public boolean isSuccessful() {
            return success;
        }

        public AbstractHidableColumnVisitor setSuccess(boolean success) {
            this.success = success;
            return this;
        }

        protected int getLastVisibleIndex() {
            return lastVisibleIndex;
        }

        protected HidableTableColumn getLastVisibleColOrAnchor() {
            return lastVisibleColumn != null ? lastVisibleColumn : dummyAnchorColumn;
        }

        public E getResult() {
            return result;
        }

        public AbstractHidableColumnVisitor setResult(E result) {
            this.result = result;
            return this;
        }
    }
}
