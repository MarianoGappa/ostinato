package boardgame.core

abstract class Game[B <: Board[_,_,_,_], P <: Player[B,_,_, _]](board: B, players: List[P], rules: Rules)

abstract class Board[P <: Piece[_,_,_,_], M <: Movement[P], B <: Board[P,M,_,_], R <: Rules](val grid: Vector[Option[P]]) {
  type Cell = Option[P]
  type Location = Option[Cell]

  def get(point: Point)(implicit boardSize: BoardSize): Location = if (point.exists) Some(grid(point.toPos)) else None
  def isPiece(l: Location): Boolean = l.flatten.nonEmpty
  def isEmptyCell(l: Location): Boolean = l.nonEmpty && l.flatten.isEmpty
  def pieces = grid.flatten

  protected def between(from: Point, to: Point)(implicit boardSize: BoardSize): Set[Location] = {
    val distance = from.distance(to)
    val delta = from.sign(to)

    if ((delta.x != 0 && delta.y != 0 && distance.x == distance.y && distance.x >= 2) ||
      (delta.x == 0 && delta.y != 0 && distance.y >= 2) ||
      (delta.x != 0 && delta.y == 0 && distance.x >= 2))
      betweenInclusive(from + delta, to - delta, delta)
    else
      Set()
  }

  private def betweenInclusive(from: Point, to: Point, delta: Point)(implicit boardSize: BoardSize): Set[Location] = {
    if (from == to)
      Set(get(from))
    else
      Set(get(from)) ++ betweenInclusive(from + delta, to, delta)
  }

  def movement(from: Point, delta: Point)(implicit rules: R): Option[M]

  def move(m: M)(implicit rules: R): B
}

object Piece {
  def pointsOf(points: Set[(Int, Int)]): Set[Point] = points map (p => Point(p._1, p._2))
}

abstract class Piece[P <: Player[B,M, _, _], M <: Movement[_], B <: Board[_,M,B,R], R <: Rules](val point: Point, val owner: P) {
  def movements(board: B)(implicit rules: R): Set[M]

  protected def allMovementsOfDelta(from: Point, delta: Point, board: B, inc: Int = 1)(implicit rules: R): Set[M] = {
    Set.empty[M]
    val a: Option[M] = board.movement(from, delta * inc)
    val b: Option[Set[M]] = a map { m: M ⇒ Set(m) ++ allMovementsOfDelta(from, delta, board, inc + 1) }
    val c: Set[M] = b getOrElse Set.empty[M]
    c
  }

  protected def movementOfDelta(from: Point, delta: Point, board: B)(implicit rules: R): Option[M] = {
    board.movement(from, delta)
  }
}

class Movement[P <: Piece[_,_,_,_]](fromPiece: P, delta: Point)

// TODO either implement movements or remove the M type parameter
abstract class Player[B <: Board[P,_,_,_], M <: Movement[_], P <: Piece[PL,_,_,_], PL <: Player[_,_,_,_]](val name: String) {
  def equals(that: Player[_,_,_,_]): Boolean // TODO wtf why doesn't this return a PL

  def pieces(board: B): Set[P] = {
    board.pieces.filter { a: P => a.owner.equals(this)}.toSet
  }

}

class Rules {}

object Point {
  def fromPos(pos: Int)(implicit boardSize: BoardSize) = {
    Point(pos % boardSize.x, pos / boardSize.x)
  }
}

case class Point(x: Int, y: Int) {
  def toPos(implicit boardSize: BoardSize): Int = y * boardSize.x + x
  def +(that: Point) = Point(x + that.x, y + that.y)
  def -(that: Point) = Point(x - that.x, y - that.y)
  def *(factor: Int) = Point(x * factor, y * factor)
  def exists(implicit boardSize: BoardSize) = x >= 0 && y >= 0 && x < boardSize.x && y < boardSize.y
  lazy val sign = Point(math.signum(x), math.signum(y))
  lazy val abs = Point(math.abs(x), math.abs(y))
  def distance(that: Point) = (this - that).abs
  def sign(that: Point): Point = (that - this).sign
}

case class BoardSize(x: Int, y: Int)
