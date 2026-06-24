import java.time.LocalDateTime
import scala.collection.mutable

object EventLayout:

  // assigns each event a column index and the total number of columns in its
  // overlap group, so events at the same time are drawn next to each other
  def layoutColumns(items: Seq[(Event, LocalDateTime, LocalDateTime)]): Seq[(Event, LocalDateTime, LocalDateTime, Int, Int)] =
    val sorted = items.sortBy(item => (item._2, item._3))
    val result = mutable.ArrayBuffer[(Event, LocalDateTime, LocalDateTime, Int, Int)]()
    var cluster = mutable.ArrayBuffer[(Event, LocalDateTime, LocalDateTime, Int)]()
    val columnEnds = mutable.ArrayBuffer[LocalDateTime]()
    var clusterMaxEnd: Option[LocalDateTime] = None

    def flushCluster(): Unit =
      val total = columnEnds.size
      cluster.foreach((ev, s, e, col) => result += ((ev, s, e, col, total)))
      cluster = mutable.ArrayBuffer()
      columnEnds.clear()
      clusterMaxEnd = None

    for ((ev, s, e) <- sorted)
      // a new event that starts after everything so far ends starts a new group
      if clusterMaxEnd.exists(maxEnd => !s.isBefore(maxEnd)) then
        flushCluster()
      // reuse the first column whose previous event has already ended
      val freeCol = columnEnds.indexWhere(end => !end.isAfter(s))
      val col =
        if freeCol == -1 then
          columnEnds += e
          columnEnds.size - 1
        else
          columnEnds(freeCol) = e
          freeCol
      cluster += ((ev, s, e, col))
      clusterMaxEnd = Some(clusterMaxEnd.filter(_.isAfter(e)).getOrElse(e))

    flushCluster()
    result.toSeq

end EventLayout
