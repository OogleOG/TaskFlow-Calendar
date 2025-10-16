# 📅 TaskFlow Calendar

A professional, feature-rich desktop calendar application built with JavaFX. TaskFlow Calendar helps you manage events, meetings, and tasks with powerful reminder capabilities and seamless data portability.

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Java](https://img.shields.io/badge/Java-20-orange.svg)
![JavaFX](https://img.shields.io/badge/JavaFX-20.0.2-green.svg)
![Platform](https://img.shields.io/badge/platform-Windows-lightgrey.svg)

## ✨ Features

### 🗓️ Calendar Management
- **Monthly Grid View** - Navigate through months with an intuitive calendar interface
- **Visual Event Indicators** - See at a glance which days have scheduled events
- **Date Selection** - Click any date to view and manage events for that day
- **Today Highlighting** - Current date clearly marked in the calendar

### ⏰ Event Management
- **Create Events** - Add events with title, date, time, and detailed descriptions
- **Edit Events** - Modify any event details after creation
- **Delete Events** - Remove events with confirmation to prevent accidents
- **Multiple Reminders** - Set up to 5 different reminder times per event:
    - 1 day before
    - 1 hour before
    - 30 minutes before
    - 10 minutes before
    - At event time

### 🔔 Smart Notifications
- **System Notifications** - Desktop alerts for upcoming events
- **Background Operation** - Continues running in system tray when window is closed
- **Persistent Reminders** - Notifications work even after system reboot
- **Auto-Start Support** - Optional automatic launch on Windows startup

### 💾 Data Management
- **Persistent Storage** - All events automatically saved to disk
- **Custom Data Location** - Choose where to store your calendar data
- **Import/Export** - Transfer your calendar between computers
- **Merge or Replace** - Choose how to import data from backups
- **Automatic Backups** - Events saved on every change

### 🎨 User Interface
- **Modern Design** - Clean, professional interface with contemporary color scheme
- **System Tray Integration** - Minimize to tray for unobtrusive background operation
- **Custom Icon** - Beautiful calendar icon throughout the application
- **Responsive Layout** - Smooth hover effects and visual feedback

## 🚀 Installation

### For Users (Windows)

1. **Download** the latest `TaskFlowCalendar-1.0.exe` from the [Releases](../../releases) page
2. **Run** the installer and follow the setup wizard
3. **Launch** TaskFlow Calendar from the Start Menu or Desktop shortcut

**Note:** No Java installation required! The installer includes everything needed.

### Optional: Auto-Start on Windows Boot

1. Press `Win + R` and type `shell:startup`
2. Create a shortcut to `C:\Program Files\TaskFlowCalendar\TaskFlowCalendar.exe`
3. TaskFlow Calendar will now start automatically when you log in

## 🛠️ Building from Source

### Prerequisites

- Java Development Kit (JDK) 20 or higher
- Gradle 7.0+
- Git

### Clone the Repository

```bash
git clone https://github.com/OogleOG/taskflow-calendar.git
cd taskflow-calendar
```

### Build the Application

```bash
# Build the JAR
gradle clean build

# Run the application (for testing)
gradle run
```

### Create Windows Installer

```bash
# Prepare dependencies
gradle prepareJpackage

# Create installer (requires JDK with jpackage tool)
jpackage --input build/jpackage-input \
  --name "TaskFlowCalendar" \
  --vendor "Prodexa" \
  --main-jar calender.jar \
  --main-class org.oogle.calender.CalendarApp \
  --icon calendar-icon.ico \
  --type exe \
  --win-shortcut \
  --win-menu \
  --win-menu-group "Calendar" \
  --app-version "1.0"
```

The installer will be created in the project root directory.

## 📖 Usage Guide

### Creating an Event

1. Click the **"+ Add New Event"** button in the sidebar
2. Fill in the event details:
    - **Title**: Name of your event
    - **Date**: When the event occurs
    - **Time**: Specific time (hours and minutes)
    - **Description**: Additional details (optional)
    - **Reminders**: Select one or more reminder times
3. Click **"Add Event"**

### Editing an Event

1. Select the date containing the event
2. Click the **"Edit"** button next to the event in the sidebar
3. Modify any details
4. Click **"Save Changes"**

### Deleting an Event

1. Select the date containing the event
2. Click the **"Delete"** button next to the event
3. Confirm deletion in the dialog

### Importing/Exporting Events

**Export (Backup):**
- `File` → `Export Events...`
- Choose save location
- Your events are saved as a `.dat` file

**Import (Restore):**
- `File` → `Import Events...`
- Select your backup file
- Choose "Merge" to add to existing events or "Replace" to overwrite

### Changing Data Storage Location

- `File` → `Change Data Location...`
- Select new folder for storing calendar data
- Existing events will be moved automatically

## ⚙️ System Requirements

- **Operating System**: Windows 10 or later (64-bit)
- **Memory**: 256 MB RAM minimum
- **Disk Space**: 200 MB for installation
- **Display**: 1024x768 minimum resolution

## 🔧 Technologies Used

- **Java 20** - Core application language
- **JavaFX 20.0.2** - Modern UI framework
- **Gradle** - Build automation and dependency management
- **jpackage** - Native installer creation
- **AWT** - System tray integration

### Dependencies

- `org.openjfx:javafx-*:20.0.2` - JavaFX modules
- `org.fxmisc.richtext:richtextfx:0.10.9` - Rich text components
- `org.json:json:20230618` - JSON processing
- `com.google.code.gson:gson:2.10.1` - JSON serialization

## 📂 Project Structure

```
taskflow-calendar/
├── src/
│   └── main/
│       └── java/
│           └── org/
│               └── oogle/
│                   └── calender/
│                       └── CalendarApp.java
├── build.gradle.kts
├── calendar-icon.ico
├── README.md
└── LICENSE
```

## 🗺️ Roadmap

Future enhancements planned:

- [ ] Recurring events (daily, weekly, monthly, yearly)
- [ ] Calendar sharing and collaboration
- [ ] Cloud sync support
- [ ] Custom color coding for different event types
- [ ] Month/Week/Day view options
- [ ] Search and filter functionality
- [ ] Export to iCal/Google Calendar format
- [ ] Dark mode theme
- [ ] Multi-language support

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- JavaFX community for excellent documentation
- Contributors and testers
- Icon design inspired by modern calendar applications

## 📧 Contact

**Developer**: Oogle  
**Project Link**: [https://github.com/OogleOG/TaskFlow-Calendar](https://github.com/OogleOG/TaskFlow-Calendar)

---

⭐ If you find TaskFlow Calendar useful, please consider giving it a star on GitHub!

## 📸 Screenshots

*Coming soon - screenshots of the application in action*

---

**Made with ❤️ using JavaFX**
