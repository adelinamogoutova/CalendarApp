import java.time.LocalDateTime
import scalafx.scene.paint.Color

class EventLayoutTest extends munit.FunSuite:

  private val category = new Category("Test", Color.Blue)

  private def event(title: String, start: LocalDateTime, end: LocalDateTime): Event =
    new Event(title, "", "", category, start, end)

  private def at(hour: Int): LocalDateTime = LocalDateTime.of(2026, 6, 24, hour, 0)

  test("a single event occupies the only column"):
    val e = event("solo", at(9), at(10))
    val result = EventLayout.layoutColumns(Seq((e, e.startDateTime, e.endDateTime)))
    assertEquals(result.size, 1)
    val (_, _, _, col, total) = result.head
    assertEquals(col, 0)
    assertEquals(total, 1)

  test("non-overlapping events each get a full-width single column"):
    val a = event("a", at(9), at(10))
    val b = event("b", at(11), at(12))
    val result = EventLayout.layoutColumns(
      Seq((a, a.startDateTime, a.endDateTime), (b, b.startDateTime, b.endDateTime))
    )
    // both events stand alone, so each reports total = 1 column
    assert(result.forall((_, _, _, col, total) => col == 0 && total == 1))

  test("two overlapping events are placed in adjacent columns"):
    val a = event("a", at(9), at(11))
    val b = event("b", at(10), at(12))
    val result = EventLayout.layoutColumns(
      Seq((a, a.startDateTime, a.endDateTime), (b, b.startDateTime, b.endDateTime))
    )
    // both share an overlap group, so the group is two columns wide
    assert(result.forall((_, _, _, _, total) => total == 2))
    val columns = result.map((_, _, _, col, _) => col).toSet
    assertEquals(columns, Set(0, 1))
