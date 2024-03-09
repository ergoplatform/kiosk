package org.ergoplatform.kiosk.nonlazy

import org.ergoplatform.kiosk.encoding.ScalaErgoConverters.{getAddressFromErgoTree, getStringFromAddress}
import org.ergoplatform.kiosk.script.ScriptUtil

object Branch {

  val branchScript =
    s"""{ 
       |  val ok = if (OUTPUTS(0).R4[Int].get == 1) {
       |    CONTEXT.dataInputs(0).R4[Long].get <= SELF.value
       |  } else { // assume Coll[Byte]
       |    CONTEXT.dataInputs(0).R4[Coll[Byte]].get != SELF.propositionBytes
       |  }
       |  sigmaProp(ok)
       |}
       |""".stripMargin

  val branchErgoTree = ScriptUtil.compile(Map(), branchScript)

  val branchBoxAddress = getStringFromAddress(getAddressFromErgoTree(branchErgoTree))

}
