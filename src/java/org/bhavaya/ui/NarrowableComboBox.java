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

package org.bhavaya.ui;

import org.bhavaya.util.ImageIconCache;
import org.bhavaya.util.StringRenderer;
import org.bhavaya.util.Utilities;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;


/**
 * A combo box that supports the concept of narrowing in on a set of data as the
 * user types.  The subset of options that match the user string are shown in
 * a scrollable popup box.  This class is used in conjunction with a narrowing
 * list model..
 *
 * @author Mr. Daniel "The Baron" Van Enckevort
 * @author Mr. Brendon "The Gui King" McLean
 * @see INarrowableListModel
 * @version $Revision: 1.34.4.3 $
 */
public class NarrowableComboBox extends JComponent implements EditableComponent {
    public static final String ITEM_CHOSEN_COMMAND = "item chosen";
    private static final int DISABLED_TEXT_LENGHT_LIMIT = -1;

    private INarrowableListModel model;
    protected JTextField textField;
    protected JButton button;
    private NarrowingJList list;
    private JScrollPane scrollPane;
    private PopupManager popupManager;
    private ListEventHandler listEventHandler;

    private Object chosenObject;
    private Object lastSelectedObject = null;
    private boolean chosenObjectDirty = true;

    private StringRenderer objectRenderer = StringRenderer.TO_STRING_RENDERER;

    private List<ActionListener> actionListeners = new ArrayList<ActionListener>();
    private List<ListSelectionListener> listSelectionListeners = new ArrayList<ListSelectionListener>();
    private ImageIcon busyIcon;

    private Boolean moveFocusAfterModelUpdate = null;   //flag to show that focus should move on after the model changes
    private boolean ignoreListEvents = false;
    private boolean ignoreTextEvents = false;
    private boolean ignoreTextFocusEvents = false;

    private boolean shortenToClosestMatch = true;
    private boolean freeFormatTextIsValid = false;

    private static Border normalBorder =  null;
    private static Border focusBorder =  BorderFactory.createLineBorder(new Color(200,160,0));

    public NarrowableComboBox(int columns, int maxVisibleRowCount) {
        this(columns, new NarrowableListModel(), maxVisibleRowCount);
    }

