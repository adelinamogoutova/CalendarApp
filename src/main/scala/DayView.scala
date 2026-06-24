import scalafx.scene.layout.{Pane, StackPane}
import scalafx.scene.shape.*
import scalafx.scene.paint.Color.*
import scalafx.scene.input.MouseEvent
import scalafx.scene.text.*
import scalafx.Includes.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.collection.mutable

// Renders a single day split into 24 hourly rows and handles event
// creation/editing within it. Owns the currently displayed day.

class DayView(
  manager: CalendarManager,
  categoryFilters: mutable.Map[String, Boolean],
  selectionRect: Rectangle,
  onAddEvent: (LocalDateTime, Int) => Unit,
  onEditEvent: Event => Unit
):

  val pane: Pane = new Pane:
    background = null

  private var currentDay = DateUtils.startOfDay(LocalDateTime.now())
  private var dragStart: Option[LocalDateTime] = None

  def next(): Unit =
    currentDay = currentDay.plusDays(1)
    render()

  def previous(): Unit =
    currentDay = currentDay.minusDays(1)
    render()

  def goTo(date: LocalDateTime): Unit =
    currentDay = DateUtils.startOfDay(date)
    render()

  def render(): Unit =
      pane.children.clear()
      val columnWidth = 1100 / 7
       //Daily view
      val daily = new Rectangle:
        x = 0
        y = 0
        width = 1100
        height = 800
        fill = White
      pane.children.add(daily)
      pane.children.add(selectionRect)

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
        pane.children += divider2

      // Add a vertical divider
      val verticalDivider = new Line:
         startX = 55
         startY = 0
         endX = 55
         endY = 800
         stroke = Black
         strokeWidth = 2

      pane.children += verticalDivider

      // Add 24 hours to the daily view
      for (i <- 0 to 23)
        val hour = new Text:
          text = s"$i:00"
          font = Font.font("Arial", FontWeight.Bold, 14)
          fill = Black
          x = 10
          y = i * rowWidth + 20
        pane.children += hour

      // Add current date to the daily view
      val formatter = DateTimeFormatter.ofPattern("EEEE dd.MM.yyyy", Locale.ENGLISH)
      val writeDate = new Text:
          text = currentDay.format(formatter)
          font = Font.font("Arial", FontWeight.Bold, 30)
          fill = White
          x = 400
          y = -10
        pane.children += writeDate

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
              holidayEntry.foreach(h => onEditEvent(h))
            }
        pane.children += holidayText

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
            onAddEvent(actualStart, duration)
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
        pane.children.add(eventRectangle)
        eventRectangle.onMouseClicked = (e: MouseEvent)
          => onEditEvent(event)

end DayView
