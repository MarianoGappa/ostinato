package boardgame.chess.core

import boardgame.core._

object ChessGame {
  def fromString(string: String, rules: ChessRules = ChessRules.default): ChessGame = {
    val (white, black) = (WhiteChessPlayer, BlackChessPlayer)
    val charVector = string.split('\n').mkString.zipWithIndex.toVector
    val grid = charVector map {
      case ('♜', i) ⇒ Some(new ♜(XY.fromI(i), white))
      case ('♞', i) ⇒ Some(new ♞(XY.fromI(i), white))
      case ('♝', i) ⇒ Some(new ♝(XY.fromI(i), white))
      case ('♛', i) ⇒ Some(new ♛(XY.fromI(i), white))
      case ('♚', i) ⇒ Some(new ♚(XY.fromI(i), white))
      case ('♟', i) ⇒ Some(new ♟(XY.fromI(i), white, rules.whitePawnDirection))
      case ('♖', i) ⇒ Some(new ♜(XY.fromI(i), black))
      case ('♘', i) ⇒ Some(new ♞(XY.fromI(i), black))
      case ('♗', i) ⇒ Some(new ♝(XY.fromI(i), black))
      case ('♕', i) ⇒ Some(new ♛(XY.fromI(i), black))
      case ('♔', i) ⇒ Some(new ♚(XY.fromI(i), black))
      case ('♙', i) ⇒ Some(new ♟(XY.fromI(i), black, rules.whitePawnDirection * -1))
      case _        ⇒ None
    }

    val enPassantPawns = charVector flatMap {
      case ('↑', i) ⇒ EnPassantPawn.fromXYD(XY.fromI(i), XY(0, -1), grid)
      case ('↓', i) ⇒ EnPassantPawn.fromXYD(XY.fromI(i), XY(0, 1), grid)
      case _        ⇒ None
    }

    // TODO: headOption means keep only the first; this is incorrect: if there's 2 there's a problem!
    new ChessGame(new ChessBoard(grid, enPassantPawns.headOption), List(white, black), rules)
  }

  val defaultGame: ChessGame = fromString(
    """♜♞♝♛♚♝♞♜
      |♟♟♟♟♟♟♟♟
      |........
      |........
      |........
      |........
      |♙♙♙♙♙♙♙♙
      |♖♘♗♕♔♗♘♖
      |""".stripMargin)
}

class ChessGame(val board: ChessBoard, val players: List[ChessPlayer], val rules: ChessRules) extends Game[ChessBoard, ChessPlayer](board, players, rules) {
  def isGameOver(implicit rules: ChessRules): Boolean = isDraw || lossFor.nonEmpty
  def lossFor(implicit rules: ChessRules): Option[ChessPlayer] = players find (board.isLossFor(_) == true)
  def isDraw(implicit rules: ChessRules): Boolean = players exists board.isDrawFor
  val whitePlayer = players.filter(_ == WhiteChessPlayer).head
  val blackPlayer = players.filter(_ == BlackChessPlayer).head
}