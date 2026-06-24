import java.time.*

class Event(title: String, description: String, location: String, category: Category,
  var startDateTime: LocalDateTime, var endDateTime: LocalDateTime)
  extends CalendarEntry(title, description, location, category):

  // checks that the event end time is after the start and the event title is there
  override def validate(): Boolean =
    val isTimeLogical = endDateTime.isAfter(startDateTime)
    val hasTitle = title.nonEmpty
    isTimeLogical && hasTitle

  def getDuration = java.time.Duration.between(startDateTime, endDateTime)

