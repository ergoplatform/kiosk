package kiosk.wallet

import kiosk.appkit.Client
import kiosk.box.KioskBoxCreator
import kiosk.encoding.EasyWebEncoder
import kiosk.offchain.parser.Parser
import kiosk.ergo
import kiosk.ergo.DhtData
import kiosk.explorer.Explorer
import kiosk.offchain.compiler
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, InputBox}
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import scorex.crypto.hash.Blake2b256
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

class KioskWallet($ergoBox: KioskBoxCreator) extends EasyMirrorSession {
  EasyWebEncoder
  private val secretKey: BigInt = BigInt(Blake2b256($ergoBox.$ergoScript.$myEnv.$sessionSecret.getOrElse("none").getBytes("UTF-16")))
  private val defaultGenerator: GroupElement = CryptoConstants.dlogGroup.generator
  private val publicKey: GroupElement = defaultGenerator.exp(secretKey.bigInteger)
  val myAddress: String = {
    Client.usingClient { implicit ctx =>
      val contract = ctx.compileContract(
        ConstantsBuilder
          .create()
          .item(
            "gZ",
            publicKey
          )
          .build(),
        "proveDlog(gZ)"
      )
      val addressEncoder = new ErgoAddressEncoder(ctx.getNetworkType.networkPrefix)
      addressEncoder.fromProposition(contract.getErgoTree).get.toString
    }
  }

  def balance = BigDecimal(Explorer.getUnspentBoxes(myAddress).map(_.value).sum) / BigDecimal(1000000000) + " Ergs"

  private val randId = java.util.UUID.randomUUID().toString

  def send(toAddress: String, ergs: BigDecimal) = {
    val $INFO$ = "Using 0.001 Ergs as fee"
    val $ergs$ = "0.001"
    val nanoErgs = (ergs * BigDecimal(1000000000)).toBigInt().toLong
    val feeNanoErgs = 1000000L
    val unspentBoxes: Seq[ergo.KioskBox] = Explorer.getUnspentBoxes(myAddress).sortBy(-_.value)
    val boxName = randId
    $ergoBox.createBoxFromAddress(boxName, toAddress, Array(), Array(), Array(), nanoErgs)
    val inputs: Seq[String] = boxSelector(nanoErgs + feeNanoErgs, unspentBoxes)
    val txJson = $ergoBox.createTx(
      inputBoxIds = inputs.toArray,
      dataInputBoxIds = Array(),
      outputBoxNames = Array(boxName),
      fee = feeNanoErgs,
      changeAddress = myAddress,
      proveDlogSecrets = Array(secretKey.toString(10)),
      proveDhtDataNames = Array(),
      broadcast = true
    )
    $ergoBox.$deleteBox(boxName)
    txJson
  }

  private val defaultFee = 1000000L

  private def boxSelector(totalNanoErgsNeeded: Long, unspentBoxes: Seq[ergo.KioskBox]) = {
    var sum = 0L
    val unspentBoxSums: Seq[(Int, Long, Long)] = unspentBoxes.zipWithIndex.map {
      case (box, i) =>
        val sumBefore = sum
        sum = sumBefore + box.value
        (i + 1, sumBefore, sum)
    }
    val index: Int = unspentBoxSums
      .find { case (i, before, after) => before < totalNanoErgsNeeded && totalNanoErgsNeeded <= after }
      .getOrElse(throw new Exception(s"Insufficient funds. Short by ${totalNanoErgsNeeded - sum} nanoErgs"))
      ._1
    unspentBoxes.take(index).map(_.optBoxId.get)
  }

  def txBuilder(script: Text, broadcast: Boolean) = {
    val $broadcast$ = "false"

    val compileResults = compiler.Compiler.compile(Parser.parse(script.getText))
    val fee = compileResults.fee.getOrElse(defaultFee)
    val outputNanoErgs = compileResults.outputs.map(_.value).sum + fee
    val deficientNanoErgs = (outputNanoErgs - compileResults.inputNanoErgs).max(0)
    /* Currently we are not going to look for deficient tokens, just nanoErgs */
    val moreInputBoxIds = if (deficientNanoErgs > 0) {
      val myBoxes: Seq[ergo.KioskBox] = Explorer.getUnspentBoxes(myAddress).filterNot(compileResults.inputBoxIds.contains).sortBy(-_.value)
      boxSelector(deficientNanoErgs, myBoxes)
    } else Nil
    val inputBoxIds = compileResults.inputBoxIds ++ moreInputBoxIds
    Client.usingClient { implicit ctx =>
      val inputBoxes: Array[InputBox] = ctx.getBoxesById(inputBoxIds: _*)
      val dataInputBoxes: Array[InputBox] = ctx.getBoxesById(compileResults.dataInputBoxIds: _*)
      //myAddress
      $ergoBox.$createTx(
        inputBoxes = inputBoxes,
        dataInputs = dataInputBoxes,
        boxesToCreate = compileResults.outputs.toArray,
        fee,
        changeAddress = myAddress,
        proveDlogSecrets = Array(secretKey.toString(10)),
        dhtData = Array[DhtData](),
        broadcast = broadcast
      ).toJson(false)
    }
  }

  override def $setSession(sessionSecret: Option[String]): KioskWallet = new KioskWallet($ergoBox.$setSession(sessionSecret))
}
