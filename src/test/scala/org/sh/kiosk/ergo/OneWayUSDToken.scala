package org.sh.kiosk.ergo

import org.ergoplatform.{Pay2SAddress, Pay2SHAddress}
import org.sh.kiosk.ergo.ErgoMix.$ergoScript
import org.sh.kiosk.ergo.util.ErgoScriptUtil._
import org.sh.cryptonode.util.BytesUtil._

import scorex.crypto.hash.Blake2b256
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

object OneWayUSDToken extends App {
  /* using the description at https://www.ergoforum.org/t/tethering-a-token-to-usd-using-the-rate-oracle/118/4

Bob can create a “token box” with a large number of “USD tokens” that can be exchanged for Ergs at the current rate in USD/Erg. The contract in the token box only allows changing Ergs to tokens and not the other way round.

So Bob could create a box with 1 trillion tokens such that anyone can pay ergs and purchase tokens at the inverse rate of USD/ergs. If the rate is 10 USD/Erg, then anyone can purchase X number of tokens by paying X/10 Ergs.

Bob promises to consider these tokens equivalent to USD. In particular, Bob promises:

To give physical USD in exchange for tokens at 1:1 rate.
To accept tokens in lieu of USD at 1:1 rate.

Bob only sells those tokens via the token box whose code is given in the contract below.

   */
  val rateOracleTokenID:Array[Byte] = Blake2b256("rate").toArray // To use the correct id in real world

  val env = new Env
  env.setCollByte("rateTokenID", rateOracleTokenID)

  // lender
  val bobPrivateKey = getRandomBigInt
  val bob = hexToGroupElement(ECC.gExp(bobPrivateKey))

  env.setGroupElement("bob", bob)

  val source =
    """{
      |  val newSelf = OUTPUTS(0) // new box created as a replica of current box
      |  val bobOut = OUTPUTS(1) // box paying to Bob
      |
      |  val bobNanoErgs = bobOut.value
      |  val validBobBox = bobOut.propositionBytes == proveDlog(bob).propBytes
      |
      |  val selfTokenID = SELF.tokens(0)._1
      |  val selfTokenAmt = SELF.tokens(0)._2
      |
      |  val newSelfTokenID = newSelf.tokens(0)._1
      |  val newSelfTokenAmt = newSelf.tokens(0)._2
      |  val validTokenID = selfTokenID == newSelfTokenID
      |  val validProp = newSelf.propositionBytes == SELF.propositionBytes
      |
      |  val tokenDiff = selfTokenAmt - newSelfTokenAmt
      |  val validNewSelf = validTokenID && validProp
      |
      |  val rateBox = CONTEXT.dataInputs(0)
      |  val rate = rateBox.R4[Long].get
      |  val validRateBox = rateBox.tokens(0)._1 == rateTokenID
      |
      |  // rate gives nanoErgo per USDCent
      |  // Thus, bobNanoErgs NanoErgs will cost bobNanoErgs / rate usd cents
      |
      |  val usdCDiff = bobNanoErgs / rate
      |
      |  tokenDiff <= usdCDiff && validRateBox && validNewSelf && validBobBox
      |}""".stripMargin

  val ergoCompiler = new ErgoScript(env) {}

  val ergoTree = ergoCompiler.$compile(source)

  val serializedScript = {
    env.$getEnv.map{
      case (keyword, value) =>
        keyword + " = " + serialize(value).encodeHex
    }.toArray ++ Array(
      ergoCompiler.$matchScript(DefaultSerializer.serializeErgoTree(ergoTree), env.$getEnv.keys.toArray).grouped(120).mkString("\n")
    )
  }

  import ergoCompiler.$ergoAddressEncoder

  println("Bobs address: "+Pay2SAddress(ergoTree))
}

