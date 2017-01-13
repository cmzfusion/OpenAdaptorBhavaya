package org.bhavaya.ui.table.formula;

import org.bhavaya.ui.table.PathPropertyChangeEvent;

import java.util.Collections;

/**
 * Subclass of PathPropertyChangeEvent that specifically relates to formula changes
 * User: Jon Moore
 * Date: 26/01/11
 * Time: 16:24
 */
public class FormulaPropertyChangeEvent extends PathPropertyChangeEvent {

    public FormulaPropertyChangeEvent(Object source, String path, Object oldValue, Object newValue) {
        super(Collections.singleton(source), source, new String[] {path}, oldValue);
        setNewValue(newValue);
    }
}
