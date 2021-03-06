package ostinato.chess.core

import ostinato.chess.core.NotationParser.PreParseInsights
import ostinato.core.{Board, XY}

case class ChessBoard(
    override val grid: Vector[Option[ChessPiece]],
    turn: ChessPlayer = WhiteChessPlayer,
    enPassantPawn: Option[EnPassantPawn] = None,
    castlingAvailable: Map[(ChessPlayer, CastlingSide.Value), Boolean] =
      castlingFullyAvailable,
    fullMoveNumber: Int = 1,
    halfMoveClock: Int = 0,
    history: List[GameStep] = List())
    extends Board[ChessBoard,
                  ChessAction,
                  ChessPiece,
                  ChessPlayer,
                  ChessOptimisations](grid) {

  def doAction(a: ChessAction)(implicit opts: ChessOptimisations =
                                 ChessOptimisations.default) = {
    lazy val calculateEnPassants = a match {
      case EnPassantAction(pawn, delta, _, _) ⇒
        Some(
          EnPassantPawn(pawn.pos + XY(0, math.signum(delta.y)),
                        pawn.movedTo(pawn.pos + XY(0, delta.y))))
      case _ ⇒
        None
    }

    lazy val calculateCastlingAvailable = a match {
      case CastlingAction(_, _, _, _, _, _) ⇒
        castlingAvailable
          .updated((turn, CastlingSide.Queenside), false)
          .updated((turn, CastlingSide.Kingside), false)
      case _ ⇒
        castlingAvailable
    }

    lazy val calculateHalfMoveClock = a match {
      case MoveAction(p: ♟, _, _, _) ⇒ 0
      case EnPassantAction(_, _, _, _) ⇒ 0
      case EnPassantCaptureAction(_, _, _, _, _) ⇒ 0
      case CaptureAction(_, _, _, _, _) ⇒ 0
      case PromoteAction(_, _, _, _, _) ⇒ 0
      case CapturePromoteAction(_, _, _, _, _, _) ⇒ 0
      case _ ⇒ halfMoveClock + 1
    }

    lazy val applyAction = {
      val newBoard = ChessBoard(
        grid = a.gridUpdates.foldLeft(grid)(applyUpdate),
        turn = turn.enemy,
        enPassantPawn = calculateEnPassants,
        castlingAvailable = calculateCastlingAvailable,
        fullMoveNumber =
          if (turn == BlackChessPlayer) fullMoveNumber + 1 else fullMoveNumber,
        halfMoveClock = calculateHalfMoveClock
      )

      val _history =
        if (opts.dontCalculateHistory)
          List()
        else {
          val previousHistory =
            if (history.isEmpty) List(GameStep(None, this)) else history
          GameStep(Some(a), newBoard) :: previousHistory
        }

      Some(newBoard.copy(history = _history))
    }

    lazy val applyFinalAction = Some(
      ChessBoard(grid,
                 turn,
                 None,
                 castlingFullyUnavailable,
                 fullMoveNumber,
                 0))

    if (opts.extraValidationOnActionApply) {
      (a, get(a.fromPiece.pos)) match {
        case (action, _) if action.isFinal ⇒
          applyFinalAction
        case (_, Some(Some(a.fromPiece))) if a.fromPiece.owner == turn ⇒
          applyAction
        case _ ⇒
          None
      }
    } else {
      if (a.isFinal)
        applyFinalAction
      else
        applyAction
    }
  }

  def movementsOfDelta(from: XY, delta: XY)(implicit opts: ChessOptimisations =
                                              ChessOptimisations.default)
    : Set[ChessAction] = {
    val to = from + delta
    val fromPiece = get(from)
    val toPiece = get(to)

    def betweenLocationsFree(f: XY = from, t: XY = to) =
      between(f, t) forall isEmptyCell

    def betweenLocationsNotThreatenedBy(player: ChessPlayer) =
      xyBetween(from, to) forall (pos ⇒
        posThreatenedBy(pos, player.enemy, this).isEmpty)

    def betweenLocationsNotThreatenedBy2(player: ChessPlayer) =
      xyBetween(from, to) forall (pos ⇒
        player.pieces(this) forall (!_.canMoveTo(pos,
                                                 this.copy(turn = player))))

    def isEnPassantPawn(pos: XY) = enPassantPawn.exists(epp ⇒ epp.from == pos)

    def targetRook(k: ♚) = get(k.targetRookPosition(delta.x)) match {
      case Some(Some(r: ♜)) if r.owner == k.owner && r.castlingSide.nonEmpty ⇒
        Some((r, r.castlingSide))
      case _ ⇒ None
    }

    lazy val validateAction: Set[ChessActionFactory] =
      (fromPiece, toPiece, enPassantPawn) match {
        case (Some(Some(p: ♟)), Some(None), Some(epp: EnPassantPawn))
            if delta.x != 0
              && isEnPassantPawn(to) && epp.pawn.owner != p.owner ⇒
          Set(EnPassantTakeActionFactory(p, delta, epp.pawn))

        case (Some(Some(p: ♟)), Some(None), _)
            if delta.x == 0 && math.abs(delta.y) == 2 && betweenLocationsFree() ⇒
          Set(EnPassantActionFactory(p, delta))

        case (Some(Some(p: ♟)), Some(Some(toP: ChessPiece)), _)
            if delta.x != 0 &&
              (!toP.isKing || opts.kingIsTakeable) && toP.owner != p.owner && to.y == p
              .promotingPosition(delta.y) ⇒
          Set(♛(from + delta, p.owner),
              ♝(from + delta, p.owner),
              ♞(from + delta, p.owner),
              ♜(from + delta, p.owner)) map (CapturePromoteActionFactory(p,
                                                                         delta,
                                                                         toP,
                                                                         _))

        case (Some(Some(p: ♟)), Some(None), _)
            if delta.x == 0 && math.abs(delta.y) == 1 &&
              to.y == p.promotingPosition(delta.y) ⇒
          Set(♛(from + delta, p.owner),
              ♝(from + delta, p.owner),
              ♞(from + delta, p.owner),
              ♜(from + delta, p.owner)) map (PromoteActionFactory(p, delta, _))

        case (Some(Some(p: ♟)), Some(None), _)
            if delta.x == 0 && math.abs(delta.y) == 1 ⇒
          Set(MoveActionFactory(p, delta))

        case (Some(Some(p: ♟)), Some(Some(toP: ChessPiece)), _)
            if delta.x != 0 &&
              (!toP.isKing || opts.kingIsTakeable) && toP.owner != p.owner ⇒
          Set(CaptureActionFactory(p, delta, toP))

        case (Some(Some(k: ♚)), _, _) if math.abs(delta.x) == 2 ⇒
          (toPiece, targetRook(k)) match {
            case (Some(None), Some((r: ♜, Some(cs: CastlingSide.Value))))
                if k.isInInitialPosition &&
                  castlingAvailable((k.owner, cs)) && betweenLocationsFree() &&
                  betweenLocationsFree(r.pos, r.pos + k.rookDeltaFor(delta)) && !k
                  .isThreatened(this) &&
                  betweenLocationsNotThreatenedBy(k.enemy) ⇒
              Set(CastlingActionFactory(k, delta, r, k.rookDeltaFor(delta)))

            case _ ⇒ Set()
          }

        case (Some(Some(p: ChessPiece)), Some(None), _)
            if !p.isPawn && betweenLocationsFree() ⇒
          Set(MoveActionFactory(p, delta))

        case (Some(Some(p: ChessPiece)), Some(Some(toP: ChessPiece)), _)
            if !p.isPawn && betweenLocationsFree()
              && (!toP.isKing || opts.kingIsTakeable) && toP.owner != p.owner ⇒
          Set(CaptureActionFactory(p, delta, toP))

        case _ ⇒ Set()
      }

    def validateAfterAction(mf: ChessActionFactory): Set[ChessAction] = {
      val m = mf.complete()
      val newBoard =
        this.copy(grid = m.gridUpdates.foldLeft(this.grid)(applyUpdate))
      val isPlayersKingThreatened =
        opts.checkForThreatens && m.fromPiece.owner
          .kingPiece(newBoard)
          .exists(_.isThreatened(newBoard))

      if (!isPlayersKingThreatened) {
        val check = opts.checkForThreatens && m.fromPiece.enemy
          .kingPiece(newBoard)
          .exists(_.isThreatened(newBoard))
        val mate = check && newBoard.isLossFor(m.fromPiece.enemy,
                                               basedOnCheckKnown = true)

        Set(mf.complete(check, mate))
      } else {
        Set()
      }
    }

    lazy val concreteMovementsOfDelta = validateAction flatMap validateAfterAction

    if (opts.validateDeltasOnActionCalculation)
      fromPiece match {
        case Some(Some(p: ChessPiece)) if p.deltas(this).contains(delta) ⇒
          concreteMovementsOfDelta
        case _ ⇒ Set()
      } else
      concreteMovementsOfDelta
  }

  def isDraw(implicit opts: ChessOptimisations = ChessOptimisations.default) =
    isDrawFor(turn)

  def isDrawFor(player: ChessPlayer)(implicit opts: ChessOptimisations =
                                       ChessOptimisations.default) =
    player.nonFinalActions(this).isEmpty && !isLossFor(player)

  def isLoss(implicit opts: ChessOptimisations = ChessOptimisations.default) =
    isLossFor(turn)

  def isLossFor(player: ChessPlayer, basedOnCheckKnown: Boolean = false)(
      implicit opts: ChessOptimisations = ChessOptimisations.default)
    : Boolean = {
    val noCheckForMates = opts.copy(checkForThreatens = false)
    lazy val allNewBoards = player.actions(this)(noCheckForMates) map doAction

    def isKingThreatened(b: ChessBoard): Boolean =
      player.kingPiece(b).exists(_.isThreatened(b)(noCheckForMates))

    player.kingPiece(this).exists { king ⇒
      (basedOnCheckKnown || king
        .isThreatened(this)(noCheckForMates)) && (allNewBoards.flatten forall isKingThreatened)
    }
  }

  override lazy val toString: String = {
    def cellToChar(cell: Cell): Char = cell map (_.toFigurine) getOrElse '.'

    val linesOfCells = grid.grouped(8) map (_.toList)

    linesOfCells map (_ map cellToChar) map (_.mkString) mkString "\n"
  }

  lazy val toShortFen: String = grid
    .map {
      case Some(c) ⇒ c.toFen
      case _ ⇒ ' '
    }
    .foldLeft(Fen(""))(Fen.+)
    .toString

  lazy val toFen: String =
    toShortFen + " " +
      turn.toFen + " " +
      fenCastling(castlingAvailable) + " " +
      enPassantPawn.map(_.from.toAn).getOrElse("-") + " " +
      halfMoveClock + " " +
      fullMoveNumber

  //TODO didn't add rules because I'm hoping to take it away very soon
  lazy val toOstinatoString: String = (toFen + " " +
    history.reverse
      .flatMap(_.action)
      .map(IccfNotationActionSerialiser().serialiseAction(_, PreParseInsights()).head._1)
      .mkString(" ")).trim

  lazy val serialisedFor3FR =
    toShortFen + " " +
      fenCastling(castlingAvailable) + " " +
      enPassantPawn.map(_.from.toAn).getOrElse("-") + " "

  private lazy val simpleInsufficientMaterial =
    Set("Kk", "Kbk", "KNk", "BKk", "Kkn") contains pieces
      .map(_.toFen)
      .mkString
      .sorted

  private lazy val kingsBishopsInsufficientMaterial =
    "BKbk" == pieces.map(_.toFen).toSet.mkString.sorted && bishops
      .map(_.pos.squareColor)
      .toSet
      .size == 1

  lazy val hasInsufficientMaterial = simpleInsufficientMaterial || kingsBishopsInsufficientMaterial
  lazy val isInFiftyMoveRule = halfMoveClock >= 50
  lazy val isInStalemate = turn.actions(this) == Set(
    DrawAction(turn, isCheck = false, isCheckmate = false))
  lazy val allSerialisedPastBoardsFor3FR = history map (_.board.serialisedFor3FR)
  lazy val isInThreefoldRepetition = (allSerialisedPastBoardsFor3FR count (_ == serialisedFor3FR)) >= 3

  //TODO didn't add rules because I'm hoping to take it away very soon
  lazy val canClaimThreefoldRepetition = isInThreefoldRepetition || {
    val allNewBoards = (actions map doAction).flatten.map(_.serialisedFor3FR)
    allNewBoards exists (nb =>
      allSerialisedPastBoardsFor3FR.count(_ == nb) >= 2)
  }

  def doAllActions(
      implicit opts: ChessOptimisations = ChessOptimisations.default) =
    actions flatMap this.doAction

  def doAllNonFinalActions(
      implicit opts: ChessOptimisations = ChessOptimisations.default) =
    nonFinalActions flatMap this.doAction

  def nonFinalActions(
      implicit opts: ChessOptimisations = ChessOptimisations.default) =
    turn.nonFinalActions(this)

  def actions(implicit opts: ChessOptimisations = ChessOptimisations.default) =
    turn.actions(this)

  def actionStream(
      implicit opts: ChessOptimisations = ChessOptimisations.default) =
    turn.actionStream(this)

  lazy val rooks: Vector[♜] = pieces flatMap {
    case p: ♜ ⇒ Vector(p); case _ ⇒ Vector()
  }
  lazy val knights: Vector[♞] = pieces flatMap {
    case p: ♞ ⇒ Vector(p); case _ ⇒ Vector()
  }
  lazy val bishops: Vector[♝] = pieces flatMap {
    case p: ♝ ⇒ Vector(p); case _ ⇒ Vector()
  }
  lazy val queens: Vector[♛] = pieces flatMap {
    case p: ♛ ⇒ Vector(p); case _ ⇒ Vector()
  }
  lazy val kings: Vector[♚] = pieces flatMap {
    case p: ♚ ⇒ Vector(p); case _ ⇒ Vector()
  }
  lazy val pawns: Vector[♟] = pieces flatMap {
    case p: ♟ ⇒ Vector(p); case _ ⇒ Vector()
  }

  def game(implicit opts: ChessOptimisations = ChessOptimisations.default) =
    ChessGame(this, opts)

  override def equals(any: Any) = any match {
    case that: ChessBoard ⇒
      that.grid == grid &&
        that.turn == turn &&
        that.castlingAvailable == castlingAvailable &&
        that.enPassantPawn == enPassantPawn &&
        that.fullMoveNumber == fullMoveNumber &&
        that.halfMoveClock == halfMoveClock
    case _ ⇒
      false
  }

  lazy val rotate = copy(grid = reverseGrid)
  private lazy val reverseGrid = grid.reverse
}
