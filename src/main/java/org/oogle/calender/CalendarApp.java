package org.oogle.calender;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    // Color scheme
    private static final String PRIMARY_COLOR = "#4A90E2";
    private static final String SECONDARY_COLOR = "#50C878";
    private static final String ACCENT_COLOR = "#FF6B6B";
    private static final String BACKGROUND_COLOR = "#F5F7FA";
    private static final String CARD_COLOR = "#FFFFFF";

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Professional Calendar");

        currentYearMonth = YearMonth.now();
        selectedDate = LocalDate.now(); // Initialize selected date to today

        // Main layout
        BorderPane mainLayout = new BorderPane();
        mainLayout.setStyle("-fx-background-color: " + BACKGROUND_COLOR + ";");

        // Top bar with navigation
        HBox topBar = createTopBar();
        mainLayout.setTop(topBar);

        // Calendar grid in center
        VBox calendarContainer = createCalendarView();
        mainLayout.setCenter(calendarContainer);

        // Event details sidebar
        VBox sidebar = createSidebar();
        mainLayout.setRight(sidebar);

        Scene scene = new Scene(mainLayout, 1200, 700);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start notification checker
        startReminderChecker();
    }

    private HBox createTopBar() {
        HBox topBar = new HBox(15);
        topBar.setPadding(new Insets(20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: " + PRIMARY_COLOR + ";");

        Label title = new Label("📅 My Calendar");
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
            for (Event event : dayEvents) {
                eventDetailsView.getItems().add(event.toString());
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