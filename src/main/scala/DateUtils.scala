import java.time.LocalDateTime

object DateUtils:

  // midnight of the given day
  def startOfDay(dateTime: LocalDateTime): LocalDateTime =
    dateTime.withHour(0).withMinute(0)

  // midnight of the Monday in the same week as the given day
  def mondayOf(dateTime: LocalDateTime): LocalDateTime =
    val dayOfWeek = dateTime.getDayOfWeek.getValue
    startOfDay(dateTime.minusDays(dayOfWeek - 1))

end DateUtils
