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


  var dragStart: Option[LocalDateTime] = None
  val selectionRect = new Rectangle:
    fill = LightBlue.opacity(0.3)
    stroke = Blue
    strokeWidth = 1
    visible = false
    mouseTransparent = true

  val readyCategories = ObservableBuffer[Category](new Category("Study", Blue), new Category("Work", Orange), new Category("Hobby", Pink), new Category("Holiday", Red), new Category("Other", Gray))
  val categoryFilters = mutable.Map[String, Boolean]()
  readyCategories.foreach(category => categoryFilters(category.name) = true)

  var currentMonday = DateUtils.mondayOf(LocalDateTime.now())
  var currentDay = DateUtils.startOfDay(LocalDateTime.now())


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

    val weeklyView = new Pane():
      background = null

    val dailyView = new Pane():
      background = null

    updateWeeklyView(weeklyView)

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
          updateWeeklyView(weeklyView)
          updateDailyView(dailyView)
        case Some(ButtonType.OK) =>
          event.updateDetails(nameField.text.value, descField.text.value, locField.text.value)
          manager.updateEntry(event)
          updateWeeklyView(weeklyView)
          updateDailyView(dailyView)
        case _ =>


    // function, that enables updating of calendar info when moving forward or backwards in time (weeks)
    def updateWeeklyView(view: Pane): Unit =
      weeklyView.children.clear()

      val weekly = new Rectangle:
        x = 0
        y = 0
        width = 1100
        height = 800
        fill = White

      weeklyView.children += weekly
      weeklyView.children.add(selectionRect)

      val weeklyDivision = 7
      val columnWidth = 1100 / weeklyDivision
      // Divide the weekyl view into seven parts
      for (i <- 1 until weeklyDivision)
        val lineX = (i * columnWidth)
        val divider = new Line:
          startX = lineX
          startY = 0
          endX = lineX
          endY = 800
          stroke = Black
          strokeWidth = 2
        weeklyView.children += divider

      // Add a horizontal divider
      val horizontalDivider = new Line:
       startX = 0
       startY = 100
       endX = 1100
       endY = 100
       stroke = Black
       strokeWidth = 2
      weeklyView.children += horizontalDivider

      // Add names of the days to the weekly view
      val days = List("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
      for (i <- 0 until 7)
        val columnStartX = i * columnWidth
        val columnCenterX = columnStartX + (columnWidth / 2)
        val currentDay = currentMonday.plusDays(i)
        val dateString = currentDay.format(DateTimeFormatter.ofPattern("dd.MM."))

        val holidayName = manager.getHolidayName(currentDay.toLocalDate).getOrElse("")

        val day = new Text:
          text = days(i)
          font = Font.font("Arial", FontWeight.Bold, 20)
          fill = Black
          x = columnCenterX - 50
          y = 80

        val holidayEntry = manager.search("").collectFirst {
          case e: Event if e.category.name == "Holiday" && e.startDateTime.toLocalDate == currentDay.toLocalDate => e
        }

        val holidayText = new Text:
          text = holidayName
          font = Font.font("Arial", FontWeight.Bold, 12)
          fill = Red
          x = columnStartX + 5
          y = 50
          wrappingWidth = columnWidth - 10
          textAlignment = TextAlignment.Center
          onMouseClicked = (e: MouseEvent) => {
            holidayEntry.foreach(h => ShowEditEventDialog(h))
          }

        val dateText = new Text:
          text = dateString
          font = Font.font("Arial", FontWeight.Bold, 20)
          fill = Black
          x = columnCenterX- 60
          y = 20
        weeklyView.children.addAll(day, holidayText, dateText)

      // Divide the weekly view into 24 parts (hours)
      for (i <- 1 until 24)
        val lineY = 100 + (i * (700.0/24.0))
        val dividerHours = new Line:
          startX = 0
          startY = lineY
          endX = 1100
          endY = lineY
          stroke = Black
          strokeWidth = 2
        weeklyView.children += dividerHours

      // Add hours of the day
      for (i <- 0 to 23)
        val hour = new Text:
          text = s"$i:00"
          font = Font.font("Arial", FontWeight.Bold, 10)
          fill = Black
          x = -30
          y = (100 + (i * (700.0/24.0))) + 3
        weeklyView.children += hour

      // Add new event by painting the desired hours
      weekly.onMousePressed = (e: MouseEvent) =>
        val clickedRow = ((e.y-100) / (700.0/24.0)).toInt
        val dayIndex = (e.x / columnWidth).toInt

        if (clickedRow >= 0 && clickedRow < 24)
          dragStart = Some(currentMonday.plusDays(dayIndex).withHour(clickedRow))
          selectionRect.x = dayIndex * columnWidth
          selectionRect.y = 100 + (clickedRow* (700.0 / 24.0))
          selectionRect.width = columnWidth
          selectionRect.height = (700.0 / 24.0)
          selectionRect.visible = true

      // Show what is being painted
      weekly.onMouseDragged = (e: MouseEvent) =>
        val clickedRow = ((e.y - 100) / (700.0 / 24.0)).toInt
        dragStart.foreach(start =>
          val startRow = ((selectionRect.y.value - 100) / (700.0 / 24.0)).toInt
          val currentRow =
            if clickedRow < 0
              then 0
            else if clickedRow > 23
              then 23
            else
              clickedRow

          val topRow = Math.min(start.getHour, currentRow)
          val bottomRow = Math.max(start.getHour, currentRow)

          selectionRect.y = 100 + (topRow * (700.0/ 24.0))
          selectionRect.height = (bottomRow-topRow + 1) * (700.0 / 24.0))


      weekly.onMouseReleased = (e: MouseEvent) =>
        selectionRect.visible = false
        val dayIndex = (e.x / columnWidth).toInt
        val releasedRow = ((e.y-100) / (700.0/24.0)).toInt

        (dragStart, releasedRow) match
          case (Some(start), relRow) if relRow >= 0 && relRow < 24 =>
            val end = currentMonday.plusDays(dayIndex).withHour(relRow + 1)
            val actualStart = if start.isBefore(end) then start else end.minusHours(1)
            val actualEnd = if start.isBefore(end) then end else start.plusHours(1)
            val duration = java.time.Duration.between(actualStart, actualEnd).toHours.toInt
            showAddEventDialog(actualStart, duration)
          case _ => dragStart = None

      val weekEnd = currentMonday.plusDays(7)
      val weeklyEvents = manager.search("").collect{case e: Event => e}.
        filter(e => e.startDateTime.isBefore(weekEnd) &&
          e.endDateTime.isAfter(currentMonday) &&
          categoryFilters.getOrElse(e.category.name, true) &&
        e.category.name != "Holiday")

      for (dayOffset <- 0 until 7)
        val targetDayStart = currentMonday.plusDays(dayOffset)
        val targetDayEnd = targetDayStart.plusDays(1)

        // events visible on this day, clipped to the day's bounds
        val dayItems = weeklyEvents.collect {
          case event if event.startDateTime.isBefore(targetDayEnd) && event.endDateTime.isAfter(targetDayStart) =>
            val visibleStart =
              if event.startDateTime.isBefore(targetDayStart) then targetDayStart else event.startDateTime
            val visibleEnd =
              if event.endDateTime.isAfter(targetDayEnd) then targetDayEnd else event.endDateTime
            (event, visibleStart, visibleEnd)
        }

        for ((event, visibleStart, visibleEnd, col, total) <- EventLayout.layoutColumns(dayItems))
          val startHour = visibleStart.getHour
          val startMinute = visibleStart.getMinute
          val durationHours = java.time.Duration.between(visibleStart, visibleEnd).toMinutes / 60.0

          val slotWidth = columnWidth.toDouble / total
          val x = dayOffset * columnWidth + col * slotWidth
          val y = (100+ (startHour * (700.00/24.00)) + (startMinute / 60.00 * (700.00/24.00))) - 3
          val rectangleHeight = durationHours * (700.00/ 24.00)

          val eventRectangle = new StackPane:
            layoutX = x
            layoutY = y
            prefWidth = slotWidth
            prefHeight = rectangleHeight
            children = Seq(
              new Rectangle:
                width = slotWidth
                height = rectangleHeight
                fill = event.category.color
                arcWidth = 10
                arcHeight = 10,
              new Text:
                text = s"${event.title}"
                font = Font.font("Arial", FontWeight.Bold, 12)
                fill = White
                )
          weeklyView.children.add(eventRectangle)
          eventRectangle.onMouseClicked = (e: MouseEvent) =>
            ShowEditEventDialog(event)


    // function, that enables updating of calendar info when moving forward or backwards in time (days)
    def updateDailyView(view: Pane): Unit =
      dailyView.children.clear()
      val columnWidth = 1100 / 7
       //Daily view
      val daily = new Rectangle:
        x = 0
        y = 0
        width = 1100
        height = 800
        fill = White
      dailyView.children.add(daily)
      dailyView.children.add(selectionRect)

      val dailyDivision = 24
      val rowWidth = 800 / dailyDivision

      // Divide the daily view into 24 parts (hours)
      for (i <- 1 until dailyDivision)
        val lineY = (i * rowWidth)
        val divider2 = new Line:
          startX = 0
          startY = lineY
          endX = 1100
          endY = lineY
          stroke = Black
          strokeWidth = 2
        dailyView.children += divider2

      // Add a vertical divider
      val verticalDivider = new Line:
         startX = 55
         startY = 0
         endX = 55
         endY = 800
         stroke = Black
         strokeWidth = 2

      dailyView.children += verticalDivider

      // Add 24 hours to the daily view
      for (i <- 0 to 23)
        val hour = new Text:
          text = s"$i:00"
          font = Font.font("Arial", FontWeight.Bold, 14)
          fill = Black
          x = 10
          y = i * rowWidth + 20
        dailyView.children += hour

      // Add current date to the daily view
      val formatter = DateTimeFormatter.ofPattern("EEEE dd.MM.yyyy", Locale.ENGLISH)
      val writeDate = new Text:
          text = currentDay.format(formatter)
          font = Font.font("Arial", FontWeight.Bold, 30)
          fill = White
          x = 400
          y = -10
        dailyView.children += writeDate

      val holidayName = manager.getHolidayName(currentDay.toLocalDate).getOrElse("")

      val holidayEntry = manager.search("").collectFirst {
        case e: Event if e.category.name == "Holiday" && e.startDateTime.toLocalDate == currentDay.toLocalDate => e
      }

      val holidayText = new Text:
          text = holidayName
          font = Font.font("Arial", FontWeight.Bold, 20)
          fill = Red
          x = 700
          y = -12
          onMouseClicked = (e: MouseEvent) => {
              holidayEntry.foreach(h => ShowEditEventDialog(h))
            }
        dailyView.children += holidayText

      // Add new event by painting the desired hours
      daily.onMousePressed = (e: MouseEvent) =>
        val hourHeight = 800.0 / 24.0
        val clickedRow = (e.y / hourHeight).toInt

        if (clickedRow >= 0 && clickedRow < 24)
          dragStart = Some(currentDay.withHour(clickedRow))
          selectionRect.x = 60
          selectionRect.y = clickedRow * hourHeight
          selectionRect.width = 1000
          selectionRect.height = hourHeight
          selectionRect.visible = true

      // Show what is being painted
      daily.onMouseDragged = (e: MouseEvent) =>
        val hourHeight = 800.0 / 24.0
        val clickedRow = (e.y / hourHeight).toInt
        dragStart.foreach(start =>
          val currentRow =
            if clickedRow < 0
              then 0
            else if clickedRow > 23
              then 23
            else
              clickedRow

          val topRow = Math.min(start.getHour, currentRow)
          val bottomRow = Math.max(start.getHour, currentRow)

          selectionRect.y = topRow * hourHeight
          selectionRect.height = (bottomRow-topRow + 1) * hourHeight)

      daily.onMouseReleased = (e: MouseEvent) =>
        selectionRect.visible = false
        val hourHeight = 800.0 / 24.0
        val releasedRow = (e.y / hourHeight).toInt

        (dragStart, releasedRow) match
          case (Some(start), relRow) if relRow >= 0 && relRow < 24 =>
            val end = currentDay.withHour(relRow + 1)
            val actualStart = if start.isBefore(end) then start else end.minusHours(1)
            val actualEnd = if start.isBefore(end) then end else start.plusHours(1)
            val duration = java.time.Duration.between(actualStart, actualEnd).toHours.toInt
            showAddEventDialog(actualStart, duration)
          case _ => dragStart = None

      val dayEnd = currentDay.plusDays(1)
      val dailyEvents = manager.search("").collect{case e: Event => e}.
        filter(e => e.startDateTime.isBefore(dayEnd) &&
          e.endDateTime.isAfter(currentDay) &&
          categoryFilters.getOrElse(e.category.name, true) &&
        e.category.name != "Holiday")

      // events clipped to the current day's bounds
      val dailyItems = dailyEvents.map { event =>
        val visibleStart =
          if event.startDateTime.isBefore(currentDay) then currentDay else event.startDateTime
        val visibleEnd =
          if event.endDateTime.isAfter(dayEnd) then dayEnd else event.endDateTime
        (event, visibleStart, visibleEnd)
      }

      val hourHeight = 800.0 / 24.0
      val dailyWidth = 1000.0

      for ((event, visibleStart, visibleEnd, col, total) <- EventLayout.layoutColumns(dailyItems))
        val startHour = visibleStart.getHour
        val startMinute = visibleStart.getMinute
        val durationHours = java.time.Duration.between(visibleStart, visibleEnd).toMinutes / 60.0

        val slotWidth = dailyWidth / total
        val x = 60 + col * slotWidth
        val y = ((startHour * hourHeight) + (startMinute / 60.00 * hourHeight))-2
        val rectangleHeight = durationHours * hourHeight

        val eventRectangle = new StackPane:
          layoutX = x
          layoutY = y
          prefWidth = slotWidth
          prefHeight = rectangleHeight
          children = Seq(
            new Rectangle:
              width = slotWidth
              height = rectangleHeight
              fill = event.category.color
              arcWidth = 10
              arcHeight = 10,
            new Text:
              text = s"${event.title}"
              font = Font.font("Arial", FontWeight.Bold, 12)
              fill = White
              )
        dailyView.children.add(eventRectangle)
        eventRectangle.onMouseClicked = (e: MouseEvent)
          => ShowEditEventDialog(event)


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
            currentDay = DateUtils.startOfDay(event.startDateTime)
            currentMonday = DateUtils.mondayOf(event.startDateTime)
            updateWeeklyView(weeklyView)
            updateDailyView(dailyView)
        case _ => 


    updateWeeklyView(weeklyView)
    updateDailyView(dailyView)


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
      if weeklyView.isVisible then
        currentMonday = currentMonday.plusDays(7)
        updateWeeklyView(weeklyView)
      else
        currentDay = currentDay.plusDays(1)
        updateDailyView(dailyView)

    val sidebarNextWeek = new VBox(10):
      layoutX = 450
      layoutY = 20
      children = buttonNext

    // button to switch to the previous day/week
    val buttonPrevious = new Button("Previous")
      buttonPrevious.onAction = _ =>
      if weeklyView.isVisible then
        currentMonday = currentMonday.minusDays(7)
        updateWeeklyView(weeklyView)
      else
        currentDay = currentDay.minusDays(1)
        updateDailyView(dailyView)

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
              updateWeeklyView(weeklyView)
              updateDailyView(dailyView)

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
            updateWeeklyView(weeklyView)
            updateDailyView(dailyView)
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

