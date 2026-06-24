import scalafx.collections.ObservableBuffer
import scala.jdk.CollectionConverters.*
import scala.jdk.OptionConverters.*
import java.time.{LocalDate, LocalDateTime}
import scala.collection.mutable.*
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.data.CalendarBuilder

import java.io.*


class CalendarManager:

  private val allEntries = ArrayBuffer[CalendarEntry]()
  private val fileName = "calendar.ics"

  // saves events to file
  def saveToFile(): Unit =
    val icsCalendar = new net.fortuna.ical4j.model.Calendar()
    icsCalendar.add(new net.fortuna.ical4j.model.property.ProdId("-//OmaKalenteri//FI"))
    icsCalendar.add(new net.fortuna.ical4j.model.property.Version("2.0", "2.0"))

    allEntries.foreach {
      case e: Event =>
        val vEvent = new net.fortuna.ical4j.model.component.VEvent(e.startDateTime, e.endDateTime, e.title)
        vEvent.add(new net.fortuna.ical4j.model.property.Description(e.description))
        vEvent.add(new net.fortuna.ical4j.model.property.Categories(e.category.name))
        val _: Any = icsCalendar.add(vEvent)
      case _ => 
    }

    val out = new java.io.FileOutputStream(fileName)
    new net.fortuna.ical4j.data.CalendarOutputter().output(icsCalendar, out)
    out.close()


  // loads saved events from file
  def loadFromFile(path: String, readyCategories: ObservableBuffer[Category]): Unit =
    val file = new File(path)
    if (!file.exists()) return

    val in = new FileInputStream(file)
    val calendar = new CalendarBuilder().build(in)

    val components = calendar.getComponents[VEvent]("VEVENT").asScala

    for (ve <- components)
      val summary = ve.getSummary.toScala.map(_.getValue).getOrElse("")
      val desc = ve.getDescription.toScala.map(_.getValue).getOrElse("")
      val start = ve.getStartDate.toScala.map { d => d.getDate match
        case ldt: LocalDateTime => ldt
        case ld: LocalDate => ld.atStartOfDay()
        case _  => LocalDateTime.now()  }.getOrElse(LocalDateTime.now())
      val end = ve.getEndDate.toScala.map { d => d.getDate match
          case ldt: LocalDateTime => ldt
          case ld: LocalDate => ld.atStartOfDay().plusDays(1)
          case _  => start.plusHours(1)}.getOrElse(start.plusHours(1))

      val categName = ve.getCategories.toScala.map(_.getValue).getOrElse("Other")
      val categ =
        if (path=="holidays.ics") then
          readyCategories.find(_.name == "Holiday").getOrElse(readyCategories.head)
        else
          val categName = ve.getCategories.toScala.map(_.getValue).getOrElse("Other")
          readyCategories.find(_.name == categName).getOrElse(readyCategories.last)
      allEntries += new Event(summary, desc, "", categ, start, end)
    in.close()

  private val categories = Map[String, Category]()

  def addEntry(entry: CalendarEntry): Unit =
    allEntries += entry
    saveToFile()

  def removeEntry(entry: CalendarEntry): Unit =
    entry match
      case e: Event =>
        allEntries.filterInPlace{
          case existing: Event =>
            !(existing.title == e.title &&
              existing.startDateTime == e.startDateTime &&
              existing.category.name == e.category.name)
          case _ => true
        }

      case _ => allEntries -= entry

    saveToFile()

  def updateEntry(entry: CalendarEntry): Unit =
    saveToFile()

  def getEntriesByPeriod(start: LocalDateTime, end: LocalDateTime): List[CalendarEntry] =
    allEntries.filter {
      case e: Event =>
        (e.startDateTime.isAfter(start) || e.startDateTime.isEqual(start)) && e.startDateTime.isBefore(end)
      case _ => false}.toList

  def search(query: String): List[CalendarEntry] =
    allEntries.filter {entry =>
      entry.title.toLowerCase.contains(query.toLowerCase) || (entry match {
        case e: Event =>
          e.description.toLowerCase.contains(query.toLowerCase) ||
          e.location.toLowerCase.contains(query.toLowerCase)
        case _ => false})
    }.toList

  def setCategoryVisibility(category: Category, visible: Boolean): Unit =
    category.isVisible = visible

  def getHolidayName(date: LocalDate): Option[String] =
    allEntries.collectFirst {
      case e: Event if e.category.name == "Holiday" && e.startDateTime.toLocalDate == date =>
        e.title}


  def clearAll(): Unit = allEntries.clear()


end CalendarManager