import scalafx.scene.layout.{Pane, StackPane}
import scalafx.scene.shape.*
import scalafx.scene.paint.Color.*
import scalafx.scene.input.MouseEvent
import scalafx.scene.text.*
import scalafx.Includes.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import scala.collection.mutable

//Renders the seven-day week grid and handles event creation/editing within it.
//Owns the currently displayed week; navigation and refreshes re-render `pane`.
 
class WeekView(
  manager: CalendarManager,
  categoryFilters: mutable.Map[String, Boolean],
  selectionRect: Rectangle,
  onAddEvent: (LocalDateTime, Int) => Unit,
  onEditEvent: Event => Unit
):

  val pane: Pane = new Pane:
    background = null

  private var currentMonday = DateUtils.mondayOf(LocalDateTime.now())
  private var dragStart: Option[LocalDateTime] = None

  def next(): Unit =
    currentMonday = currentMonday.plusDays(7)
    render()

  def previous(): Unit =
    currentMonday = currentMonday.minusDays(7)
    render()

  def goTo(date: LocalDateTime): Unit =
    currentMonday = DateUtils.mondayOf(date)
    render()

  def render(): Unit =
      pane.children.clear()

      val weekly = new Rectangle:
        x = 0
        y = 0
        width = 1100
        height = 800
        fill = White

      pane.children += weekly
      pane.children.add(selectionRect)

      val weeklyDivision = 7
      val columnWidth = 1100 / weeklyDivision
      // Divide the weekly view into seven parts
      for (i <- 1 until weeklyDivision)
        val lineX = (i * columnWidth)
        val divider = new Line:
          startX = lineX
          startY = 0
          endX = lineX
          endY = 800
          stroke = Black
          strokeWidth = 2
        pane.children += divider

      // Add a horizontal divider
      val horizontalDivider = new Line:
       startX = 0
       startY = 100
       endX = 1100
       endY = 100
       stroke = Black
       strokeWidth = 2
      pane.children += horizontalDivider

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
            holidayEntry.foreach(h => onEditEvent(h))
          }

        val dateText = new Text:
          text = dateString
          font = Font.font("Arial", FontWeight.Bold, 20)
          fill = Black
          x = columnCenterX- 60
          y = 20
        pane.children.addAll(day, holidayText, dateText)

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
        pane.children += dividerHours

      // Add hours of the day
      for (i <- 0 to 23)
        val hour = new Text:
          text = s"$i:00"
          font = Font.font("Arial", FontWeight.Bold, 10)
          fill = Black
          x = -30
          y = (100 + (i * (700.0/24.0))) + 3
        pane.children += hour

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
            onAddEvent(actualStart, duration)
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
          pane.children.add(eventRectangle)
          eventRectangle.onMouseClicked = (e: MouseEvent) =>
            onEditEvent(event)

end WeekView
