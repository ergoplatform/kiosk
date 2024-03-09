package org.ergoplatform.kiosk.ergo

/*
  Using the idea from http://research.paradigm.xyz/Yield.pdf

  This version uses a standalone token (no management contract)
 */
trait YieldProtocol {
  /*
  Alice is bond issuer; she creates a box with a large number of
  tokens (as large as possible)

   */

  val liquidatedBoxSrc =
    """
      |{
      |  val bondOwner = proveDlog(alice)
      |
      |  val fixedRate = SELF.R4[Long].get // nanoErgs per usdCent at time of liquidation
      |  val maxRedeemTime = SELF.R5[Int].get
      |
      |  val tokenID = SELF.tokens(0)._1 // tokenID that maps to bonds
      |  val tokenNum = SELF.tokens(0)._2 // how many bond tokens left
      |
      |  val newBox = OUTPUTS(0)
      |  val newBoxTokenID = newBox.tokens(0)._1
      |  val newBoxTokenNum = newBox.tokens(0)._2 // how many bond tokens left
      |  val bondDiff = newBoxTokenNum - tokenNum
      |  val ergsDiff = SELF.value - newBox.value
      |
      |  val validNewBox = newBox.propositionBytes == SELF.propositionBytes &&
      |                    newBoxTokenID == tokenID &&
      |                    bondDiff >= 10000 && // at least 100 USD difference (prevent micro tx)
      |                    ergsDiff <= bondDiff * fixedRate &&
      |                    newBox.R4[Long].get == fixedRate &&
      |                    newBox.R5[Int].get == maxRedeemTime
      |
      |  (bondOwner && (HEIGHT > maxRedeemTime)) || validNewBox
      |}
      |""".stripMargin

  val bondBoxSource =
    """{
      |  val numBonds = SELF.R4[Long].get // how many bonds issued (one bond = 1 USD cent)
      |  val tokenID = SELF.tokens(0)._1 // tokenID that maps to bonds
      |  val tokenNum = SELF.tokens(0)._2 // how many bond tokens left
      |
      |  val newBox = OUTPUTS(0)
      |  val newBoxTokenID = newBox.tokens(0)._1
      |  val newBoxTokenNum = newBox.tokens(0)._2 // how many bond tokens left
      |  val validNewBoxToken = tokenID == newBoxTokenID
      |
      |  val rateBox = CONTEXT.dataInputs(0)
      |  val rate = rateBox.R4[Long].get // nanoErgs per usdCent
      |  val validRateBox = rateBox.tokens(0)._1 == rateTokenID
      |
      |  val lockedErgs = SELF.value // nanoErgs
      |  val neededErgs = numBonds * rate
      |
      |  val insufficientErgs = lockedErgs * 10 >= neededErgs * 11  // at least 10 percent margin
      |
      |  if (HEIGHT > endHeight || insufficientErgs) {
      |     // bond ended or margin call
      |     blake2b256(newBox.propositionBytes) == liquidatedBoxScriptHash &&
      |     validNewBoxToken && newBoxTokenNum == tokenNum &&
      |     newBox.R4[Long].get == rate &&
      |     newBox.R5[Int].get >= HEIGHT + withdrawDeadline
      |  } else {
      |     // purchase bonds
      |     val numTokensReduced = tokenNum - newBoxTokenNum
      |     val numNewBonds = newBox.R4[Long].get
      |     val numBondsIncreased = numNewBonds - numBonds
      |     val ergsIncreased = newBox.value - SELF.value
      |
      |     val validErgsIncrease = ergsIncreased >= numBondsIncreased * rate
      |
      |     newBox.propositionBytes == SELF.propositionBytes &&
      |     numBondsIncreased >= minBondsToPurchase &&
      |     numBondsIncreased == numTokensReduced &&
      |     validErgsIncrease &&
      |     numNewBonds <= maxBonds
      |  }
      |}""".stripMargin

}
