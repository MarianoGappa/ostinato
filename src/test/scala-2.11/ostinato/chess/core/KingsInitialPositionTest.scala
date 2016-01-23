package ostinato.chess.core

import org.scalatest.{ShouldMatchers, FunSpec}

class KingsInitialPositionTest extends FunSpec with ShouldMatchers {
  describe("King's initial position") {
    it("should determine that white king is in initial position, if white is on bottom") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |........
          |........
          |........
          |........
          |........
          |....♔...""".stripMargin)

      game.whitePlayer.kingPiece(game.board) match {
        case Some(k: ♚) => k.isInInitialPosition shouldBe true
        case _ => fail
      }
    }
    it("should determine that white king is NOT in initial position") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |........
          |........
          |........
          |........
          |....♔...
          |........""".stripMargin)

      game.whitePlayer.kingPiece(game.board) match {
        case Some(k: ♚) => k.isInInitialPosition shouldBe false
        case _ => fail
      }
    }
    it("should determine that white king is NOT in initial position if it's in black's initial position, if white is on bottom") {
      val game = ChessGame.fromGridString(
        """....♔...
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin)

      game.whitePlayer.kingPiece(game.board) match {
        case Some(k: ♚) => k.isInInitialPosition shouldBe false
        case _ => fail
      }
    }
    it("should determine that black king is in initial position, if black is on top") {
      val game = ChessGame.fromGridString(
        """....♚...
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin)

      game.blackPlayer.kingPiece(game.board) match {
        case Some(k: ♚) => k.isInInitialPosition shouldBe true
        case _ => fail
      }
    }
    it("should determine that black king is NOT in initial position, if black is on top") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |....♚...
          |........
          |........
          |........
          |........
          |........""".stripMargin)

      game.blackPlayer.kingPiece(game.board) match {
        case Some(k: ♚) => k.isInInitialPosition shouldBe false
        case _ => fail
      }
    }
    it("should determine that black king is NOT in initial position if it's in white's initial position, if black is on top") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |........
          |........
          |........
          |........
          |........
          |....♚...""".stripMargin)

      game.blackPlayer.kingPiece(game.board) match {
        case Some(k: ♚) => k.isInInitialPosition shouldBe false
        case _ => fail
      }
    }
  }
}
