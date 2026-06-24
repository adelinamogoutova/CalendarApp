import scalafx.scene.layout.{GridPane, HBox, Pane, StackPane, VBox}
import scalafx.scene.control.*
import scalafx.scene.shape.*
import scalafx.application.JFXApp3
import scalafx.scene.Scene
import scalafx.scene.paint.Color
import scalafx.scene.shape.Rectangle
import scalafx.scene.paint.Color.*
import scalafx.geometry.{Insets, Pos}
import scalafx.scene.input.MouseEvent
import scalafx.scene.text.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scalafx.Includes.*
import scalafx.collections.ObservableBuffer
import java.util.Locale
import scala.collection.mutable


object Main extends JFXApp3:

  val manager = new CalendarManager()


  val selectionRect = new Rectangle:
    fill = LightBlue.opacity(0.3)
    stroke = Blue
    strokeWidth = 1
    visible = false
    mouseTransparent = true

  val readyCategories = ObservableBuffer[Category](new Category("Study", Blue), new Category("Work", Orange), new Category("Hobby", Pink), new Category("Holiday", Red), new Category("Other", Gray))
  val categoryFilters = mutable.Map[String, Boolean]()
  readyCategories.foreach(category => categoryFilters(category.name) = true)


  // Adding reminders
  val reminderItems = ObservableBuffer[String]()
  val reminderFileName = "reminders.txt"

  // saves reminders to file so they persist between sessions
  def saveReminders(): Unit =
    val out = new java.io.PrintWriter(reminderFileName)
    reminderItems.foreach(out.println)
    out.close()

  // loads saved reminders from file
  def loadReminders(): Unit =
    val file = new java.io.File(reminderFileName)
    if (!file.exists()) return
    val source = scala.io.Source.fromFile(file)
    source.getLines().filter(_.nonEmpty).foreach(line => reminderItems += line)
    source.close()

  def showAddReminderDialog(): Unit =
        val dialog = new TextInputDialog():
          title = "New Reminder"
          headerText = "Add a task to your list"
          contentText = "Task:"

        val result = dialog.showAndWait()
        result.foreach(task =>
          reminderItems += s"• $task"
          saveReminders())


  def start() =
    manager.clearAll()
    manager.loadFromFile("holidays.ics", readyCategories)
    manager.loadFromFile("calendar.ics", readyCategories)
    loadReminders()

    readyCategories.foreach(categ =>
    if (!categoryFilters.contains(categ.name)) categoryFilters(categ.name) = true)

    stage = new JFXApp3.PrimaryStage:
      title = "Calendar"
      width = 1600
      height = 1000

    val root = new Pane():
      background = null
    val scene = Scene(parent = root)
    stage.scene = scene
    scene.setFill(Color.rgb(189,135,82))

    // re-renders both views; called whenever events, dates or filters change
    def refresh(): Unit =
      weekView.render()
      dayView.render()

    // editing calendar events
    def ShowEditEventDialog(event: Event) =
      val dialog = new Dialog[ButtonType]():
        title = "Edit event"
        headerText = s"Editing event ${event.title}"

      val deleteButton = new ButtonType("Delete", ButtonBar.ButtonData.Left)
      dialog.dialogPane().buttonTypes = Seq(deleteButton, ButtonType.OK, ButtonType.Cancel)

      val nameField = new TextField:
        text = event.title
      val descField = new TextField:
        text = event.description
      val locField = new TextField:
        text = event.location

      val grid = new GridPane():
        hgap = 10
        vgap = 10
        padding = Insets(20)
        add(new Label("Name:"), 0, 0)
        add(nameField, 1, 0)
        add(new Label("Description:"), 0, 1)
        add(descField, 1, 1)
        add(new Label("Location:"), 0, 2)
        add(locField, 1, 2)

      dialog.dialogPane().content = grid

      val result = dialog.showAndWait()

      result match
        case Some(`deleteButton`) =>
          manager.removeEntry(event)
          refresh()
        case Some(ButtonType.OK) =>
          event.updateDetails(nameField.text.value, descField.text.value, locField.text.value)
          manager.updateEntry(event)
          refresh()
        case _ =>


    // New events can be added to the calendar
    def showAddEventDialog(startTime: LocalDateTime, duration: Int) =
      val dialog = new Dialog[Event]():
        title = "Add New Event"
        headerText = s"Time: ${startTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"))} - Duration: ${duration}h"

      dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)

      val datePicker = new DatePicker(startTime.toLocalDate)

      val hourChoice = new ChoiceBox[Int]:
        items = ObservableBuffer.from((0 to 23).toList)
        value = startTime.getHour

      val durationField = new TextField:
        text = duration.toString
        promptText = "Duration (h)"

      val nameField = new TextField:
        promptText = "Event Name"
      val descriptionField = new TextField:
        promptText = "Description"
      val locationField = new TextField:
        promptText = "Location"

      val categoryChoose = new ChoiceBox[String]:
        items = ObservableBuffer.from(readyCategories.map(_.name))
        value = "Category"

      val grid = new GridPane():
        hgap = 10
        vgap = 10
        padding = Insets(20, 150, 10, 10)
        add(new Label("Date:"), 0, 0)
        add(datePicker, 1, 0)
        add(new Label("Start Hour:"), 0, 1)
        add(hourChoice, 1, 1)
        add(new Label("Name"), 0, 2)
        add(nameField, 1, 2)
        add(new Label("Description:"), 0, 3)
        add(descriptionField, 1, 3)
        add(new Label("Duration (h):"), 0, 4)
        add(durationField, 1, 4)
        add(new Label("Location"), 0, 5)
        add(locationField, 1, 5)
        add(new Label("Category:"), 0, 6)
        add(categoryChoose, 1, 6)

      dialog.dialogPane().content = grid

      dialog.resultConverter = (buttonType) =>
        if (buttonType == ButtonType.OK) then
          val selectedDate = datePicker.value.value
          val selectedHour = hourChoice.value.value
          val finalStart = selectedDate.atTime(selectedHour, 0)
          val duration = durationField.text.value.toIntOption.getOrElse(1)
          val selectedCategory = readyCategories.find(_.name == categoryChoose.value.value).getOrElse(readyCategories.last)
          new Event(nameField.text.value, descriptionField.text.value, locationField.text.value, selectedCategory, finalStart, finalStart.plusHours(duration))
        else
          null

      val result = dialog.showAndWait()

      result match
        case Some(event: Event) =>
          if event.validate() then
            manager.addEntry(event)
            weekView.goTo(event.startDateTime)
            dayView.goTo(event.startDateTime)
        case _ =>


    lazy val weekView: WeekView =
      new WeekView(manager, categoryFilters, selectionRect, showAddEventDialog, ShowEditEventDialog)
    lazy val dayView: DayView =
      new DayView(manager, categoryFilters, selectionRect, showAddEventDialog, ShowEditEventDialog)

    val weeklyView = weekView.pane
    val dailyView = dayView.pane

    refresh()


    val stack = new StackPane:
      layoutX = 370
      layoutY = 50
      prefWidth = 1600
      prefHeight = 1000
      alignment = Pos.TopLeft
      background = null
      children = Seq(weeklyView, dailyView)

    //Add event by pressing a button
    val buttonAddEvent = new Button("Add Event")
    buttonAddEvent.onAction = _ =>
      showAddEventDialog(LocalDateTime.now().withMinute(0), 1)


    val sidebarEvent = new VBox(10):
      layoutX = 228
      layoutY = 20
      children = buttonAddEvent


    // Add a button to switch to weekly view
    val buttonWeek = new Button("Week")
    buttonWeek.onAction = _ =>
      weeklyView.visible = true
      dailyView.visible = false

    val sidebarWeek = new VBox(10):
      layoutX = 1425
      layoutY = 20
      children = buttonWeek

    // Add a button to switch to daily view
    val buttonDay = new Button("Day")
      buttonDay.onAction = _ =>
      dailyView.visible = true
      weeklyView.visible = false

    val sidebarDay = new VBox(10):
      layoutX = 1380
      layoutY = 20
      children = buttonDay

    // button to switch to the next day/week
    val buttonNext = new Button("Next")
      buttonNext.onAction = _ =>
      if weeklyView.isVisible then weekView.next()
      else dayView.next()

    val sidebarNextWeek = new VBox(10):
      layoutX = 450
      layoutY = 20
      children = buttonNext

    // button to switch to the previous day/week
    val buttonPrevious = new Button("Previous")
      buttonPrevious.onAction = _ =>
      if weeklyView.isVisible then weekView.previous()
      else dayView.previous()

    val sidebarPreviousWeek = new VBox(10):
      layoutX = 370
      layoutY = 20
      children = buttonPrevious

    // Add the control panel to the side
    val controlPanel = new Rectangle:
      x = 50
      y = 50
      width = 250
      height = 450
      fill = White

    val categoryText = new Text("Categories:"):
      font = Font.font("Arial", FontWeight.Bold, 16)
      fill = Black

    val sidebarCategory = new VBox(10):
      layoutX = 125
      layoutY = 100


    def rebuildSidebar() =
      sidebarCategory.children.clear()
      sidebarCategory.children.add(categoryText)

      val newRows: Seq[javafx.scene.Node] = readyCategories.toSeq.map(category =>
          val checkbox = new CheckBox:
            selected = categoryFilters.getOrElse(category.name, true)
            onAction = _ =>
              categoryFilters(category.name) = selected.value
              refresh()

          val categoryDot = new Circle:
            radius = 6
            fill = category.color

          val categoryLabel = new Label(category.name):
            font = Font.font("Arial", 14)
            textFill = Black

          new HBox(10):
            alignment = Pos.CenterLeft
            children.addAll(checkbox, categoryDot, categoryLabel))

      sidebarCategory.children.addAll(newRows)

      val reminderHeader = new Text("Reminders:"):
        font = Font.font("Arial", FontWeight.Bold, 16)
        fill = Black
        margin = Insets(20, 0, 5, 0)

      val reminderListView = new VBox(5)

      def refreshReminderList(): Unit =
        reminderListView.children.clear()
        reminderItems.zipWithIndex.foreach { case (text, index) =>
          val label = new Label(text):
            font = Font.font("Arial", 13)
            wrapText = true
            maxWidth = 170

          val deleteButton = new Button("✕"):
            font = Font.font("Arial", 10)
            onAction = _ =>
              reminderItems.remove(index)
              saveReminders()

          reminderListView.children.add(new HBox(5):
            alignment = Pos.CenterLeft
            children.addAll(label, deleteButton))}

      refreshReminderList()
      reminderItems.onChange { refreshReminderList() }

      sidebarCategory.children.addAll(reminderHeader, reminderListView)

    rebuildSidebar()


    // add a new category
    val buttonAddCategory = new Button("Add Category")
      buttonAddCategory.onAction = _ =>
        val dialog = new Dialog[Category]():
          title = "New Category"
          headerText = "Give a name and choose a color for the category."

        dialog.dialogPane().buttonTypes = Seq(ButtonType.OK, ButtonType.Cancel)
        val nameField = new TextField { promptText = "Category Name" }
        val colorPicker = new ColorPicker(Color.Green)

        val grid = new GridPane():
          hgap = 10
          vgap = 10
          padding = Insets(20)
          add(new Label("Name:"), 0, 0)
          add(nameField, 1, 0)
          add(new Label("Color:"), 0, 1)
          add(colorPicker, 1, 1)

        dialog.dialogPane().content = grid

        dialog.resultConverter = (buttonType) =>
          if (buttonType == ButtonType.OK && nameField.text.value.trim.nonEmpty) then
            new Category(nameField.text.value, colorPicker.value.value)
          else
            null

        val result = dialog.showAndWait()

        result match
          case Some(newCategory: Category) =>
            readyCategories += newCategory
            categoryFilters(newCategory.name) = true
            refresh()
            rebuildSidebar()
          case _ =>

      val buttonAddReminder = new Button("Add Reminder"):
        onAction = _ => showAddReminderDialog()

      val sidebarAddCategory = new VBox(10):
        layoutX = 60
        layoutY = 20
        children = Seq(buttonAddCategory, buttonAddReminder)


    // ability to search events eg. certain people or course names etc.
      val searchField = new TextField:
        promptText = "Search"
        layoutX = 50
        layoutY = 20

      val searchResults = new ListView[String]:
        maxHeight = 200
        maxWidth = 200

      searchField.text.onChange{(_, _, newValue) =>
        if (newValue.trim.isEmpty) then
          searchResults.items = ObservableBuffer.empty[String]
        else
          val results = manager.search(newValue).collect{case e: Event => e}
          searchResults.items = ObservableBuffer.from(results.map(e =>
            s"${e.startDateTime.format(DateTimeFormatter.ofPattern("dd.MM HH:mm"))} - ${e.title}"))
      }

      val searchContainer = new VBox(5):
        layoutX = 50
        layoutY = 620
        children = Seq(searchField, searchResults)

    dailyView.visible = false

    root.children.addAll(controlPanel, stack, sidebarWeek, sidebarDay, sidebarNextWeek, sidebarPreviousWeek, sidebarEvent, sidebarCategory, sidebarAddCategory, searchContainer)


  end start

end Main

