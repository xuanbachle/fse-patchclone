package parsers

import java.io.{File, IOException}
import java.util

import myLib.MyjavaLib
import org.eclipse.jdt.core.JavaCore
import org.eclipse.jdt.core.dom._

/**
 * Created by xuanbach32bit on 4/22/15.
 */

class JavaParser extends AbstractParser[CompilationUnit]{

  def parse[B >:CompilationUnit](str: String, filePath: String): CompilationUnit = {
    val file = new File(filePath)
    val parser: ASTParser = ASTParser.newParser(AST.JLS4)
    //parser.setEnvironment(RepairOptions.libs,Array(file.getParent, file.getParentFile.getParent), null, true)
    parser.setEnvironment(Array[String]("/usr/java/jdk1.8.0_51/jre/lib/rt.jar"),Array(new File(filePath).getParent), null, true)
    parser.setUnitName(new File(filePath).getName)
    //import scala.collection.JavaConversions._
    //parser.setEnvironment(Array(str), Array(str),Array("UTF-8"), true)
    parser.setSource(str.toCharArray)
    parser.setKind(ASTParser.K_COMPILATION_UNIT)
    parser.setResolveBindings(true)
    parser.setBindingsRecovery(true)
    parser.setStatementsRecovery(true)

    val options = JavaCore.getOptions();
    JavaCore.setComplianceOptions(JavaCore.VERSION_1_5, options);
    parser.setCompilerOptions(options);

    try {
      val root = parser.createAST(null)
      val cu: CompilationUnit = root.asInstanceOf[CompilationUnit]
      //cu.recordModifications
      assert(cu != null)
      return cu
    }catch {
      case e: Throwable => {
        println(filePath)
        e.printStackTrace()
        sys.error("CU is null when parsing!!!"+filePath)
      }
    }
  }

  def getCompilationUnit(fileName: String): CompilationUnit ={
    globalASTs.get(fileName)
  }


  @throws(classOf[IOException])
  def parseFilesInDir(dirPath: String) {
    val root: File = new File(dirPath)
    val files: java.util.List[File] = MyjavaLib.walk(dirPath,".java",new java.util.ArrayList[File])
    ParseFiles(files)
  }

}

