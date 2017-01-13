package org.bhavaya.javafxui;

import javafx.animation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.*;
import javafx.util.Duration;
import org.bhavaya.ui.FlashingTabButton;

import java.awt.KeyboardFocusManager;
import java.awt.KeyEventPostProcessor;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ga2mop0
 * Date: 29/07/13
 * Time: 17:08
 */
public class JavaFxTabButton extends StackPane implements FlashingTabButton {

    private static final int WIDTH_PADDING = 8;
    private static final int HEIGHT_PADDING = 4;

    private javafx.scene.paint.Paint originalPaint = null;
    private FillTransition fillTransition = null;
    private javafx.scene.shape.Rectangle rectangle;
    private HBox textLabelHbox;
    private final boolean toggleButton;
    private boolean selected = false;
    private boolean armed = false;
    private boolean over = false;

    private Tooltip tooltip;
    private KeyCode mnemonic;

    private static KeyEventPostProcessorImpl keyEventProcessor;

    static {
        //todo - I'm not sure this is the best way of doing this - seems wrong mixing awt and javafx
        keyEventProcessor = new KeyEventPostProcessorImpl();
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(keyEventProcessor);
    }

    private ChangeListener<Number> sizeListener = new ChangeListener<Number>() {
        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number number, Number number2) {
            ObservableList<Node> labels = textLabelHbox.getChildren();
            if(!labels.isEmpty()) {
                int width = WIDTH_PADDING;
                for(Node label : labels) {
                    width += ((Label)label).getWidth();
                }
                double height = ((Label)labels.get(0)).getHeight()+HEIGHT_PADDING;
                rectangle.setX(-width/2);
                rectangle.setY(-height/2);
                rectangle.setWidth(width);
                rectangle.setHeight(height);
            }
        }
    };

    public JavaFxTabButton(String text, int mnemonic, String tooltip) {
        this(text, mnemonic, tooltip, false);
    }

    public JavaFxTabButton(String text, int mnemonic, String tooltip, boolean toggleButton) {
        this.toggleButton = toggleButton;
        this.tooltip = new Tooltip(tooltip);
        this.mnemonic = KeyCode.getKeyCode(""+(char)mnemonic);
        keyEventProcessor.addMnemonicMapping(mnemonic, this);
        rectangle = new javafx.scene.shape.Rectangle();

        getChildren().addAll(rectangle, createLabel(text));

        initFillTransition();
        setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                setOver(true);
                if(isPressed()) {
                    setArmed(true);
                }
                repaint();
            }
        });

        setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                setOver(false);
                if(isPressed()) {
                    setArmed(false);
                }
                repaint();
            }
        });

        setOnMouseReleased(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                setPressed(false);
                setArmed(false);
                toggleSelected();
                repaint();
                fireAction();
            }
        });

        setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                setPressed(true);
                setArmed(true);
                repaint();
            }
        });

        repaint();
    }

    private void repaint() {
        String idAdapter = "";
        if(selected || armed) {
            idAdapter = "Pressed";
        } else if (over) {
            idAdapter = "Hover";
        }
        rectangle.setId("tabButton"+idAdapter);
        ObservableList<Node> labels = textLabelHbox.getChildren();
        if(!labels.isEmpty()) {
            for(Node label : labels) {
                label.setId("tabButtonText" + idAdapter);
            }
        }
    }

    private void fireAction() {
        EventHandler<ActionEvent> handler = onAction.get();
        if(handler != null) {
            handler.handle(new ActionEvent(this, this));
        }
    }

    public final ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return onAction;
    }

    public final void setOnAction(EventHandler<ActionEvent> value) {
        onActionProperty().set(value);
    }

    public final EventHandler<ActionEvent> getOnAction() {
        return onActionProperty().get();
    }

    private ObjectProperty<EventHandler<ActionEvent>> onAction = new ObjectPropertyBase<EventHandler<ActionEvent>>() {
        @Override protected void invalidated() {
            setEventHandler(ActionEvent.ACTION, get());
        }

        @Override
        public Object getBean() {
            return JavaFxTabButton.this;
        }

        @Override
        public String getName() {
            return "onAction";
        }
    };

    private void initFillTransition() {
        fillTransition = FillTransitionBuilder.create()
                .duration(Duration.millis(500))
                .shape(rectangle)
                .cycleCount(Timeline.INDEFINITE)
                .autoReverse(true)
                .build();
    }

    private Node createLabel(String text) {
        textLabelHbox = new HBox();
        textLabelHbox.setAlignment(Pos.CENTER);
        setText(text);
        return textLabelHbox;
    }

    public void setText(String text) {
        Collection<javafx.scene.control.Label> labels = new ArrayList<>();
        String mnemonicText = mnemonic == null ? "" : mnemonic.getName();
        int index = mnemonicText.length() == 1 ? text.toLowerCase().indexOf(mnemonicText.toLowerCase()) : -1;
        if(index >= 0) {
            if(index == 0) {
                labels.add(createLabel(text.substring(0, 1), true));
                labels.add(createLabel(text.substring(1), false));
            } else if (index == text.length()-1) {
                labels.add(createLabel(text.substring(0, index-1), false));
                labels.add(createLabel(text.substring(index), true));
            } else {
                labels.add(createLabel(text.substring(0, index), false));
                labels.add(createLabel(text.substring(index, index+1), true));
                labels.add(createLabel(text.substring(index+1), false));
            }
        } else {
            labels.add(createLabel(text, false));
        }
        textLabelHbox.getChildren().clear();
        textLabelHbox.getChildren().addAll(labels);
    }

    private javafx.scene.control.Label createLabel(String text, boolean underline) {
        javafx.scene.control.Label label = new javafx.scene.control.Label(text);
        label.setUnderline(underline);
        label.setTooltip(tooltip);
        label.widthProperty().addListener(sizeListener);
        label.heightProperty().addListener(sizeListener);

        return label;
    }


    public void startFlashing(final java.awt.Color color) {
        JavaFxUtilities.runLater(new Runnable() {
            @Override
            public void run() {
                fillTransition.stop();
                if(originalPaint == null) {
                    originalPaint = rectangle.getFill();
                    fillTransition.setFromValue(getColor(originalPaint));
                }
                fillTransition.setToValue(javafx.scene.paint.Color.rgb(color.getRed(), color.getGreen(), color.getBlue()));
                fillTransition.play();
            }
        });
    }

    public void stopFlashing() {
        JavaFxUtilities.runLater(new Runnable() {
            @Override
            public void run() {
                fillTransition.stop();
                if(originalPaint != null) {
                    rectangle.setFill(originalPaint);
                }
                repaint();
            }
        });
    }

    @Override
    public void setButtonVisible(final boolean show) {
        JavaFxUtilities.runOnJavaFxThread(new Runnable() {
            @Override
            public void run() {
                setVisible(show);
            }
        });
    }

    private javafx.scene.paint.Color getColor(javafx.scene.paint.Paint paint) {
        javafx.scene.paint.Color color = null;
        if(paint instanceof javafx.scene.paint.Color) {
            color = (javafx.scene.paint.Color)paint;
        } else if (paint instanceof LinearGradient) {
            LinearGradient lg = (LinearGradient)paint;
            java.util.List<Stop> stops = lg.getStops();
            if(stops.size() > 0) {
                color = stops.get(0).getColor();
            }
        }
        return color == null ? javafx.scene.paint.Color.GRAY : color;
    }

    public boolean isToggleButton() {
        return toggleButton;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if(toggleButton) {
            this.selected = selected;
            repaint();
        }
    }

    public void toggleSelected() {
        setSelected(!selected);
    }

    public boolean isArmed() {
        return armed;
    }

    public void setArmed(boolean armed) {
        this.armed = armed;
    }

    public boolean isOver() {
        return over;
    }

    public void setOver(boolean over) {
        this.over = over;
    }

    private static class KeyEventPostProcessorImpl implements KeyEventPostProcessor {
        Map<Integer, JavaFxTabButton> mnemonicMap = new HashMap<>();

        public void addMnemonicMapping(int mnemonic, JavaFxTabButton tabButton) {
            mnemonicMap.put(mnemonic, tabButton);
        }

        public boolean postProcessKeyEvent(java.awt.event.KeyEvent e) {
            return e.getID() == java.awt.event.KeyEvent.KEY_PRESSED &&
                    e.getModifiers() == java.awt.event.KeyEvent.ALT_MASK &&
                    processHotkey(e);
        }

        /**
         * Processes hotkey press e.g. Alt + <mnemonics>
         */
        private boolean processHotkey(java.awt.event.KeyEvent e) {
            final JavaFxTabButton tabButton = mnemonicMap.get(e.getKeyCode());
            if (tabButton != null) {
                JavaFxUtilities.runLater(new Runnable() {
                    @Override
                    public void run() {
                        tabButton.fireAction();
                    }
                });
                return true;
            } else {
                return false;
            }
        }
    }
}
