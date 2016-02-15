package utilities

import org.eclipse.jdt.core.dom.{CompilationUnit, ASTNode, ASTVisitor}

import scala.collection.mutable.ArrayBuffer

/**
  * Created by xuanbach on 2/15/16.
  */
class FindContextVisitor(lineNumber: Int, comp: CompilationUnit) extends ASTVisitor{
  val befContext = new ArrayBuffer[ASTNode]()
  val aftContext = new ArrayBuffer[ASTNode]()

  override def preVisit2(node: ASTNode): Boolean ={
    val nodeLine = comp.getLineNumber(node.getStartPosition)
    if((nodeLine >= lineNumber - 3) && nodeLine < lineNumber){
      befContext.append(node)
    } else if(nodeLine > lineNumber && (nodeLine <= lineNumber+3)){
      aftContext.append(node)
    }
    return true
  }

  private def context2String(context: ArrayBuffer[ASTNode]): String ={
    context.foldLeft(""){
      (res, ctx) => {
         res + ctx.toString + "\n"
      }
    }
  }

  def getBefContextString(): String ={
    return context2String(befContext)
  }

  def getAftContextString(): String ={
    return context2String(aftContext)
  }
}
