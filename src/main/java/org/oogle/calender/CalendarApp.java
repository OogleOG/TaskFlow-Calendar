package org.oogle.calender;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class CalendarApp extends Application {

    private final Map<LocalDate, List<Event>> eventsMap = new HashMap<>();
    private Timer timer;
    private GridPane calendarGrid;
    private Label monthYearLabel;
    private YearMonth currentYearMonth;
    private ListView<String> eventDetailsView;
    private LocalDate selectedDate;
    private Label sidebarTitle;
    private Stage primaryStage;
    private TrayIcon trayIcon;

    // Color scheme
    private static final String PRIMARY_COLOR = "#4A90E2";
    private static final String SECONDARY_COLOR = "#50C878";
    private static final String ACCENT_COLOR = "#FF6B6B";
    private static final String BACKGROUND_COLOR = "#F5F7FA";
    private static final String CARD_COLOR = "#FFFFFF";

    // Data file for persistent storage
    private static final String DEFAULT_DATA_FILE = "calendar_events.dat";
    private String dataFilePath;
    private Preferences prefs;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        primaryStage.setTitle("TaskFlow Calendar");

        // Set window icon
        primaryStage.getIcons().add(createWindowIcon());

        // Prevent app from closing when all windows are closed
        Platform.setImplicitExit(false);

        // Initialize preferences and data file path
        prefs = Preferences.userNodeForPackage(CalendarApp.class);
        dataFilePath = prefs.get("dataFilePath", getDefaultDataPath());

        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now(); // Initialize selected date to today

        // Load saved events
        loadEventsFromFile();

        // Main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        // Menu bar
        MenuBar menuBar = createMenuBar();

        // Top bar with navigation
        HBox topBar = createTopBar();

        VBox topContainer = new VBox(menuBar, topBar);
        mainLayout.setTop(topContainer);

        // Calendar grid in center
        VBox calendarContainer = createCalendarView();
        mainLayout.setCenter(calendarContainer);

        // Event details sidebar
        VBox sidebar = createSidebar();
        mainLayout.setRight(sidebar);

        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setScene(scene);

        // Handle window close event - minimize to tray instead of closing
        primaryStage.setOnCloseRequest(event -> {
            event.consume(); // Prevent default close
            hideToSystemTray();
        });

        primaryStage.show();

        // Set up system tray
        setupSystemTray();

        // Start notification checker
        startReminderChecker();
    }

    private String getDefaultDataPath() {
        // Default to user's home directory + Calendar folder
        String userHome = System.getProperty("user.home");
        File calendarDir = new File(userHome, "Calendar");
        if (!calendarDir.exists()) {
            calendarDir.mkdirs();
        }
        return new File(calendarDir, DEFAULT_DATA_FILE).getAbsolutePath();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("File");

        MenuItem saveItem = new MenuItem("Save Events");
        saveItem.setOnAction(e -> {
            saveEventsToFile();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Save Successful");
            alert.setHeaderText(null);
            alert.setContentText("All events have been saved successfully!");
            alert.showAndWait();
        });

        MenuItem importItem = new MenuItem("Import Events...");
        importItem.setOnAction(e -> importEvents());

        MenuItem exportItem = new MenuItem("Export Events...");
        exportItem.setOnAction(e -> exportEvents());

        MenuItem changeLocationItem = new MenuItem("Change Data Location...");
        changeLocationItem.setOnAction(e -> changeDataLocation());

        MenuItem viewLocationItem = new MenuItem("View Current Data Location");
        viewLocationItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Data Location");
            alert.setHeaderText("Current data file location:");
            alert.setContentText(dataFilePath);
            alert.showAndWait();
        });

        MenuItem hideItem = new MenuItem("Minimize to Tray");
        hideItem.setOnAction(e -> hideToSystemTray());

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Exit Calendar");
            confirm.setHeaderText("Are you sure you want to exit?");
            confirm.setContentText("The application will close completely and notifications will stop.");

            Optional<ButtonType> result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                saveEventsToFile(); // Save before exiting
                if (timer != null) {
                    timer.cancel();
                }
                if (trayIcon != null) {
                    SystemTray.getSystemTray().remove(trayIcon);
                }
                Platform.exit();
                System.exit(0);
            }
        });

        fileMenu.getItems().addAll(
                saveItem,
                new SeparatorMenuItem(),
                importItem,
                exportItem,
                new SeparatorMenuItem(),
                changeLocationItem,
                viewLocationItem,
                new SeparatorMenuItem(),
                hideItem,
                new SeparatorMenuItem(),
                exitItem
        );
        menuBar.getMenus().add(fileMenu);

        return menuBar;
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");

        Label title = new Label("📅 Calendar");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.WHITE);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button prevBtn = new Button("◀ Previous");
        Button todayBtn = new Button("Today");
        Button nextBtn = new Button("Next ▶");

        styleButton(prevBtn, CARD_COLOR, PRIMARY_COLOR);
        styleButton(todayBtn, SECONDARY_COLOR, CARD_COLOR);
        styleButton(nextBtn, CARD_COLOR, PRIMARY_COLOR);

        prevBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.minusMonths(1);
            updateCalendarView();
        });

        todayBtn.setOnAction(e -> {
            currentYearMonth = YearMonth.now();
            updateCalendarView();
        });

        nextBtn.setOnAction(e -> {
            currentYearMonth = currentYearMonth.plusMonths(1);
            updateCalendarView();
        });

        topBar.getChildren().addAll(title, spacer, prevBtn, todayBtn, nextBtn);
        return topBar;
    }

    private VBox createCalendarView() {
        VBox container = new VBox(20);
        container.setPadding(new Insets(30));
        container.setAlignment(Pos.TOP_CENTER);

        // Month/Year label
        monthYearLabel = new Label();
        monthYearLabel.setFont(Font.font("System", FontWeight.BOLD, 28));
        monthYearLabel.setTextFill(Color.web("#2C3E50"));

        // Day headers
        GridPane dayHeaders = new GridPane();
        dayHeaders.setHgap(10);
        dayHeaders.setVgap(10);
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (int i = 0; i < days.length; i++) {
            Label dayLabel = new Label(days[i]);
            dayLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
            dayLabel.setTextFill(Color.web("#7F8C8D"));
            dayLabel.setPrefWidth(140);
            dayLabel.setAlignment(Pos.CENTER);
            dayHeaders.add(dayLabel, i, 0);
        }

        // Calendar grid
        calendarGrid = new GridPane();
        calendarGrid.setHgap(10);
        calendarGrid.setVgap(10);

        container.getChildren().addAll(monthYearLabel, dayHeaders, calendarGrid);
        updateCalendarView();

        return container;
    }

    private VBox createSidebar() {
        VBox sidebar = new VBox(15);
        sidebar.setPadding(new Insets(30, 20, 30, 20));
        sidebar.setPrefWidth(350);
        sidebar.setStyle("-fx-background-color: " + CARD_COLOR + "; -fx-border-color: #E0E0E0; -fx-border-width: 0 0 0 1;");

        sidebarTitle = new Label("Today's Events");
        sidebarTitle.setFont(Font.font("System", FontWeight.BOLD, 20));
        sidebarTitle.setTextFill(Color.web("#2C3E50"));

        Button addEventBtn = new Button("+ Add New Event");
        addEventBtn.setMaxWidth(Double.MAX_VALUE);
        styleButton(addEventBtn, SECONDARY_COLOR, CARD_COLOR);
        addEventBtn.setOnAction(e -> showAddEventDialog(selectedDate));

        eventDetailsView = new ListView<>();
        eventDetailsView.setPlaceholder(new Label("No events for today"));
        eventDetailsView.setPrefHeight(400);
        VBox.setVgrow(eventDetailsView, Priority.ALWAYS);

        sidebar.getChildren().addAll(sidebarTitle, addEventBtn, eventDetailsView);
        updateEventDetailsView(LocalDate.now());

        return sidebar;
    }

    private void updateCalendarView() {
        calendarGrid.getChildren().clear();
        monthYearLabel.setText(currentYearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")));

        LocalDate firstOfMonth = currentYearMonth.atDay(1);
        int dayOfWeek = firstOfMonth.getDayOfWeek().getValue() % 7;
        int daysInMonth = currentYearMonth.lengthOfMonth();

        int row = 0;
        int col = dayOfWeek;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = LocalDate.of(currentYearMonth.getYear(), currentYearMonth.getMonth(), day);
            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(LocalDate date) {
        VBox cell = new VBox(5);
        cell.setPrefSize(140, 100);
        cell.setAlignment(Pos.TOP_CENTER);
        cell.setPadding(new Insets(8));

        boolean isToday = date.equals(LocalDate.now());
        boolean isSelected = date.equals(selectedDate);
        boolean hasEvents = eventsMap.containsKey(date) && !eventsMap.get(date).isEmpty();

        String cellStyle;

        if (isToday) {
            cellStyle = "-fx-background-color: " + PRIMARY_COLOR + "; " +
                    "-fx-border-color: " + PRIMARY_COLOR + "; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;";
        } else if (isSelected) {
            cellStyle = "-fx-background-color: #E8F4F8; " +
                    "-fx-border-color: " + PRIMARY_COLOR + "; " +
                    "-fx-border-width: 2; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;";
        } else {
            cellStyle = "-fx-background-color: " + CARD_COLOR + "; " +
                    "-fx-border-color: #E0E0E0; " +
                    "-fx-border-radius: 8; " +
                    "-fx-background-radius: 8; " +
                    "-fx-cursor: hand;";
        }

        cell.setStyle(cellStyle);

        Label dayNum = new Label(String.valueOf(date.getDayOfMonth()));
        dayNum.setFont(Font.font("System", FontWeight.BOLD, 16));
        dayNum.setTextFill(isToday ? Color.WHITE : (isSelected ? Color.web(PRIMARY_COLOR) : Color.web("#2C3E50")));

        cell.getChildren().add(dayNum);

        if (hasEvents) {
            int eventCount = eventsMap.get(date).size();
            Label eventIndicator = new Label(eventCount + " event" + (eventCount > 1 ? "s" : ""));
            eventIndicator.setFont(Font.font("System", 11));
            eventIndicator.setTextFill(isToday ? Color.WHITE : Color.web(ACCENT_COLOR));
            eventIndicator.setStyle("-fx-background-color: " + (isToday ? "rgba(255,255,255,0.3)" : "rgba(255,107,107,0.2)") + "; " +
                    "-fx-padding: 3 8 3 8; " +
                    "-fx-background-radius: 10;");
            cell.getChildren().add(eventIndicator);
        }

        cell.setOnMouseClicked(e -> {
            selectedDate = date;
            updateEventDetailsView(date);
            updateCalendarView(); // Refresh to show new selection
        });

        cell.setOnMouseEntered(e -> {
            if (!isToday && !isSelected) {
                cell.setStyle(cellStyle + "-fx-background-color: #F0F4F8;");
            }
        });

        cell.setOnMouseExited(e -> {
            cell.setStyle(cellStyle);
        });

        return cell;
    }

    private void updateEventDetailsView(LocalDate date) {
        eventDetailsView.getItems().clear();
        List<Event> dayEvents = eventsMap.getOrDefault(date, new ArrayList<>());

        // Update sidebar title to show selected date
        if (date.equals(LocalDate.now())) {
            sidebarTitle.setText("Today's Events");
        } else {
            sidebarTitle.setText(date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        }

        if (dayEvents.isEmpty()) {
            eventDetailsView.setPlaceholder(new Label("No events on this day"));
        } else {
            // Clear and rebuild the list with custom cell factory
            eventDetailsView.setCellFactory(lv -> new EventListCell(date));
            eventDetailsView.getItems().addAll(
                    dayEvents.stream().map(Event::toString).collect(Collectors.toList())
            );
        }
    }

    private class EventListCell extends ListCell<String> {
        private final LocalDate cellDate;

        public EventListCell(LocalDate date) {
            this.cellDate = date;
        }

        @Override
        protected void updateItem(String item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setGraphic(null);
                setText(null);
            } else {
                HBox container = new HBox(10);
                container.setAlignment(Pos.CENTER_LEFT);

                // Event details
                Label eventLabel = new Label(item);
                eventLabel.setWrapText(true);
                eventLabel.setMaxWidth(200);
                HBox.setHgrow(eventLabel, Priority.ALWAYS);

                // Edit button
                Button editBtn = new Button("Edit");
                editBtn.setStyle("-fx-background-color: " + SECONDARY_COLOR + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-background-radius: 5; " +
                        "-fx-cursor: hand;");
                editBtn.setTooltip(new Tooltip("Edit Event"));

                // Delete button
                Button deleteBtn = new Button("Delete");
                deleteBtn.setStyle("-fx-background-color: " + ACCENT_COLOR + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 5 10 5 10; " +
                        "-fx-background-radius: 5; " +
                        "-fx-cursor: hand;");
                deleteBtn.setTooltip(new Tooltip("Delete Event"));

                int eventIndex = getIndex();
                List<Event> dayEvents = eventsMap.get(cellDate);

                if (dayEvents != null && eventIndex < dayEvents.size()) {
                    Event event = dayEvents.get(eventIndex);

                    editBtn.setOnAction(e -> {
                        CalendarApp.this.showEditEventDialog(cellDate, event, eventIndex);
                    });

                    deleteBtn.setOnAction(e -> {
                        CalendarApp.this.deleteEvent(cellDate, event, eventIndex);
                    });
                }

                container.getChildren().addAll(eventLabel, editBtn, deleteBtn);
                setGraphic(container);
                setText(null);
            }
        }
    }

    private void showAddEventDialog(LocalDate initialDate) {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Add New Event");
        dialog.setHeaderText("Create a new event");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField();
        titleField.setPromptText("Event title");

        DatePicker datePicker = new DatePicker(initialDate);

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 12);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0);
        hourSpinner.setPrefWidth(80);
        minuteSpinner.setPrefWidth(80);

        TextArea descField = new TextArea();
        descField.setPromptText("Event description (optional)");
        descField.setPrefRowCount(3);

        // Multiple reminders
        Label remindersLabel = new Label("Reminders:");
        remindersLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        VBox remindersBox = new VBox(10);
        CheckBox reminder1 = new CheckBox("1 day before");
        CheckBox reminder2 = new CheckBox("1 hour before");
        CheckBox reminder3 = new CheckBox("30 minutes before");
        CheckBox reminder4 = new CheckBox("10 minutes before");
        CheckBox reminder5 = new CheckBox("At event time");

        remindersBox.getChildren().addAll(reminder1, reminder2, reminder3, reminder4, reminder5);

        HBox timeBox = new HBox(10, hourSpinner, new Label(":"), minuteSpinner);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Time:"), 0, 2);
        grid.add(timeBox, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descField, 1, 3);
        grid.add(remindersLabel, 0, 4);
        grid.add(remindersBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType addBtn = new ButtonType("Add Event", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(addBtn, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == addBtn && !titleField.getText().trim().isEmpty()) {
                String title = titleField.getText().trim();
                LocalDate date = datePicker.getValue();
                LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
                LocalDateTime eventDateTime = LocalDateTime.of(date, time);
                String description = descField.getText().trim();

                List<Integer> reminderMinutes = new ArrayList<>();
                if (reminder1.isSelected()) reminderMinutes.add(1440);  // 1 day
                if (reminder2.isSelected()) reminderMinutes.add(60);    // 1 hour
                if (reminder3.isSelected()) reminderMinutes.add(30);    // 30 min
                if (reminder4.isSelected()) reminderMinutes.add(10);    // 10 min
                if (reminder5.isSelected()) reminderMinutes.add(0);     // at time

                Event event = new Event(title, eventDateTime, description, reminderMinutes);

                eventsMap.computeIfAbsent(date, k -> new ArrayList<>()).add(event);

                // Save events to file
                saveEventsToFile();

                // Update selected date to show the new event
                selectedDate = date;
                updateCalendarView();
                updateEventDetailsView(date);

                return event;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void showEditEventDialog(LocalDate date, Event existingEvent, int eventIndex) {
        Dialog<Event> dialog = new Dialog<>();
        dialog.setTitle("Edit Event");
        dialog.setHeaderText("Update event details");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(20));

        TextField titleField = new TextField(existingEvent.title);
        titleField.setPromptText("Event title");

        DatePicker datePicker = new DatePicker(date);

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, existingEvent.dateTime.getHour());
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, existingEvent.dateTime.getMinute());
        hourSpinner.setPrefWidth(80);
        minuteSpinner.setPrefWidth(80);

        TextArea descField = new TextArea(existingEvent.description);
        descField.setPromptText("Event description (optional)");
        descField.setPrefRowCount(3);

        // Multiple reminders - pre-check based on existing event
        Label remindersLabel = new Label("Reminders:");
        remindersLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        VBox remindersBox = new VBox(10);
        CheckBox reminder1 = new CheckBox("1 day before");
        CheckBox reminder2 = new CheckBox("1 hour before");
        CheckBox reminder3 = new CheckBox("30 minutes before");
        CheckBox reminder4 = new CheckBox("10 minutes before");
        CheckBox reminder5 = new CheckBox("At event time");

        // Pre-select existing reminders
        for (Integer minutes : existingEvent.reminderMinutes) {
            if (minutes == 1440) reminder1.setSelected(true);
            else if (minutes == 60) reminder2.setSelected(true);
            else if (minutes == 30) reminder3.setSelected(true);
            else if (minutes == 10) reminder4.setSelected(true);
            else if (minutes == 0) reminder5.setSelected(true);
        }

        remindersBox.getChildren().addAll(reminder1, reminder2, reminder3, reminder4, reminder5);

        HBox timeBox = new HBox(10, hourSpinner, new Label(":"), minuteSpinner);
        timeBox.setAlignment(Pos.CENTER_LEFT);

        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Date:"), 0, 1);
        grid.add(datePicker, 1, 1);
        grid.add(new Label("Time:"), 0, 2);
        grid.add(timeBox, 1, 2);
        grid.add(new Label("Description:"), 0, 3);
        grid.add(descField, 1, 3);
        grid.add(remindersLabel, 0, 4);
        grid.add(remindersBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        ButtonType saveBtn = new ButtonType("Save Changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == saveBtn && !titleField.getText().trim().isEmpty()) {
                String title = titleField.getText().trim();
                LocalDate newDate = datePicker.getValue();
                LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
                LocalDateTime eventDateTime = LocalDateTime.of(newDate, time);
                String description = descField.getText().trim();

                List<Integer> reminderMinutes = new ArrayList<>();
                if (reminder1.isSelected()) reminderMinutes.add(1440);
                if (reminder2.isSelected()) reminderMinutes.add(60);
                if (reminder3.isSelected()) reminderMinutes.add(30);
                if (reminder4.isSelected()) reminderMinutes.add(10);
                if (reminder5.isSelected()) reminderMinutes.add(0);

                // Remove old event
                List<Event> oldDateEvents = eventsMap.get(date);
                if (oldDateEvents != null) {
                    oldDateEvents.remove(eventIndex);
                    if (oldDateEvents.isEmpty()) {
                        eventsMap.remove(date);
                    }
                }

                // Add updated event
                Event updatedEvent = new Event(title, eventDateTime, description, reminderMinutes);
                eventsMap.computeIfAbsent(newDate, k -> new ArrayList<>()).add(updatedEvent);

                // Save changes
                saveEventsToFile();

                // Update view
                selectedDate = newDate;
                updateCalendarView();
                updateEventDetailsView(newDate);

                return updatedEvent;
            }
            return null;
        });

        dialog.showAndWait();
    }

    private void deleteEvent(LocalDate date, Event event, int eventIndex) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Event");
        confirm.setHeaderText("Are you sure you want to delete this event?");
        confirm.setContentText("Event: " + event.title + "\nTime: " +
                event.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            List<Event> dayEvents = eventsMap.get(date);
            if (dayEvents != null) {
                dayEvents.remove(eventIndex);

                // Remove date from map if no more events
                if (dayEvents.isEmpty()) {
                    eventsMap.remove(date);
                }

                // Save changes
                saveEventsToFile();

                // Update view
                updateCalendarView();
                updateEventDetailsView(date);

                // Show confirmation
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Event Deleted");
                success.setHeaderText(null);
                success.setContentText("Event deleted successfully!");
                success.show();
            }
        }
    }

    private void startReminderChecker() {
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                LocalDateTime now = LocalDateTime.now();

                for (List<Event> eventList : eventsMap.values()) {
                    for (Event event : eventList) {
                        for (int i = 0; i < event.reminderMinutes.size(); i++) {
                            if (!event.notified[i]) {
                                int reminderMin = event.reminderMinutes.get(i);
                                LocalDateTime reminderTime = event.dateTime.minusMinutes(reminderMin);

                                if (now.isAfter(reminderTime) || now.isEqual(reminderTime)) {
                                    final int index = i;
                                    Platform.runLater(() -> showNotification(event, reminderMin));
                                    event.notified[index] = true;
                                }
                            }
                        }
                    }
                }
            }
        }, 0, 30_000); // Check every 30 seconds
    }

    private void setupSystemTray() {
        // Check if system tray is supported
        if (!SystemTray.isSupported()) {
            System.out.println("System tray is not supported");
            return;
        }

        SystemTray systemTray = SystemTray.getSystemTray();

        // Create tray icon image
        BufferedImage trayIconImage = createTrayIcon();

        // Create popup menu
        PopupMenu popup = new PopupMenu();

        // Use java.awt.MenuItem explicitly for system tray
        java.awt.MenuItem openItem = new java.awt.MenuItem("Open Calendar");
        openItem.addActionListener(e -> Platform.runLater(this::showWindow));

        java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
        exitItem.addActionListener(e -> {
            Platform.runLater(() -> {
                saveEventsToFile(); // Save before exiting
                if (timer != null) {
                    timer.cancel();
                }
                systemTray.remove(trayIcon);
                Platform.exit();
                System.exit(0);
            });
        });

        popup.add(openItem);
        popup.addSeparator();
        popup.add(exitItem);

        // Create tray icon
        trayIcon = new TrayIcon(trayIconImage, "TaskFlow Calendar", popup);
        trayIcon.setImageAutoSize(true);

        // Double-click to open
        trayIcon.addActionListener(e -> Platform.runLater(this::showWindow));

        try {
            systemTray.add(trayIcon);
        } catch (AWTException e) {
            System.err.println("Unable to add tray icon");
            e.printStackTrace();
        }
    }

    private BufferedImage createTrayIcon() {
        // Create a simple calendar icon
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw calendar icon (using java.awt.Color explicitly)
        g.setColor(new java.awt.Color(74, 144, 226)); // PRIMARY_COLOR
        g.fillRoundRect(1, 3, 14, 12, 3, 3);

        g.setColor(java.awt.Color.WHITE);
        g.fillRect(2, 1, 3, 3);
        g.fillRect(11, 1, 3, 3);

        g.setColor(java.awt.Color.WHITE);
        g.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 8));
        g.drawString(String.valueOf(LocalDate.now().getDayOfMonth()), 5, 12);

        g.dispose();
        return image;
    }

    private void hideToSystemTray() {
        primaryStage.hide();

        // Show notification that app is still running
        if (trayIcon != null) {
            trayIcon.displayMessage("Calendar Running",
                    "The calendar is still running in the background. Notifications will continue.",
                    TrayIcon.MessageType.INFO);
        }
    }

    private void showWindow() {
        primaryStage.show();
        primaryStage.toFront();
    }

    private Image createWindowIcon() {
        // Create a calendar icon for the window
        int size = 64;
        BufferedImage bufferedImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = bufferedImage.createGraphics();

        // Enable anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw calendar background
        g.setColor(new java.awt.Color(74, 144, 226)); // PRIMARY_COLOR
        g.fillRoundRect(8, 16, 48, 40, 8, 8);

        // Draw calendar header (darker blue)
        g.setColor(new java.awt.Color(50, 100, 180));
        g.fillRoundRect(8, 16, 48, 12, 8, 8);
        g.fillRect(8, 20, 48, 8);

        // Draw binding rings
        g.setColor(java.awt.Color.WHITE);
        g.fillOval(16, 10, 8, 8);
        g.fillOval(40, 10, 8, 8);

        // Draw grid lines
        g.setColor(new java.awt.Color(255, 255, 255, 100));
        g.drawLine(20, 32, 44, 32);
        g.drawLine(20, 40, 44, 40);
        g.drawLine(20, 48, 44, 48);
        g.drawLine(28, 28, 28, 52);
        g.drawLine(36, 28, 36, 52);

        // Draw current day highlight
        g.setColor(new java.awt.Color(255, 107, 107)); // ACCENT_COLOR
        g.fillOval(38, 42, 6, 6);

        g.dispose();

        // Convert BufferedImage to JavaFX Image
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(bufferedImage, "png", baos);
            return new Image(new ByteArrayInputStream(baos.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveEventsToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(dataFilePath))) {
            for (Map.Entry<LocalDate, List<Event>> entry : eventsMap.entrySet()) {
                LocalDate date = entry.getKey();
                for (Event event : entry.getValue()) {
                    // Format: DATE|TITLE|TIME|DESCRIPTION|REMINDERS
                    String reminders = event.reminderMinutes.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(","));

                    String line = String.format("%s|%s|%s|%s|%s",
                            date.toString(),
                            escapeString(event.title),
                            event.dateTime.toLocalTime().toString(),
                            escapeString(event.description),
                            reminders
                    );
                    writer.println(line);
                }
            }
            System.out.println("Events saved successfully to " + dataFilePath);
        } catch (IOException e) {
            System.err.println("Error saving events: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Error Saving Events", "Could not save events to file: " + e.getMessage());
        }
    }

    private void loadEventsFromFile() {
        File file = new File(dataFilePath);
        if (!file.exists()) {
            System.out.println("No saved events found at: " + dataFilePath);
            return;
        }

        eventsMap.clear(); // Clear existing events before loading

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int loadedCount = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 5) {
                        LocalDate date = LocalDate.parse(parts[0]);
                        String title = unescapeString(parts[1]);
                        LocalTime time = LocalTime.parse(parts[2]);
                        String description = unescapeString(parts[3]);

                        List<Integer> reminders = new ArrayList<>();
                        if (!parts[4].isEmpty()) {
                            String[] reminderParts = parts[4].split(",");
                            for (String r : reminderParts) {
                                reminders.add(Integer.parseInt(r.trim()));
                            }
                        }

                        LocalDateTime dateTime = LocalDateTime.of(date, time);
                        Event event = new Event(title, dateTime, description, reminders);

                        eventsMap.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
                        loadedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line);
                    e.printStackTrace();
                }
            }
            System.out.println("Loaded " + loadedCount + " events from file.");
        } catch (IOException e) {
            System.err.println("Error loading events: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void importEvents() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Events");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Calendar Data Files", "*.dat")
        );
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Import Events");
            confirm.setHeaderText("How would you like to import?");
            confirm.setContentText("Choose import option:");

            ButtonType mergeBtn = new ButtonType("Merge with Current");
            ButtonType replaceBtn = new ButtonType("Replace All");
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            confirm.getButtonTypes().setAll(mergeBtn, replaceBtn, cancelBtn);

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isPresent() && result.get() == mergeBtn) {
                // Merge: Load and add to existing events
                importAndMerge(selectedFile);
            } else if (result.isPresent() && result.get() == replaceBtn) {
                // Replace: Clear current and load new
                eventsMap.clear();
                String tempPath = dataFilePath;
                dataFilePath = selectedFile.getAbsolutePath();
                loadEventsFromFile();
                dataFilePath = tempPath;
                saveEventsToFile(); // Save merged data to current location
                updateCalendarView();
                updateEventDetailsView(selectedDate);

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Import Complete");
                success.setHeaderText(null);
                success.setContentText("Events replaced successfully!");
                success.showAndWait();
            }
        }
    }

    private void importAndMerge(File importFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(importFile))) {
            String line;
            int importedCount = 0;
            while ((line = reader.readLine()) != null) {
                try {
                    String[] parts = line.split("\\|");
                    if (parts.length >= 5) {
                        LocalDate date = LocalDate.parse(parts[0]);
                        String title = unescapeString(parts[1]);
                        LocalTime time = LocalTime.parse(parts[2]);
                        String description = unescapeString(parts[3]);

                        List<Integer> reminders = new ArrayList<>();
                        if (!parts[4].isEmpty()) {
                            String[] reminderParts = parts[4].split(",");
                            for (String r : reminderParts) {
                                reminders.add(Integer.parseInt(r.trim()));
                            }
                        }

                        LocalDateTime dateTime = LocalDateTime.of(date, time);
                        Event event = new Event(title, dateTime, description, reminders);

                        eventsMap.computeIfAbsent(date, k -> new ArrayList<>()).add(event);
                        importedCount++;
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing line during import: " + line);
                }
            }

            saveEventsToFile(); // Save merged data
            updateCalendarView();
            updateEventDetailsView(selectedDate);

            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("Import Complete");
            success.setHeaderText(null);
            success.setContentText(importedCount + " events imported and merged successfully!");
            success.showAndWait();

        } catch (IOException e) {
            showErrorAlert("Import Error", "Could not import events: " + e.getMessage());
        }
    }

    private void exportEvents() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export Events");
        fileChooser.setInitialFileName("calendar_backup_" + LocalDate.now() + ".dat");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Calendar Data Files", "*.dat")
        );

        File selectedFile = fileChooser.showSaveDialog(primaryStage);
        if (selectedFile != null) {
            try {
                Files.copy(Paths.get(dataFilePath), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Export Successful");
                success.setHeaderText(null);
                success.setContentText("Events exported successfully to:\n" + selectedFile.getAbsolutePath());
                success.showAndWait();
            } catch (IOException e) {
                showErrorAlert("Export Error", "Could not export events: " + e.getMessage());
            }
        }
    }

    private void changeDataLocation() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Change Data Location");
        info.setHeaderText("Select a new folder for storing calendar data");
        info.setContentText("Your current events will be moved to the new location.");
        info.showAndWait();

        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose Data Storage Folder");

        File currentDir = new File(dataFilePath).getParentFile();
        if (currentDir.exists()) {
            dirChooser.setInitialDirectory(currentDir);
        }

        File selectedDir = dirChooser.showDialog(primaryStage);
        if (selectedDir != null) {
            String newPath = new File(selectedDir, DEFAULT_DATA_FILE).getAbsolutePath();

            try {
                // Copy current data to new location
                File oldFile = new File(dataFilePath);
                if (oldFile.exists()) {
                    Files.copy(oldFile.toPath(), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
                }

                // Update data file path
                dataFilePath = newPath;
                prefs.put("dataFilePath", dataFilePath);

                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("Location Changed");
                success.setHeaderText("Data location updated successfully!");
                success.setContentText("New location: " + dataFilePath);
                success.showAndWait();

            } catch (IOException e) {
                showErrorAlert("Error", "Could not move data file: " + e.getMessage());
            }
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String escapeString(String str) {
        if (str == null) return "";
        return str.replace("|", "&#124;").replace("\n", "&#10;");
    }

    private String unescapeString(String str) {
        if (str == null) return "";
        return str.replace("&#124;", "|").replace("&#10;", "\n");
    }

    private void showNotification(Event event, int minutesBefore) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("📅 Event Reminder");

        String headerText = minutesBefore == 0 ?
                "Event happening now!" :
                "Event in " + formatMinutes(minutesBefore);

        alert.setHeaderText(headerText);
        alert.setContentText("Event: " + event.title + "\n" +
                "Time: " + event.dateTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")) +
                (event.description.isEmpty() ? "" : "\n\n" + event.description));

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: " + CARD_COLOR + ";");

        alert.show();
    }

    private String formatMinutes(int minutes) {
        if (minutes >= 1440) return (minutes / 1440) + " day(s)";
        if (minutes >= 60) return (minutes / 60) + " hour(s)";
        return minutes + " minute(s)";
    }

    private void styleButton(Button btn, String bgColor, String textColor) {
        btn.setStyle("-fx-background-color: " + bgColor + "; " +
                "-fx-text-fill: " + textColor + "; " +
                "-fx-font-weight: bold; " +
                "-fx-padding: 10 20 10 20; " +
                "-fx-background-radius: 5; " +
                "-fx-cursor: hand;");

        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 0.9;"));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle() + "-fx-opacity: 1.0;"));
    }

    public static void main(String[] args) {
        System.out.println("=== CalendarApp Starting ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        launch(args);
    }

    static class Event {
        String title;
        LocalDateTime dateTime;
        String description;
        List<Integer> reminderMinutes;
        boolean[] notified;

        Event(String title, LocalDateTime dateTime, String description, List<Integer> reminderMinutes) {
            this.title = title;
            this.dateTime = dateTime;
            this.description = description;
            this.reminderMinutes = reminderMinutes;
            this.notified = new boolean[reminderMinutes.size()];
        }

        @Override
        public String toString() {
            String time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            String reminders = reminderMinutes.stream()
                    .map(m -> m == 0 ? "at time" : m + "min before")
                    .collect(Collectors.joining(", "));
            return "🕐 " + time + " - " + title +
                    (description.isEmpty() ? "" : "\n   " + description) +
                    "\n   📢 Reminders: " + reminders;
        }
    }
}