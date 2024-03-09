package org.ergoplatform.kiosk.timestamp.v2

import org.ergoplatform.kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import scorex.crypto.hash.Blake2b256
import org.ergoplatform.kiosk.ergo._
import org.ergoplatform.kiosk.script.ScriptUtil

import scala.collection.mutable.{Map => MMap}

object Timestamp {

  val buffer: Int = 5 // blocks
  val minStorageRent: Long = 1500000L

  val timestampScript = "sigmaProp(false)"
  val timestampErgoTree = ScriptUtil.compile(Map(), timestampScript)

  val env = MMap[String, KioskType[_]]()

  import ScriptUtil._

  env.setCollByte("timestampScriptBytes", timestampErgoTree.bytes)

  val emissionScript =
    s"""{ 
       |  
       |  val out = OUTPUTS(0)  // copy of this box with balance number of tokens
       |  val box = CONTEXT.dataInputs(0)  // the box for which we need a timestamp
       |  val timestampBox = OUTPUTS(1)  // the timestamp (proves that "box existed in UTXO at height x")
       |  
       |  val inTokenId = SELF.tokens(0)._1
       |  val inTokens = SELF.tokens(0)._2
       |  val outTokenId = out.tokens(0)._1
       |  val outTokens = out.tokens(0)._2
       |  
       |  val validIn = SELF.id == INPUTS(0).id
       |   
       |  val validOut = out.propositionBytes == SELF.propositionBytes && out.value >= SELF.value && outTokens >= 2
       |     
       |  val validTokens = outTokenId == inTokenId && inTokens == (outTokens + 1)
       |  
       |  val validTimestamp = timestampBox.R4[Coll[Byte]].get == box.id && 
       |                       timestampBox.R5[Int].get >= (HEIGHT - $buffer) && 
       |                       timestampBox.propositionBytes == timestampScriptBytes && 
       |                       timestampBox.tokens(0)._1 == inTokenId
       |    
       |  sigmaProp(
       |    validIn && validOut && validTokens && validTimestamp
       |  )
       |}
       |""".stripMargin

  val emissionErgoTree = ScriptUtil.compile(env.toMap, emissionScript)
  val emissionScriptHash = Blake2b256(emissionErgoTree.bytes)

  env.setCollByte("emissionScriptHash", emissionScriptHash)
  val masterScript =
    s"""{
       |  val out = OUTPUTS(0)
       |  val emissionBox = OUTPUTS(1)
       |  
       |  val validIn = SELF.id == INPUTS(0).id
       |  
       |  val validOut = out.propositionBytes == SELF.propositionBytes && 
       |                 out.tokens(0)._1 == SELF.tokens(0)._1 && 
       |                 SELF.tokens(0)._2 == (out.tokens(0)._2 + 1000) && 
       |                 out.value == SELF.value
       |                 
       |  val validEmissionBox = blake2b256(emissionBox.propositionBytes) == emissionScriptHash && 
       |                         emissionBox.tokens(0)._1 == SELF.tokens(0)._1 && 
       |                         emissionBox.tokens(0)._2 == 1000 &&
       |                         emissionBox.value >= $minStorageRent
       |                           
       |  sigmaProp(validIn && validOut && validEmissionBox)
       |}
       |""".stripMargin
  val emissionAddress = getStringFromAddress(getAddressFromErgoTree(emissionErgoTree))

  val masterErgoTree = ScriptUtil.compile(env.toMap, masterScript)
  val masterAddress = getStringFromAddress(getAddressFromErgoTree(masterErgoTree))

  def main(args: Array[String]): Unit = {
    println(emissionAddress)
    println(masterAddress)
    println("timestamp ErgoTree (hex) " + timestampErgoTree.bytes.encodeHex)
    assert(
      emissionAddress == "AbEPxRNUk5bxs7jiN2r3yoHQg56CULrN9XRdpV3UQQfxxtVvSYzpw6NhfGDjT5XqxvrmCLaWAAJ6HvG9hLzAuxMmhTQNnd4mBRynvVk5Z7JDL1aULqKYgQmEDy1SFFEQs6WsAB7h7oYRL61k8Ph5wHwBdrrywFp9CqLEBQ1VSZTHvCxQpULqPukeXNVGN7E4GHeSyYdzqiHvzunRvhr7bbnbCmszFLG1tUrwaeP5MutN1qdw7LgN1fPUu78b")
    assert(
      masterAddress == "2vTQnMx5uFfFfJjL6ucuprpWSUeXHBaY3o29jvt1ArkPQJ1UuMZwFWSwrBzUEFQFZsQsDGewdNWEAjWF9BFErt99qBmFHbPy3QKjw32v54Vft2tzJhib6QDneR4TGzyDGtkqEjyC3FdwPZjYAan8RuAhsSHkCnPmdtwSRtLcVQKYPPqXqNG7JZMsK1qpR7Bqj2cpGFFVttq27jHeCYEpTM2dZktx6v16ESZ1DjXbe9FTU6QsmW6p4cWZVN78VczxSF38ru3eeasecAAYgMenuFkNQVdaJcJpotmW")
  }
}
