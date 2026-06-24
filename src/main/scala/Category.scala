import scalafx.scene.paint.Color

class Category(var name: String, var color: Color):

  var isVisible: Boolean = true

  def changeVisibility() =
    isVisible = !isVisible
