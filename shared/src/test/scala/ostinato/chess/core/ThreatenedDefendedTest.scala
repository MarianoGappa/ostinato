package ostinato.chess.core

import org.scalatest._

class ThreatenedDefendedTest extends FunSpec with Matchers {
  describe("ChessGame threatened/defended pieces") {
    it("should find no actions for King if it's threatened in every direction") {
      val game = ChessGame.fromGridString(
        """..♛.....
          |........
          |♛.......
          |...♔....
          |.......♛
          |........
          |........
          |....♛...""".stripMargin, turn = WhiteChessPlayer).get

      game.board.kings.head.actions(game.board) shouldBe Set()
    }
    it("should find that the Queen is defended") {
      val game = ChessGame.fromGridString(
        """..♛.....
          |........
          |..♛.....
          |........
          |........
          |........
          |........
          |........""".stripMargin).get

      game.board.queens.head.isDefended(game.board) shouldBe true
    }

    it("should find that the Queen is threatened") {
      val game = ChessGame.fromGridString(
        """..♖.....
          |........
          |..♛.....
          |........
          |........
          |........
          |........
          |........""".stripMargin).get

      game.board.queens.head.isThreatened(game.board) shouldBe true
    }

    it("should find that the Queen is not threatened") {
      val game = ChessGame.fromGridString(
        """...♖....
          |........
          |..♛.....
          |........
          |........
          |........
          |........
          |........""".stripMargin).get

      game.board.queens.head.isThreatened(game.board) shouldBe false
    }

    it("should find that the Queen is threatened by Pawn") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |..♛.....
          |.♙......
          |........
          |........
          |........
          |........""".stripMargin).get

      game.board.queens.head.isThreatened(game.board) shouldBe true
    }

    it("should find that the Knight is NOT threatened by Pawn") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |........
          |.♙......
          |..♞.....
          |........
          |........
          |........""".stripMargin).get

      game.board.knights.head.isThreatened(game.board) shouldBe false
    }

    it("should find that the King is threatened by Knight") {
      val game = ChessGame.fromGridString(
        """..♘.....
          |♚.......
          |.......♖
          |...♗....
          |........
          |........
          |........
          |♘♖.....♔""".stripMargin).get

      game.board.kings.head.isThreatened(game.board) shouldBe true
    }
  }
}
