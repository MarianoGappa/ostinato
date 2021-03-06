package ostinato.chess.core

import org.scalatest._
import ostinato.core.XY

class CastlingTest extends FunSpec with Matchers {
  describe("Castling") {
    it("should determine that black king can castle") {
      val game = ChessGame
        .fromGridString(
          """....♚..♜
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.exists {
        case m: CastlingAction ⇒ true
        case _ ⇒ false
      } shouldBe true
    }
    it(
      "should determine that black king can't castle because it's white's turn") {
      val game = ChessGame
        .fromGridString(
          """....♚..♜
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin,
          turn = WhiteChessPlayer
        )
        .get

      game.board.actions.forall {
        case m: CastlingAction ⇒ false
        case _ ⇒ true
      } shouldBe true
    }
    it(
      "should determine that black king can't castle because it's not in initial position") {
      val game = ChessGame
        .fromGridString(
          """...♚...♜
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.forall {
        case m: CastlingAction ⇒ false
        case _ ⇒ true
      } shouldBe true
    }
    it(
      "should determine that black king can't castle because target rook is not in initial position") {
      val game = ChessGame
        .fromGridString(
          """....♚.♜.
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.forall {
        case m: CastlingAction ⇒ false
        case _ ⇒ true
      } shouldBe true
    }
    it(
      "should determine that black king can't castle because black is on top unless otherwise specified") {
      val game = ChessGame
        .fromGridString(
          """........
          |........
          |........
          |........
          |........
          |........
          |........
          |....♚..♜""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.forall {
        case m: CastlingAction ⇒ false
        case _ ⇒ true
      } shouldBe true
    }
    it(
      "should determine that black king can't castle because the king is threatened") {
      val game = ChessGame
        .fromGridString(
          """....♚..♜
          |........
          |........
          |........
          |........
          |........
          |....♖...
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.exists {
        case m: CastlingAction ⇒ true
        case _ ⇒ false
      } shouldBe false
    }
    it(
      "should determine that black king can't castle because a piece the king will pass through is threatened") {
      val game = ChessGame
        .fromGridString(
          """....♚..♜
          |........
          |........
          |........
          |........
          |........
          |.....♖..
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.exists {
        case m: CastlingAction ⇒ true
        case _ ⇒ false
      } shouldBe false
    }
    it(
      "should determine that black king can't long castle because a piece the king will pass through is threatened") {
      val game = ChessGame
        .fromGridString(
          """♜...♚...
          |........
          |........
          |........
          |........
          |........
          |..♖.....
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.exists {
        case m: CastlingAction ⇒ true
        case _ ⇒ false
      } shouldBe false
    }
    it("should determine that black king can long castle") {
      val game = ChessGame
        .fromGridString(
          """♜...♚...
          |........
          |........
          |........
          |........
          |........
          |.....♖..
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.blackPlayer.kingPiece(game.board).get.actions(game.board)
      game.board.actions.exists {
        case m: CastlingAction ⇒ true
        case _ ⇒ false
      } shouldBe true
    }
    it("should disable ability for black to castle after castling") {
      val game = ChessGame
        .fromGridString(
          """♜...♚...
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board
        .doAction(
          CastlingAction(♚(XY(4, 0), BlackChessPlayer),
                         XY(-2, 0),
                         ♜(XY(0, 0), BlackChessPlayer),
                         XY(3, 0))
        )
        .get
        .castlingAvailable shouldBe Map(
        (WhiteChessPlayer, CastlingSide.Queenside) -> true,
        (WhiteChessPlayer, CastlingSide.Kingside) -> true,
        (BlackChessPlayer, CastlingSide.Queenside) -> false,
        (BlackChessPlayer, CastlingSide.Kingside) -> false
      )
    }
    it("should disable ability for white to castle after castling") {
      val game = ChessGame
        .fromGridString(
          """........
          |........
          |........
          |........
          |........
          |........
          |........
          |♖...♔...""".stripMargin,
          turn = WhiteChessPlayer
        )
        .get

      game.board
        .doAction(
          CastlingAction(♚(XY(4, 7), WhiteChessPlayer),
                         XY(-2, 0),
                         ♜(XY(0, 7), WhiteChessPlayer),
                         XY(3, 0))
        )
        .get
        .castlingAvailable shouldBe Map(
        (WhiteChessPlayer, CastlingSide.Queenside) -> false,
        (WhiteChessPlayer, CastlingSide.Kingside) -> false,
        (BlackChessPlayer, CastlingSide.Queenside) -> true,
        (BlackChessPlayer, CastlingSide.Kingside) -> true
      )
    }
    it(
      "should not long castle if there is something in the way of the rook but not of the king") {
      val game = ChessGame
        .fromGridString(
          """♜♞..♚...
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.actions.exists {
        case m: CastlingAction ⇒ true
        case _ ⇒ false
      } shouldBe false
    }
    it("should properly update the grid after a black long castle") {
      val game = ChessGame
        .fromGridString(
          """♜...♚...
          |........
          |........
          |........
          |........
          |........
          |........
          |........""".stripMargin,
          turn = BlackChessPlayer
        )
        .get

      game.board.doAction(CastlingAction.blackQueenside()).get.game shouldBe
        ChessGame
          .fromGridString(
            """..♚♜....
            |........
            |........
            |........
            |........
            |........
            |........
            |........""".stripMargin,
            turn = WhiteChessPlayer,
            castlingAvailable = castlingOnlyWhiteAvailable,
            fullMoveNumber = 2,
            halfMoveClock = 1
          )
          .get
    }
  }
}
