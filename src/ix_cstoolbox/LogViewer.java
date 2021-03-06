/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ix_cstoolbox;

/**
 *
 * @author bho
 */

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.scene.control.*;
import javafx.util.Duration;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

class Log {
    private static final int MAX_LOG_ENTRIES = 2_000_000_000;
    //private final List<LogRecord> copyLog = new ArrayList<>();
    private final BlockingDeque<LogRecord> log = new LinkedBlockingDeque<>(MAX_LOG_ENTRIES);

    public void drainTo(Collection<? super LogRecord> collection) {
        //copyTo(copyLog);
        log.drainTo(collection);
    }

    /*public void drainCopyTo(Collection<? super LogRecord> collection) {
        collection.addAll(copyLog);
    }*/
    public void copyTo(Collection<? super LogRecord> collection){
        collection.addAll(log);
    }
    
    public void offer(LogRecord record) {
        log.offer(record);
    }
}

class Logger implements ILogger{
    private final Log log;
    private final String context;

    public Logger(Log log, String context) {
        this.log = log;
        this.context = context;
    }

    public void log(LogRecord record) {
        log.offer(record);
    }

    public void debug(String msg) {
        log(new LogRecord(Level.DEBUG, context, msg));
    }

    public void info(String msg) {
        log(new LogRecord(Level.INFO, context, msg));
    }

    public void warn(String msg) {
        log(new LogRecord(Level.WARN, context, msg));
    }

    public void error(String msg) {
        log(new LogRecord(Level.ERROR, context, msg));
    }

    public Log getLog() {
        return log;
    }
}

enum Level { DEBUG, INFO, WARN, ERROR }

class LogRecord {
    private final Date   timestamp;
    private final Level  level;
    private final String context;
    private final String message;

    public LogRecord(Level level, String context, String message) {
        this.timestamp = new Date();
        this.level     = level;
        this.context   = context;
        this.message   = message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getContext() {
        return context;
    }

    public String getMessage() {
        return message;
    }
}

class LogView extends ListView<LogRecord> {
    private static final int MAX_ENTRIES = 10_000_000_00;

    private final static PseudoClass debug = PseudoClass.getPseudoClass("debug");
    private final static PseudoClass info  = PseudoClass.getPseudoClass("info");
    private final static PseudoClass warn  = PseudoClass.getPseudoClass("warn");
    private final static PseudoClass error = PseudoClass.getPseudoClass("error");

    private final static SimpleDateFormat timestampFormatter = new SimpleDateFormat("HH:mm:ss.SSS");

    private final BooleanProperty       showTimestamp = new SimpleBooleanProperty(false);
    private final ObjectProperty<Level> filterLevel   = new SimpleObjectProperty<>(null);
    private final BooleanProperty       tail          = new SimpleBooleanProperty(false);
    private final BooleanProperty       paused        = new SimpleBooleanProperty(false);
    private final DoubleProperty        refreshRate   = new SimpleDoubleProperty(60);

    private final ObservableList<LogRecord> logItems = FXCollections.observableArrayList();

    public BooleanProperty showTimeStampProperty() {
        return showTimestamp;
    }

    public ObjectProperty<Level> filterLevelProperty() {
        return filterLevel;
    }

    public BooleanProperty tailProperty() {
        return tail;
    }

    public BooleanProperty pausedProperty() {
        return paused;
    }

    public DoubleProperty refreshRateProperty() {
        return refreshRate;
    }

    public LogView(Logger logger) {
        getStyleClass().add("log-view");

        Timeline logTransfer = new Timeline(
                new KeyFrame(
                        Duration.seconds(1),
                        event -> {
                            logger.getLog().drainTo(logItems);
                            

                            if (logItems.size() > MAX_ENTRIES) {
                                logItems.remove(0, logItems.size() - MAX_ENTRIES);
                            }

                            if (tail.get()) {
                                scrollTo(logItems.size());
                            }
                        }
                )
        );
        logTransfer.setCycleCount(Timeline.INDEFINITE);
        logTransfer.rateProperty().bind(refreshRateProperty());

        this.pausedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && logTransfer.getStatus() == Animation.Status.RUNNING) {
                logTransfer.pause();
            }

