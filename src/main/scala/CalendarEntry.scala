abstract class CalendarEntry(var title: String, var description: String,
                             var location: String, var category: Category):

  def validate(): Boolean =
    // the event title cannot be empty
    title != null && title.trim.nonEmpty

  def updateDetails(newTitle: String, newDescription: String, newLocation: String) =
    this.title = newTitle
    this.description = newDescription
    this.location = newLocation



