package org.ergoplatform.kiosk.oraclepool.v3

import org.ergoplatform.kiosk.encoding.ScalaErgoConverters
import org.ergoplatform.kiosk.ergo.KioskType
import org.ergoplatform.kiosk.script.ScriptUtil
import scorex.crypto.hash.Blake2b256
import sigmastate.Values

import scala.collection.mutable.{Map => MMap}

trait OraclePool {
  /*
        <--------------livePeriod------------><---------prepPeriod-------->
 ... ------------------------------------------------------------------------------
        ^                                     ^                           ^
        |                                     |                           |
        epoch start                           epoch end                   epoch preparation end
                                              (epoch preparation start)   (next epoch start)


   */

  // constants
  def livePeriod: Int // blocks
  def prepPeriod: Int // blocks
  val epochPeriod: Int = livePeriod + prepPeriod

  def buffer: Int // blocks
  def errorMargin: Int // percent 0 to 100

  def oracleTokenId: Array[Byte]

  def poolTokenId: Array[Byte]

  def oracleReward: Long // Nano ergs. One reward per data point to be paid to oracle
  def minPoolBoxValue: Long // how much min must exist in oracle pool box

  val env = MMap[String, KioskType[_]]()

  import org.ergoplatform.kiosk.script.ScriptUtil._

  env.setCollByte("oracleTokenId", oracleTokenId)
  env.setCollByte("poolTokenId", poolTokenId)
  env.setLong("minPoolBoxValue", minPoolBoxValue)
  env.setLong("oracleReward", oracleReward)

  require(errorMargin > 0)
  require(errorMargin < 100)

  val liveEpochScript: String =
    s"""{ // This box:
       |  // R4: The latest finalized datapoint (from the previous epoch)
       |  // R5: Block height that the current epoch will finish on
       |  // R6: Address of the "Epoch Preparation" stage contract.
       |
       |  // Oracle box:
       |  // R4: Public key (group element)
       |  // R5: Epoch box Id (this box's Id)
       |  // R6: Data point
       |
       |  val oldDatapoint = SELF.R4[Long].get
       |  val delta = oldDatapoint / 100 * $errorMargin
       |  val minDatapoint = oldDatapoint - delta
       |  val maxDatapoint = oldDatapoint + delta
       |
       |  val oracleBoxes = CONTEXT.dataInputs.filter{(b:Box) =>
       |    b.R5[Coll[Byte]].get == SELF.id &&
       |    b.tokens(0)._1 == oracleTokenId &&
       |    b.R6[Long].get >= minDatapoint &&
       |    b.R6[Long].get <= maxDatapoint
       |  }
       |
       |  val pubKey = oracleBoxes.map{(b:Box) => proveDlog(b.R4[GroupElement].get)}(0)
       |
       |  val sum = oracleBoxes.fold(0L, { (t:Long, b: Box) => t + b.R6[Long].get })
       |
       |  val average = sum / oracleBoxes.size // do we need to check for division by zero here?
       |
       |  val oracleRewardOutputs = oracleBoxes.fold((1, true), { (t:(Int, Boolean), b:Box) =>
       |    (t._1 + 1, t._2 &&
       |               OUTPUTS(t._1).propositionBytes == proveDlog(b.R4[GroupElement].get).propBytes &&
       |               OUTPUTS(t._1).value >= $oracleReward)
       |  })
       |
       |  val epochPrepScriptHash = SELF.R6[Coll[Byte]].get
       |
       |  sigmaProp(
       |    blake2b256(OUTPUTS(0).propositionBytes) == epochPrepScriptHash &&
       |    oracleBoxes.size > 0 &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    OUTPUTS(0).R4[Long].get == average &&
       |    OUTPUTS(0).R5[Int].get == SELF.R5[Int].get + $epochPeriod &&
       |    OUTPUTS(0).value >= SELF.value - (oracleBoxes.size + 1) * $oracleReward &&
       |    oracleRewardOutputs._2
       |  ) && pubKey
       |}
       |""".stripMargin

