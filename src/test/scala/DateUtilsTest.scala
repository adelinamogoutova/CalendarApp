import java.time.LocalDateTime

class DateUtilsTest extends munit.FunSuite:

  test("startOfDay zeroes the hour and minute"):
    val result = DateUtils.startOfDay(LocalDateTime.of(2026, 6, 24, 14, 37))
    assertEquals(result.getHour, 0)
    assertEquals(result.getMinute, 0)
    // the calendar day itself is preserved
    assertEquals(result.toLocalDate, LocalDateTime.of(2026, 6, 24, 0, 0).toLocalDate)

  test("mondayOf returns the Monday of the same week at midnight"):
    // 2026-06-24 is a Wednesday; the Monday of that week is 2026-06-22
    val result = DateUtils.mondayOf(LocalDateTime.of(2026, 6, 24, 14, 37))
    assertEquals(result, LocalDateTime.of(2026, 6, 22, 0, 0))

  test("mondayOf is stable when given a Monday"):
    val monday = LocalDateTime.of(2026, 6, 22, 9, 15)
    assertEquals(DateUtils.mondayOf(monday), LocalDateTime.of(2026, 6, 22, 0, 0))

  test("mondayOf handles Sunday (end of week)"):
    // 2026-06-28 is a Sunday; its week still starts on Monday 2026-06-22
    val sunday = LocalDateTime.of(2026, 6, 28, 23, 0)
    assertEquals(DateUtils.mondayOf(sunday), LocalDateTime.of(2026, 6, 22, 0, 0))