    public NarrowableComboBox(int columns, INarrowableListModel model, int maxVisibleRowCount, boolean shortenToClosestMatch, int maxLength) {
        super();


        this.shortenToClosestMatch = shortenToClosestMatch;
        this.model = model;
        textField = new JTextField(columns);

        if (maxLength != DISABLED_TEXT_LENGHT_LIMIT) {
            textField.setDocument(new LimitedLengthPlainDocument(maxLength));
        }
        if(normalBorder == null){
            normalBorder = textField.getBorder();
        }
        button = new JButton();
        button.setDisabledIcon(busyIcon);
        enableButton();

        list = new NarrowingJList(model, maxVisibleRowCount);
        scrollPane = new JScrollPane(list);
        listEventHandler = new ListEventHandler();
        model.addListDataListener(listEventHandler);

        configureComponents();
        popupManager = createPopupManager(textField, scrollPane);
        configureEventBindings();
        busyIcon = ImageIconCache.getImageIcon("hourglass.gif");

        this.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                 if(!ignoreTextFocusEvents){
                     textField.requestFocus();
                }
                ignoreTextFocusEvents = false;
            }
            @Override
            public void focusLost(FocusEvent e) {
            }
        });

        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                        textField.setBorder(focusBorder);
            }
            @Override
            public void focusLost(FocusEvent e) {
                textField.setBorder(normalBorder);
            }
        });
        textField.setFocusTraversalKeysEnabled(false);

        textField.addKeyListener(new KeyListener(){
            @Override
            public void keyTyped(KeyEvent e) {
            }
            @Override
            public void keyPressed(KeyEvent e) {
                 if(e.getKeyChar() == KeyEvent.VK_TAB){
                    ignoreTextFocusEvents = true;
                    if(e.isShiftDown()){
                        traverseBack();
                    }
                    else{
                        traverseForward();
                    }
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

    }

    private void traverseForward(){
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.focusNextComponent();
    }

    private void traverseBack(){
       this.transferFocusBackward();
       KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
       manager.focusPreviousComponent(this);
    }

    public NarrowableComboBox(int columns, INarrowableListModel model, int maxVisibleRowCount, boolean shortenToClosestMatch) {
        this(columns, model, maxVisibleRowCount, shortenToClosestMatch, DISABLED_TEXT_LENGHT_LIMIT);
    }

    public NarrowableComboBox(int columns, INarrowableListModel model, int maxVisibleRowCount) {
        this (columns, model, maxVisibleRowCount, true);
    }

    protected PopupManager createPopupManager(Component owner, JComponent popup) {
        return new PopupManager(owner, popup);
    }

    public void setModel(INarrowableListModel model) {
        if (this.model != null) this.removeListSelectionListener(listEventHandler);
        this.model = model;
        model.addListDataListener(listEventHandler);
        list.setModel(model);
    }

    public INarrowableListModel getModel() {
        return model;
    }

    public StringRenderer getRenderer() {
        return objectRenderer;
    }

    public void setRenderer(StringRenderer objectRenderer) {
        setRenderer(objectRenderer, true);
    }

    public boolean getFreeFormatTextIsValid() {
        return freeFormatTextIsValid;
    }

    public void setFreeFormatTextIsValid(boolean freeFormatTextIsValid) {
        if (shortenToClosestMatch && freeFormatTextIsValid) {
            throw new IllegalArgumentException("ShortenToClosetMatch must be false to set freeFormatTextIsValid to true.");
        }
        boolean oldValue = this.freeFormatTextIsValid;
        this.freeFormatTextIsValid = freeFormatTextIsValid;
        firePropertyChange("freeFormatTextIsValid", oldValue, this.freeFormatTextIsValid);
    }

    /**
     * Make sure you know what you're doing if you this method instead of the simpler version.  If you set
     * updateListRender to false, you will have to perform the rendering of the underlying object to strings
     * yourself.  Normally this is handled for you.
     *
     * @param objectRenderer takes an object and returns a string
     * @param updateListRenderer should be true.  If you set this to false, make sure you know what you're doing.
     */
    public void setRenderer(StringRenderer objectRenderer, boolean updateListRenderer) {
        this.objectRenderer = objectRenderer;
        model.setRenderer(objectRenderer);
        list.setCellRenderer(UIUtilities.createListCellRenderer(objectRenderer));
    }

    /**
     * @param listCellRenderer - Make sure you know what you're doing here.
     */
    public void setListCellRenderer(ListCellRenderer listCellRenderer) {
        list.setCellRenderer(listCellRenderer);
    }

    private boolean isTypedTextValid() {
        boolean textValid = textField.getText().equals(getChosenObjectText());
        if (!shortenToClosestMatch && freeFormatTextIsValid) {
            textValid = true;
        }
        return model.getSize() > 0 || textField.getText().length() == 0 || textValid;
    }

    protected void configureComponents() {
        final GridBagConstraints c = new GridBagConstraints();
        GridBagLayout layout = new GridBagLayout(){
            {defaultConstraints = c;}
        };
        setLayout(layout);
        c.gridwidth=3;
        c.gridheight=1;
        c.fill=GridBagConstraints.BOTH;
        c.ipadx=1;
        c.ipady=1;
        c.weightx=1.0;
        c.weighty=1.0;

        textField.setFocusTraversalKeysEnabled(false);
        add(textField, c);

        c.weightx=0;
        button.setPreferredSize(new Dimension(20, 20));
        button.setFocusable(false);
        add(button, c);

        list.setFocusable(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setFocusable(false);
        scrollPane.getVerticalScrollBar().setFocusable(false);
        scrollPane.setDoubleBuffered(true);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.black));
    }

    protected void configureEventBindings() {
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            public void ancestorMoved(HierarchyEvent e) {
                hidePopup();
            }
        });
        textField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                if (!e.isTemporary() && isEditable()) {
                    ensureValueSelected(null);
                    enableButton();
                }
            }

            public void focusGained(FocusEvent e) {
                if (chosenObject == null && model.getSize() > 1 && chosenObjectDirty) narrowAndPop();
            }
        });
        textField.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                // Combobox can be made unfocusable, because as preselected chosenObject was set,
                // i.e. so when the user presses tab from a previous component it tabs over the combobox.
                // However the user needs to be able to explicity override the preselected chosenObject
                // currently this is indicated by the user explicity clicking on the combobox.
                // There is probably a better way of doing this using FocusTraversalPolicies.
                setFocusable(true);
                requestFocusInWindow();
                if (model.getSize() > 1 && chosenObjectDirty) narrowAndPop();
            }
        });
        textField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent evt) {
                if (!ignoreTextEvents) narrow();
            }

            public void removeUpdate(DocumentEvent e) {
                if (!ignoreTextEvents) {
                    narrow();
                    if (textField.getText().length() == 0) {
                        list.clearSelection();
                    }
                }
            }

            public void changedUpdate(DocumentEvent e) {
                if (!ignoreTextEvents) narrow();
            }
        });
        textField.addKeyListener(new TextFieldKeyHandler());
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setChosenObject(model.getElementAt(list.locationToIndex(e.getPoint())));
            }
        });
        list.addListSelectionListener(listEventHandler);
        button.addActionListener(getButtonActionListener());
    }

    /**
     * @param focusForward
     * @return true if a value was unambigusouly chosen, false if the user should be allowed more interaction
     */
    public boolean ensureValueSelected(Boolean focusForward) {
        // If the object has been chosen and no modifications have occurred, then no action required.
        if (!chosenObjectDirty) {
            processFocus(focusForward);
            return true;
        }

        if (!popupManager.isShowing() || getModel().isLoadingData()) {
            if (textField.getText().length() == 0) {
                setChosenObjectAndMoveFocus(null, focusForward);
                return true;
            } else {
                return tryToSetChosenObjectFromText(focusForward);
            }
        } else {
            Object chosenObject = null;
            if (list.getSelectedValue() != null) { //value in list is highlighted
                chosenObject = getSelectedValue();
            }
            setChosenObjectAndMoveFocus(chosenObject, focusForward);
            return true;
        }
    }

    private void showPopup() {
        if (!isEditable()) return;
        // if the textfield was not focusable before, the fact the user has clicked on the button or textfield
        // with the mouse, indicates it should be focusable
        textField.setFocusable(true);
        textField.requestFocusInWindow();
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (model.getSize() > 0) {
                    if (textField.getText().length() == 0) list.clearSelection();
                    popupManager.show();
                }
            }
        });
    }

    protected void hidePopup() {
        popupManager.hide();
    }

    private void narrow() {
        chosenObjectDirty = true;
        disableButton();
        model.narrow(textField.getText());
        //the button icon is reset by the ListEventHandler.contentsChanged method
    }

    protected void enableButton() {
        button.setText("...");
        button.setIcon(null);
    }

    protected void disableButton() {
        button.setText("");
        button.setIcon(busyIcon);
    }

    /*
     * Horribly inefficient, but a simple and trustworthy recursive search
     */
    private String findClosestMatch(String narrowText) {
        if (narrowText.length() != 0) {
            model.narrow(narrowText, false);

            // If there is no luck, trim string by 1 char and recurse
            if (model.getSize() == 0) {
                return findClosestMatch(narrowText.substring(0, narrowText.length() - 1));
            } else {
                return narrowText;
            }
        }
        return null;
    }

    public void setFocusable(boolean focusable) {
        if (textField == null) super.setFocusable(focusable);
        else textField.setFocusable(focusable);
    }