            if (!newValue && logTransfer.getStatus() == Animation.Status.PAUSED && getParent() != null) {
                logTransfer.play();
            }
        });

        this.parentProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                logTransfer.pause();
            } else {
                if (!paused.get()) {
                    logTransfer.play();
                }
            }
        });

        filterLevel.addListener((observable, oldValue, newValue) -> {
            setItems(
                    new FilteredList<LogRecord>(
                            logItems,
                            logRecord ->
                                logRecord.getLevel().ordinal() >=
                                filterLevel.get().ordinal()
                    )
            );
        });
        filterLevel.set(Level.DEBUG);

        setCellFactory(param -> new ListCell<LogRecord>() {
            {
                showTimestamp.addListener(observable -> updateItem(this.getItem(), this.isEmpty()));
            }

            @Override
            protected void updateItem(LogRecord item, boolean empty) {
                super.updateItem(item, empty);

                pseudoClassStateChanged(debug, false);
                pseudoClassStateChanged(info, false);
                pseudoClassStateChanged(warn, false);
                pseudoClassStateChanged(error, false);

                if (item == null || empty) {
                    setText(null);
                    return;
                }

                String context =
                        (item.getContext() == null)
                                ? ""
                                : item.getContext() + " ";

                if (showTimestamp.get()) {
                    String timestamp =
                            (item.getTimestamp() == null)
                                    ? ""
                                    : timestampFormatter.format(item.getTimestamp()) + " ";
                    setText(timestamp + context + item.getMessage());
                } else {
                    setText(context + item.getMessage());
                }

                switch (item.getLevel()) {
                    case DEBUG:
                        pseudoClassStateChanged(debug, true);
                        break;

                    case INFO:
                        pseudoClassStateChanged(info, true);
                        break;

                    case WARN:
                        pseudoClassStateChanged(warn, true);
                        break;

                    case ERROR:
                        pseudoClassStateChanged(error, true);
                        break;
                }
            }
        });
    }
}

class Lorem {
    private static final String[] IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque hendrerit imperdiet mi quis convallis. Pellentesque fringilla imperdiet libero, quis hendrerit lacus mollis et. Maecenas porttitor id urna id mollis. Suspendisse potenti. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Cras lacus tellus, semper hendrerit arcu quis, auctor suscipit ipsum. Vestibulum venenatis ante et nulla commodo, ac ultricies purus fringilla. Aliquam lectus urna, commodo eu quam a, dapibus bibendum nisl. Aliquam blandit a nibh tincidunt aliquam. In tellus lorem, rhoncus eu magna id, ullamcorper dictum tellus. Curabitur luctus, justo a sodales gravida, purus sem iaculis est, eu ornare turpis urna vitae dolor. Nulla facilisi. Proin mattis dignissim diam, id pellentesque sem bibendum sed. Donec venenatis dolor neque, ut luctus odio elementum eget. Nunc sed orci ligula. Aliquam erat volutpat.".split(" ");
    private static final int MSG_WORDS = 8;
    private int idx = 0;

    private final Random random = new Random(42);

    synchronized public String nextString() {
        int end = Math.min(idx + MSG_WORDS, IPSUM.length);

        StringBuilder result = new StringBuilder();
        for (int i = idx; i < end; i++) {
            result.append(IPSUM[i]).append(" ");
        }

        idx += MSG_WORDS;
        idx = idx % IPSUM.length;

        return result.toString();
    }

    synchronized public Level nextLevel() {
        double v = random.nextDouble();

        if (v < 0.8) {
            return Level.DEBUG;
        }

        if (v < 0.95) {
            return Level.INFO;
        }

        if (v < 0.985) {
            return Level.WARN;
        }

        return Level.ERROR;
    }
}