  val epochPrepScript: String =
    s"""
       |{
       |  // This box:
       |  // R4: The finalized data point from collection
       |  // R5: Height the epoch will end
       |
       |  val canStartEpoch = HEIGHT > SELF.R5[Int].get - $livePeriod
       |  val epochNotOver = HEIGHT < SELF.R5[Int].get
       |  val epochOver = HEIGHT >= SELF.R5[Int].get
       |  val enoughFunds = SELF.value >= $minPoolBoxValue
       |
       |  val maxNewEpochHeight = HEIGHT + $epochPeriod + $buffer
       |  val minNewEpochHeight = HEIGHT + $epochPeriod
       |
       |  if (OUTPUTS(0).R6[Coll[Byte]].isDefined) {
       |    val isliveEpochOutput = OUTPUTS(0).R6[Coll[Byte]].get == blake2b256(SELF.propositionBytes) &&
       |                            blake2b256(OUTPUTS(0).propositionBytes) == liveEpochScriptHash
       |    sigmaProp( // start next epoch
       |      epochNotOver && canStartEpoch && enoughFunds &&
       |      OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |      OUTPUTS(0).R5[Int].get == SELF.R5[Int].get &&
       |      OUTPUTS(0).tokens == SELF.tokens &&
       |      OUTPUTS(0).value >= SELF.value &&
       |      isliveEpochOutput
       |    ) || sigmaProp( // create new epoch
       |      epochOver &&
       |      enoughFunds &&
       |      OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |      OUTPUTS(0).R5[Int].get >= minNewEpochHeight &&
       |      OUTPUTS(0).R5[Int].get <= maxNewEpochHeight &&
       |      OUTPUTS(0).tokens == SELF.tokens &&
       |      OUTPUTS(0).value >= SELF.value &&
       |      isliveEpochOutput
       |    )
       |  } else {
       |    sigmaProp( // collect funds
       |      OUTPUTS(0).R4[Long].get == SELF.R4[Long].get &&
       |      OUTPUTS(0).R5[Int].get == SELF.R5[Int].get &&
       |      OUTPUTS(0).propositionBytes == SELF.propositionBytes &&
       |      OUTPUTS(0).tokens == SELF.tokens &&
       |      OUTPUTS(0).value > SELF.value
       |    )
       |  }
       |}
       |""".stripMargin

  val dataPointScript: String =
    s"""
       |{
       |  // This box:
       |  // R4: The address of the oracle (never allowed to change after bootstrap).
       |  // R5: The box id of the latest Live Epoch box.
       |  // R6: The oracle's datapoint.
       |
       |  val pubKey = SELF.R4[GroupElement].get
       |
       |  val liveEpochBox = CONTEXT.dataInputs(0)
       |
       |  val validLiveEpochBox = liveEpochBox.tokens(0)._1 == poolTokenId &&
       |                          blake2b256(liveEpochBox.propositionBytes) == liveEpochScriptHash
       |
       |  sigmaProp(
       |    OUTPUTS(0).R4[GroupElement].get == pubKey &&
       |    OUTPUTS(0).R5[Coll[Byte]].get == liveEpochBox.id &&
       |    OUTPUTS(0).R6[Long].get > 0 &&
       |    OUTPUTS(0).propositionBytes == SELF.propositionBytes &&
       |    OUTPUTS(0).tokens == SELF.tokens &&
       |    validLiveEpochBox
       |  ) && proveDlog(pubKey)
       |}
       |""".stripMargin

  val poolDepositScript: String =
    s"""
       |{
       |  val allFundingBoxes = INPUTS.filter{(b:Box) =>
       |    b.propositionBytes == SELF.propositionBytes
       |  }
       |
       |  val totalFunds = allFundingBoxes.fold(0L, { (t:Long, b: Box) => t + b.value })
       |
       |  sigmaProp(
       |    blake2b256(INPUTS(0).propositionBytes) == epochPrepScriptHash &&
       |    OUTPUTS(0).propositionBytes == INPUTS(0).propositionBytes &&
       |    OUTPUTS(0).value >= INPUTS(0).value + totalFunds &&
       |    OUTPUTS(0).tokens(0)._1 == poolTokenId
       |  )
       |}
       |""".stripMargin

  import ScalaErgoConverters._

  val liveEpochErgoTree: Values.ErgoTree = ScriptUtil.compile(env.toMap, liveEpochScript)
  env.setCollByte("liveEpochScriptHash", Blake2b256(liveEpochErgoTree.bytes))
  val epochPrepErgoTree: Values.ErgoTree = ScriptUtil.compile(env.toMap, epochPrepScript)
  val dataPointErgoTree: Values.ErgoTree = ScriptUtil.compile(env.toMap, dataPointScript)
  env.setCollByte("epochPrepScriptHash", Blake2b256(epochPrepErgoTree.bytes))
  val poolDepositErgoTree: Values.ErgoTree = ScriptUtil.compile(env.toMap, poolDepositScript)

  val liveEpochAddress: String = getStringFromAddress(getAddressFromErgoTree(liveEpochErgoTree))
  val epochPrepAddress: String = getStringFromAddress(getAddressFromErgoTree(epochPrepErgoTree))
  val dataPointAddress: String = getStringFromAddress(getAddressFromErgoTree(dataPointErgoTree))
  val poolDepositAddress: String = getStringFromAddress(getAddressFromErgoTree(poolDepositErgoTree))
}
