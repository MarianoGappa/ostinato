package ostinato.core

import scala.annotation.tailrec
import scala.util.Random

abstract class Game[B <: Board[_, _, _, _], P <: Player[B, _, _, _]](val board: B, val players: List[P], val rules: Rules)

abstract class Board[P <: Piece[_, _, _, _, P], M <: Movement[P], B <: Board[P, M, _, _], R <: Rules](val grid: Vector[Option[P]]) {
  type Cell = Option[P]
  type Location = Option[Cell]

  def get(pos: XY)(implicit boardSize: BoardSize): Location = if (pos.exists) Some(grid(pos.toI)) else None
  def isPiece(l: Location): Boolean = l.flatten.nonEmpty
  def isEmptyCell(l: Location): Boolean = l.nonEmpty && l.flatten.isEmpty
  def pieces = grid.flatten
  def applyUpdate(grid: Vector[Option[P]], update: (Int, Option[P])) = grid.updated(update._1, update._2)

  protected def between(from: XY, to: XY)(implicit boardSize: BoardSize): Set[Location] = xyBetween(from, to) map get

  protected def xyBetween(from: XY, to: XY)(implicit boardSize: BoardSize): Set[XY] = {
    val distance = from.distance(to)
    val delta = from.sign(to)

    if ((delta.x != 0 && delta.y != 0 && distance.x == distance.y && distance.x >= 2) ||
      (delta.x == 0 && delta.y != 0 && distance.y >= 2) ||
      (delta.x != 0 && delta.y == 0 && distance.x >= 2))
      betweenInclusive(from + delta, to - delta, delta)
    else
      Set()
  }

  private def betweenInclusive(from: XY, to: XY, delta: XY)(implicit boardSize: BoardSize): Set[XY] =
    Set(from) ++ (if (from == to) Set() else betweenInclusive(from + delta, to, delta))

  def movements: Set[M]
  def movement(from: XY, delta: XY)(implicit rules: R): Set[M]
  def move(m: M)(implicit rules: R): B
}

object Piece {
  def toXYs(points: Set[(Int, Int)]): Set[XY] = points map (p ⇒ XY(p._1, p._2))
}

abstract class Piece[P <: Player[B, M, _, _], M <: Movement[_], B <: Board[_, M, B, R], R <: Rules, PC <: Piece[_, _, _, _, _]](val pos: XY, val owner: P) {
  def movements(board: B)(implicit rules: R): Set[M]
  def movedTo(pos: XY): PC // N.B. unsafe (doesn't check bounds)

  @tailrec
  private final def doAllMovementsOfDelta(from: XY, delta: XY, board: B, inc: Int = 1, acc: Set[M] = Set())(
    implicit rules: R): Set[M] =
    movementOfDelta(from, delta * inc, board) match {
      case ms if ms.isEmpty ⇒ acc
      case ms               ⇒ doAllMovementsOfDelta(from, delta, board, inc + 1, acc ++ ms)
    }

  // N.B. this proxy serves no purpose other than enabling the tailrec optimisation
  protected def allMovementsOfDelta(from: XY, delta: XY, board: B)(implicit rules: R) =
    doAllMovementsOfDelta(from, delta, board)

  protected def movementOfDelta(from: XY, delta: XY, board: B)(implicit rules: R): Set[M] =
    board.movement(from, delta)
}

abstract class Movement[P <: Piece[_, _, _, _, P]](fromPiece: P, delta: XY) {
  def gridUpdates: List[(Int, Option[P])]
}

abstract class Player[B <: Board[P, _, _, _], M <: Movement[_], P <: Piece[PL, _, _, _, P], PL <: Player[_, _, _, _]](val name: String) {
  def equals(that: PL): Boolean = { that.name == name }

  def pieces(board: B): Set[P] = {
    board.pieces.filter { a: P ⇒ a.owner.equals(this) }.toSet
  }

  def cantMoveMovement: M
}

class Rules {} // TODO this should be abstract... and thought about

object XY {
  def fromI(i: Int)(implicit boardSize: BoardSize) = {
    XY(i % boardSize.x, i / boardSize.x)
  }
}

case class XY(x: Int, y: Int) {
  def toI(implicit boardSize: BoardSize): Int = y * boardSize.x + x
  def +(that: XY) = XY(x + that.x, y + that.y)
  def -(that: XY) = XY(x - that.x, y - that.y)
  def *(factor: Int) = XY(x * factor, y * factor)
  def exists(implicit boardSize: BoardSize) = x >= 0 && y >= 0 && x < boardSize.x && y < boardSize.y
  lazy val sign = XY(math.signum(x), math.signum(y))
  lazy val abs = XY(math.abs(x), math.abs(y))
  def distance(that: XY) = (this - that).abs
  def chebyshevDistance(that: XY) = math.max(math.abs(this.x - that.x), math.abs(this.y - that.y))
  lazy val subtractXY = x - y
  def sign(that: XY): XY = (that - this).sign
}

case class BoardSize(x: Int, y: Int) // N.B. implementation defines an implicit BoardSize -> wouldn't make this a Point

abstract class Ai[B <: Board[_, _, _, _], P <: Player[_, M, _, _], G <: Game[_, _], M <: Movement[_]](player: P, seed: Option[Long] = None) {
  lazy val random = seed map (new Random(_)) getOrElse new Random()
  def move(game: G): M
  def cantMoveMovement: M = player.cantMoveMovement
}

abstract class RandomAi[B <: Board[_, M, _, _], P <: Player[_, M, _, _], G <: Game[B, _], M <: Movement[_]](player: P, seed: Option[Long] = None)
    extends Ai[B, P, G, M](player, seed) {

  def randomMovement(movements: Set[M]): Option[M] = random.shuffle(movements.toList).headOption
  def move(game: G): M = randomMovement(game.board.movements) getOrElse cantMoveMovement
}