package ostinato.chess.core

import org.scalatest._
import ostinato.core.XY

class DrawTest extends FunSpec with Matchers {
  describe("Board Insufficient material") {
    it("should find insufficient material on the Kk case") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |........
          |...♔....
          |........
          |....♚...
          |........
          |........""".stripMargin).get

      game.board.hasInsufficientMaterial shouldBe true
    }
    it("should find insufficient material on the KBk case") {
      val game = ChessGame.fromGridString(
        """........
          |.....♗..
          |........
          |...♔....
          |........
          |....♚...
          |........
          |........""".stripMargin).get

      game.board.hasInsufficientMaterial shouldBe true
    }
    it("should find insufficient material on the Kkb case") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |........
          |...♔....
          |........
          |....♚...
          |..♝.....
          |........""".stripMargin).get

      game.board.hasInsufficientMaterial shouldBe true
    }
    it("should find insufficient material on the KNk case") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |......♘.
          |...♔....
          |........
          |....♚...
          |........
          |........""".stripMargin).get

      game.board.hasInsufficientMaterial shouldBe true
    }
    it("should find insufficient material on the Kkn case") {
      val game = ChessGame.fromGridString(
        """........
          |........
          |........
          |...♔....
          |........
          |....♚...
          |........
          |.♞......""".stripMargin).get

      game.board.hasInsufficientMaterial shouldBe true
    }
    it("should find insufficient material on the KBkb case") {
      val game = ChessGame.fromGridString(
        """♝.♝.♝.♝.
          |.♝.♝.♝.♝
          |♗.♗.♗.♗.
          |.♗.♔.♗.♗
          |..♗.....
          |....♚...
          |........
          |........""".stripMargin).get

      game.board.hasInsufficientMaterial shouldBe true
    }
    it("should find Draw based on insufficient material") {
      val game = ChessGame.fromGridString(
        """♝.♝.♝.♝.
          |.♝.♝.♝.♝
          |♗.♗.♗.♗.
          |.♗.♔.♗.♗
          |..♗.....
          |....♚...
          |........
          |........""".stripMargin).get

      game.board.actions shouldBe Set(DrawAction(WhiteChessPlayer))
    }
  }
  describe("Fifty move rule") {
    it("should calculate the fifty-move rule properly") {
      def firstMoveAction(as: Set[ChessAction]) = as.collect { case a: MoveAction ⇒ a }.head

      def doFirstKnightMoveAction(n: Int, board: ChessBoard): ChessBoard =
        if (n == 0)
          board
        else
          doFirstKnightMoveAction(n - 1, board.doAction(firstMoveAction(board.knights.flatMap(_.actions(board)).toSet)).get)

      doFirstKnightMoveAction(50, ChessGame.defaultGame.board).isInFiftyMoveRule shouldBe true
    }
  }
  describe("Threefold repetition rule") {
    it("should calculate the threefold repetition rule properly") {
      val board =
        ChessGame.fromGridString(
          """♜.......
            |........
            |........
            |...♔....
            |........
            |.....♚..
            |........
            |.......♖""".stripMargin, turn = WhiteChessPlayer).get.board

      val whiteLeft = MoveAction(♚(XY(3, 3), WhiteChessPlayer), XY(-1, 0))
      val whiteRight = MoveAction(♚(XY(2, 3), WhiteChessPlayer), XY(1, 0))
      val blackLeft = MoveAction(♚(XY(5, 5), BlackChessPlayer), XY(-1, 0))
      val blackRight = MoveAction(♚(XY(4, 5), BlackChessPlayer), XY(1, 0))

      board.
        doAction(whiteLeft).get.
        doAction(blackLeft).get.
        doAction(whiteRight).get.
        doAction(blackRight).get.
        doAction(whiteLeft).get.
        doAction(blackLeft).get.
        doAction(whiteRight).get.
        doAction(blackRight).get.
        isInThreefoldRepetition shouldBe true
    }
  }
  describe("Stalemate rule") {
    it("should find stalemate") {
      ChessGame.fromGridString(
        """..♛.....
          |........
          |♛.......
          |...♔....
          |.......♛
          |........
          |........
          |....♛...""".stripMargin, turn = WhiteChessPlayer).get.board.isInStalemate shouldBe true
    }
  }
}