//    public boolean requestFocusInWindow() {
//        if (textField == null) return super.requestFocusInWindow();
//        return textField.requestFocusInWindow();
//    }

//    public void requestFocus() {
//        if (textField == null) super.requestFocus();
//        else textField.requestFocus();
//    }

//    public boolean requestFocus(boolean temporary) {
//        if (textField == null) return super.requestFocus(temporary);
//        return textField.requestFocus(temporary);
//    }

//    public void transferFocus() {
//        if (textField == null) textField.transferFocus();
//        else textField.transferFocus();
//    }

//    public void addFocusListener(FocusListener l) {
//        if (textField == null) {
//            super.addFocusListener(l);
//        } else {
//            textField.addFocusListener(l);
//        }
//    }

    public boolean isFocusable() {
        //return true;
        if (textField == null) return super.isFocusable();
        return textField.isFocusable();
    }

    public boolean isFocusOwner() {  
        if (textField == null) return super.isFocusOwner();
        return textField.isFocusOwner();
    }

//    public void removeFocusListener(FocusListener l) {
//        if (textField == null) textField.removeFocusListener(l);
//        else textField.removeFocusListener(l);
//    }

    public void addListSelectionListener(ListSelectionListener l) {
        listSelectionListeners.add(l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        listSelectionListeners.remove(l);
    }

    /**
     * the combo box fires action events when someone has selected an item from the list.
     */
    public void addActionListener(ActionListener l) {
        actionListeners.add(l);
    }

    public void removeActionListener(ActionListener l) {
        actionListeners.remove(l);
    }

    public Object getSelectedValue() {
        return chosenObjectDirty ? (model.getSize() == 0 ? chosenObject : list.getSelectedValue()) : chosenObject;
    }

    public void setChosenObject(final Object chosenObject) {
        hidePopup();
        Object oldChosenObject = this.chosenObject;
        String oldChosenObjectText = oldChosenObject != null ? objectRenderer.render(oldChosenObject) : "";
        this.chosenObject = chosenObject;
        String chosenObjText = getChosenOrTypedText();
        boolean textChanged = false;
        if (!Utilities.equals(oldChosenObjectText, chosenObjText)) {
            ignoreTextEvents = true;
            ignoreListEvents = true;
            textField.setText(chosenObjText);
            textField.setForeground(isTypedTextValid() ? Color.black : Color.red);
            textField.setCaretPosition(chosenObjText.length());
            chosenObjectDirty = false;
            ignoreListEvents = false;
            ignoreTextEvents = false;
            textChanged = true;
        }
        if (!Utilities.equals(this.chosenObject, oldChosenObject)|| textChanged) fireObjectChosen();
    }

    private String getChosenObjectText() {
        return chosenObject != null ? objectRenderer.render(chosenObject) : "";
    }

    private String getChosenOrTypedText() {
        String text = chosenObject != null ? objectRenderer.render(chosenObject) : "";
        if (chosenObject == null && !shortenToClosestMatch && freeFormatTextIsValid) {
            text = getTypedText();
        }
        return text;
    }

    protected void setChosenObjectAndMoveFocus(final Object chosenObject, Boolean focusForward) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                setChosenObject(chosenObject);
            }
        });
        processFocus(focusForward);
    }

    private void fireObjectChosen() {
        ActionEvent e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, ITEM_CHOSEN_COMMAND);
        for (ActionListener listener : actionListeners) {
            listener.actionPerformed(e);
        }
    }

    /**
     * Returns the object that the user has chosen using the combo box.
     * This will be null if the user has not chosen anything yet.
     * Likewise it will be cleared only when user leaves the combo box or presses enter (can happen that this is
     * still set to something when the combo box is cleared).
     * For info about current selection use {@link #getSelectedValue()}
     */
    public Object getChosenObject() {
        return chosenObject;
    }

    public void setEnabled(boolean enabled) {
        textField.setEnabled(enabled);
        button.setEnabled(enabled);
        list.setEnabled(enabled);
        scrollPane.setEnabled(enabled);
    }

    public void setEditable(boolean editable) {
        textField.setEditable(editable);
        button.setEnabled(editable);
        list.setEnabled(editable);
        scrollPane.setEnabled(editable);
    }

    public boolean isEditable() {
        return textField.isEditable();
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        if (!isEditable()) return false;
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    /**
     * used to set the combo box to a specific value.
     * if the typed text corresponds to a list entry, we will fire a "chosen" event
     */
    public void setTypedText(final String typedText) {
        if (getSelectedValue() != null && getSelectedValue().toString().equalsIgnoreCase(typedText)) {
            setChosenObject(getSelectedValue());
        } else if (typedText == null || typedText.length() == 0) {
            setChosenObject(null);
        } else {
            UIUtilities.runInDispatchThread(new Runnable() {
                public void run() {
                    textField.setText(typedText);
                }
            });
           if (model.getSize() == 1) setChosenObject(model.getFirstMatchingObject(typedText));
        }
    }

    public String getTypedText() {
        return textField.getText();
    }

    /**
     * @param traverseFocusForwardOnSuccess direction to move focus if the chosen object can be unambiguously set.
     *          True moves focus forward, False moves backward, null does not move focus
     * @return if the attempt was successful
     */
    private boolean tryToSetChosenObjectFromText(Boolean traverseFocusForwardOnSuccess) {
        String currentText = getTypedText();
        Object closestMatchingObject;
        if (getModel().isLoadingData()) {
            moveFocusAfterModelUpdate = traverseFocusForwardOnSuccess;
            return false;
        } else {
            ignoreListEvents = true;
            String closestMatchingString = findClosestMatch(currentText);
            String firstMatchingObjectString = model.getSize() > 0 ? objectRenderer.render(model.getElementAt(0)) : "";
            ignoreListEvents = false;

            if (firstMatchingObjectString.equalsIgnoreCase(currentText) || (model.getSize() == 1 && closestMatchingString.length() == currentText.length())) {
                closestMatchingObject = model.getElementAt(list.getSelectedIndex()); // If list has elements with the same text string you may not have picked index 0
                if (traverseFocusForwardOnSuccess != null) {
                    setChosenObjectAndMoveFocus(closestMatchingObject, traverseFocusForwardOnSuccess);
                } else {
                    setChosenObject(closestMatchingObject);
                }
                return true;
            } else {
                setChosenObject(null);
                if (shortenToClosestMatch) {
                    textField.setText(closestMatchingString);
                } else {
                    textField.setText(currentText);
                }
                if (!shortenToClosestMatch && freeFormatTextIsValid) {
                    processFocus(traverseFocusForwardOnSuccess);
                }
                return false;
            }
        }
    }

    private void processFocus(Boolean traverseFocus) {
        if (traverseFocus != null) {
            if (traverseFocus) {
                textField.transferFocus();
            } else {
                textField.transferFocusBackward();
            }
        }
    }

    private class ListEventHandler implements ListDataListener, ListSelectionListener {
        public void intervalAdded(ListDataEvent e) {
        }

        public void intervalRemoved(ListDataEvent e) {
        }

        public void contentsChanged(ListDataEvent e) {
            if (ignoreListEvents) return;

            chosenObjectDirty = true;

            list.dataChanged();

            if (moveFocusAfterModelUpdate != null) {
                Boolean moveFocus = moveFocusAfterModelUpdate;
                moveFocusAfterModelUpdate = null;
                tryToSetChosenObjectFromText(moveFocus);
            } else {
                if (shouldShowPopup()) {
                    showPopup();
                } else {
                    hidePopup();
                }
            }
            enableButton();
            updateSelectionListeners();
            textField.setForeground(isTypedTextValid() ? Color.black : Color.red);
        }

        private boolean shouldShowPopup() {
            return textField.isFocusOwner()
                    && model.getSize() != 0
                    && (model.getSize() > 1
                    || (getTypedText().length() != objectRenderer.render(model.getElementAt(0)).length()));
        }

        public void valueChanged(ListSelectionEvent e) {
            updateSelectionListeners();
        }

        private void updateSelectionListeners() {
            // We do this because the normal list behaviour is not send an event if the selected list index is the same
            // In our case, the index remains 0 while the data itself changes.  We send this out as a selection event.
            if (getSelectedValue() != lastSelectedObject) {
                ListSelectionEvent event = new ListSelectionEvent(list, 0, 0, false);
                for (ListSelectionListener listener : listSelectionListeners) {
                    listener.valueChanged(event);
                }
            }
            lastSelectedObject = getSelectedValue();
        }
    }

    private class NarrowingJList extends JList {
        private int maxVisibleRowCount;

        public NarrowingJList(ListModel listModel, int maxVisibleRowCount) {
            super(listModel);
            this.maxVisibleRowCount = maxVisibleRowCount;
            setVisibleRowCount(maxVisibleRowCount);

            // All data events should result in the first list item being selected
            setModel(listModel);

            // A filthy hack of the worst magnitude, but necessary to stop the UI delegates from scanning the entire
            // model to out work cell widths
            setFixedCellHeight(1);
            setFixedCellWidth(1);
        }

        public void setCellRenderer(ListCellRenderer cellRenderer) {
            super.setCellRenderer(cellRenderer);
        }

        public void incrementIndex(int inc) {
            int modelSize = getModel().getSize();
            if (modelSize > 0) {
                int index = getSelectedIndex();
                index = Math.min(index + inc, modelSize - 1);

                setSelectedIndex(index);
                ensureIndexIsVisible(index);
            }
        }

        public void decrementIndex(int dec) {
            int modelSize = getModel().getSize();
            if (modelSize > 0) {
                int index = getSelectedIndex();
                index = Math.max(index - dec, 0);

                setSelectedIndex(index);
                ensureIndexIsVisible(index);
            }
        }

        public void dataChanged() {
            ListModel listModel = getModel();
            int size = listModel.getSize();
            if (size > 0) {
                int newSelectedIndex = 0;

                String firstObjectRendering = getRenderer().render(listModel.getElementAt(0));
                String lastObjectRendering = getRenderer().render(listModel.getElementAt(size - 1));
                if (firstObjectRendering.equals(lastObjectRendering)) {
                    // if everything in the list is the same, then it could be that the user has chosen a value.
                    // (if there are different values in the list, then the user cannot have made a selection yet)
                    // It is possible that there are other values with the same rendering as the chosen object,
                    // we need to ensure we select the correct one.
                    for (int i = 0; i < size; i++) {
                        if (listModel.getElementAt(i).equals(chosenObject)) {
                            newSelectedIndex = i;
                            break;
                        }
                    }
                }

                setSelectedIndex(newSelectedIndex);
                ensureIndexIsVisible(newSelectedIndex);
                setVisibleRowCount(Math.min(size, maxVisibleRowCount));
                invalidate();
            } else {
                clearSelection();
            }
        }

        public Dimension getPreferredScrollableViewportSize() {
            int modelSize = getModel().getSize();
            boolean usingScrollBars = modelSize > maxVisibleRowCount;

            int scrollbarWidth = usingScrollBars ? scrollPane.getVerticalScrollBar().getPreferredSize().width : 0;

            Insets scrollpaneInsets = scrollPane.getInsets();
            int cellWidth = textField.getWidth() - scrollbarWidth - scrollpaneInsets.left - scrollpaneInsets.right;
            int cellHeight = getCellRenderer().getListCellRendererComponent(this, getModel().getElementAt(0), 0, false, false).getPreferredSize().height;
            setFixedCellWidth(cellWidth);
            setFixedCellHeight(cellHeight);

            return new Dimension(cellWidth, cellHeight * getVisibleRowCount());
        }
    }

    /**
     * Class manages the popup list that sits below the textfield.
     */
    protected static class PopupManager {
        private Popup popup;

        private Component owner;
        private Component contents;

        private PopupManager(Component owner, Component contents) {
            this.owner = owner;
            this.contents = contents;
        }

        private void show() {
            // Only popup if owner is focused.
            if (owner.isFocusOwner() && owner.isEnabled() && owner.isShowing()) {
                Popup oldPopup = popup;

                // We always need to recreate the popup because we can't just change its size.
                popup = PopupFactory.getSharedInstance().getPopup(owner, contents, owner.getLocationOnScreen().x,
                        owner.getLocationOnScreen().y + owner.getHeight());
                popup.show();

                // Now hide the old one. (Hide after show avoids flickering.
                if (oldPopup != null) {
                    oldPopup.hide();
                }
            }
        }

        private void hide() {
            if (popup != null) {
                popup.hide();
                popup = null;
            }
        }

        private boolean isShowing() {
            return popup != null;
        }
    }

    private void narrowAndPop() {
        if (!popupManager.isShowing()) {
            narrow();
            popupManager.show();
        }
    }
    private class TextFieldKeyHandler extends KeyAdapter {
        public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_DOWN:
                    e.consume();

                    if (popupManager.isShowing()) list.incrementIndex(1);
                    if (model.getSize() > 0) narrowAndPop();
                    break;
                case KeyEvent.VK_UP:
                    e.consume();

                    if (popupManager.isShowing()) list.decrementIndex(1);
                    if (model.getSize() > 0) narrowAndPop();
                    break;
                case KeyEvent.VK_PAGE_DOWN:
                    e.consume();

                    if (popupManager.isShowing()) list.incrementIndex(list.getVisibleRowCount());
                    if (model.getSize() > 0) narrowAndPop();
                    break;
                case KeyEvent.VK_PAGE_UP:
                    e.consume();

                    if (popupManager.isShowing()) list.decrementIndex(list.getVisibleRowCount());
                    if (model.getSize() > 0) narrowAndPop();
                    break;
                case (KeyEvent.VK_ENTER):
                case (KeyEvent.VK_TAB):
                    if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK) break;
                    e.consume();
                    if (e.getID() == KeyEvent.KEY_PRESSED) {
                        boolean focusForward = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != KeyEvent.SHIFT_DOWN_MASK;
                        ensureValueSelected(focusForward);
                    }
                    break;
                case KeyEvent.VK_BACK_SPACE:
                    // CTRL-Backspace support
                    if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK) {
                        e.consume();
                        String typedText = textField.getText();
                        int endTextCrop = textField.getCaretPosition();
                        int startTextCrop = Math.max(0, typedText.lastIndexOf(' '));
                        textField.setText(Utilities.cutTextFromString(typedText, startTextCrop, endTextCrop));
                        textField.setCaretPosition(startTextCrop);
                    }
                    break;
                case KeyEvent.VK_DELETE:
                    // CTRL-Delete support
                    if ((e.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) == KeyEvent.CTRL_DOWN_MASK) {
                        e.consume();
                        String typedText = textField.getText();
                        int startCrop = textField.getCaretPosition();
                        int endTextCrop = typedText.length() - Math.max(Utilities.lastIndexFromEndOf(typedText.substring(startCrop), ' ') - 1, 0);
                        textField.setText(Utilities.cutTextFromString(typedText, startCrop, endTextCrop));
                        textField.setCaretPosition(startCrop);
                    }
                    break;
                case (KeyEvent.VK_ESCAPE):
                    if (popupManager.isShowing()) e.consume();
                    setChosenObject(chosenObject);
                    break;
            }
        }
    }

    protected ActionListener getButtonActionListener() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!popupManager.isShowing()) {
                    narrowAndPop();
                } else {
                    hidePopup();
                }
            }
        };
    }
}