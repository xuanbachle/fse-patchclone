package parsers;

import myLib.MyScalaLib
import myLib.Lib
import org.eclipse.jgit.revwalk.RevCommit

import scala.collection.mutable.ArrayBuffer
import util.parsing.combinator._
import java.io._

object GitDiff {
  // file names have "a/" or "b/" as prefix, need to drop that to compare
  def apply (files: (String,String), op: FileOperation, ind: IndexInformation, chunks: List[ChangeChunk]) = {
    def strip(s: String) = s.dropWhile(_ != '/').drop(1)
    new GitDiff( strip( files._1 ), strip( files._2 ), op,ind, chunks )
  }
}

case class GitDiff(oldFile: String, newFile: String, op: FileOperation, ind: IndexInformation,chunks: List[ChangeChunk]) {
  val isRename = oldFile != newFile
}

class ParserDiffException(msg: String) extends RuntimeException(msg)

object ParserDiffException {
  def create(msg: String) : ParserDiffException = new ParserDiffException(msg)

  def create(msg: String, cause: Throwable) = new ParserDiffException(msg).initCause(cause)
}

sealed trait FileOperation
case class NewFile(mode: Int) extends FileOperation
case class DeletedFile(mode: Int) extends FileOperation
case object UpdatedFile extends FileOperation

sealed trait LineChange { def line: String }
case class ContextLine(line: String) extends LineChange
case class LineRemoved(line: String) extends LineChange
case class LineAdded(line: String) extends LineChange
case class OtherChange(line: String) extends LineChange

sealed trait Information
case class RangeInformation(oldOffset: Int, oldLength: Int, newOffset: Int, newLength: Int) extends Information
case class OtherInformation(infor: String) extends Information
case class IndexInformation(ind1: String,ind2: String) extends Information
case class ChangeChunk(rangeInformation: RangeInformation, changeLines: List[LineChange]) extends Information

// Code taken from http://stackoverflow.com/questions/3560073/how-to-write-parser-for-unified-diff-syntax

object GitDiffParser extends RegexParsers {

  override def skipWhitespace = false

  def allDiffs: Parser[List[GitDiff]] = rep1(gitDiff)

  def gitDiff: Parser[GitDiff] = filesChanged ~ fileOperation ~ index ~ diffChunks ^^ {
    case files ~ op ~ idx ~ chunks => GitDiff(files, op,idx , chunks)
  }

  def filesChanged: Parser[(String, String)] =
    "diff --git " ~> filename ~ (" " ~> filename) <~ (newline) ^^ { case f1 ~ f2 => (f1,f2) }

  def fileOperation: Parser[FileOperation] =
    opt(deletedFileMode | newFileMode) ^^ { _ getOrElse UpdatedFile }

  def index: Parser[IndexInformation] =  opt("index " ~ hash ~ ".." ~ hash ~ opt(" " ~ mode) ~ (newline)) ^^ { a => a match {
      case Some (id ~ h1 ~ dotdot ~ h2 ~ op ~ nl) => IndexInformation(h1, h2)
      case None => IndexInformation("","")
    }
  }
  def deletedFileMode: Parser[DeletedFile] = "deleted file mode " ~> mode <~ newline ^^ { m => DeletedFile(m) }
  def newFileMode: Parser[NewFile] = "new file mode " ~> mode <~ newline ^^ { m => NewFile(m) }
  def hash: Parser[String] = """[0-9a-f]{7}""".r
  def mode: Parser[Int] = """\d{6}""".r ^^ { _.toInt }

  def diffChunks: Parser[List[ChangeChunk]] = (oldFile ~ newFile) ~> rep1(changeChunk)

  def oldFile: Parser[String] = "--- " ~> filename <~ newline
  def newFile: Parser[String] = "+++ " ~> filename <~ newline
  def filename: Parser[String] = """[\S]+""".r

  def changeChunk: Parser[ChangeChunk] = rangeInformation ~ opt(contextLine) ~ (opt(newline) ~> rep1(lineChange)) ^^ {
    case ri ~ opCtx ~ lines => ChangeChunk(ri, opCtx map (_ :: lines) getOrElse (lines))
  }
  
  def generalInformation: Parser[Information] = rangeInformation | otherInformation
  
  def otherInformation: Parser[OtherInformation] =
    (("Binary ") /*| ("Index ")*/) ~> """.*""".r ^^ (l => OtherInformation("Other infor, may not be important ..."))
    
  def rangeInformation: Parser[RangeInformation] =
    ("@@ " ~> "-" ~> number) ~ opt("," ~> number) ~ (" +" ~> number) ~ opt("," ~> number) <~ " @@" ^^ {
      case a ~ b ~ c ~ d => RangeInformation(a, b getOrElse 0, c, d getOrElse 0)
    }

  def lineChange: Parser[LineChange] = contextLine | addedLine | deletedLine|otherLine
  def contextLine: Parser[ContextLine] = " " ~> """.*""".r <~ ((newline+)?) ^^ { l => ContextLine(l) }
  def addedLine: Parser[LineAdded] = "+" ~> """.*""".r <~ ((newline+)?) ^^ { l => LineAdded(l) }
  def deletedLine: Parser[LineRemoved] = "-" ~> """.*""".r <~ ((newline+)?) ^^ { l => LineRemoved(l) }
  def otherLine: Parser[OtherChange] = "\\" ~ " " ~> """.*""".r <~ ((newline+)?) ^^ { l => OtherChange(l) }

  def newline: Parser[String] = """\n""".r
  def number: Parser[Int] = """\d+""".r ^^ { _.toInt }

  def parse(str: String) = parseAll(allDiffs, str) 

  def parseFromFile = {
    val lines = scala.io.Source.fromFile("./mydata/testdiff.patch").mkString
    parseAll(allDiffs, lines) match {
      case Success(s,_) => 
        {
          println( s )
          //println(parseAndPostProcess(lines))
        }
      case NoSuccess(msg,_) =>
        println( "xxxx")
        //sys.error("ERROR: " + msg)
        throw new ParserDiffException("ERROR: " + msg)
    }
  }

  def getUnifiedDiff(lines: Iterator[String]): String ={
    var startUnifiedDiff = false
    var collectLines = new ArrayBuffer[String]
    while(lines.hasNext){
      val lineStr = lines.next()
      //println("bach: "+lineStr)
      if(lineStr.startsWith("diff --")){
        startUnifiedDiff = true
      }
      if(startUnifiedDiff){
        collectLines.append(lineStr)
        //println("Collected: "+lineStr)
      }
    }
    val str=collectLines.foldLeft("")((res,line) => res+line+"\n")
    //println(str)
    str
  }

  def parseFromFile(pathToFile: String): java.util.List[GitDiff] = {
    val lines = scala.io.Source.fromFile(pathToFile).getLines()
    return parseFromString(getUnifiedDiff(lines))
  }

  def parseFromString(lines: String): java.util.List[GitDiff] ={
    parseAll(allDiffs, lines) match {
      case Success(s,_) =>
      {
        //println( "asdasd" )
        //println(parseAndPostProcess(lines))
        import scala.collection.JavaConversions._
        s.toList
      }
      case NoSuccess(msg,_) =>
        //println( "xxxx")
        //sys.error("ERROR: " + msg)
        throw new ParserDiffException("ERROR: " + msg)
    }
  }

  def parseAndPrint(str: String) = {
    parse(str) match {
      case Success(s,_) => {
        //println( s )
        printToFile("./mylogs/log.txt", s.toString()+"\n", UpdatedFile)
      }
      case NoSuccess(msg,_) => {
         printToFile("./mylogs/errorlog.txt", str, NewFile(0))
         sys.error(str)
      }
    }
  }
  
   def parseAndPostProcess_old(str: String): Boolean = {
     var cFilesChanged = 0
    parse(str) match {
      case Success(s,_) => {
        /*Diff contains .c or .java files only*/
        //return true
        s.foreach { gdiff =>
          if(gdiff.isRename||gdiff.op.isInstanceOf[NewFile]||gdiff.isInstanceOf[DeletedFile])
            return false

          else if(gdiff.oldFile.endsWith(".cpp")||gdiff.oldFile.endsWith(".h"))
            return false
        }

        s.foreach { gdiff =>
          if(gdiff.oldFile.endsWith(".c")||gdiff.oldFile.endsWith(".java")) {
            cFilesChanged +=1
            //println(gdiff.chunks.head.changeLines.length)
            if (gdiff.chunks.length > 1)//can only have one chunk
              return false
            if(processChangeChunks(gdiff.chunks) == false)
              return false
            if(cFilesChanged>1)//can only have changes on one file
              return false
          }
        }

        if(cFilesChanged==1)
          return true
        //
        return false
      }
      case NoSuccess(msg,_) => {
         printToFile("./mylogs/errorlog.txt", str, NewFile(0))
         false
         //sys.error(str)
      }
    }

  }

  def processChangeChunks(chunks: List[ChangeChunk]): Boolean = {
    val linesChanged = chunks.foldLeft (0) ( (count,ck) =>
      ck.changeLines.foldLeft (count) ( (lineCount,cl) =>
        if(cl.line.trim().startsWith("/*") || cl.line.trim().startsWith("*")|| cl.line.trim().startsWith("//")){
          lineCount // do nothing if the line added or removed are comments
        } else{
          lineCount + 1 // increase number of line changed if the changed line is not comment
        }
      )
    )
    if(linesChanged>2)// on one chunk, only have 2 lines, including one minus and one plus
      return false
    return true
  }

  def parseAndPostProcessHelper(str: String): (Boolean,String) = {
    var filesChanged = 0
    parse(str) match {
      case Success(s,_) => {
        /*Diff contains .c or .java files only*/
        //return true
        s.foreach { gdiff =>
          if(gdiff.isRename||gdiff.isInstanceOf[DeletedFile])
            return (false,"")

          //else if(gdiff.oldFile.endsWith(".cpp")||gdiff.oldFile.endsWith(".h"))
          //  return false
        }
        var unitTest = ""
        s.foreach { gdiff =>
          if(gdiff.oldFile.endsWith(".java")) {
            if(gdiff.oldFile.endsWith("Test.java")){
              println("Found unit test commit!");
              unitTest += gdiff.oldFile+";";
            }else {
              filesChanged += 1
              //println(gdiff.chunks.head.changeLines.length)
              if (gdiff.chunks.length > 1) //can only have one chunk
                return (false,unitTest)
              if(processChangeChunks(gdiff.chunks) == false)
                return (false,unitTest)
              if (filesChanged > 1) //can only have changes on one file
                return (false,unitTest)
            }
          }
        }

        //
        if(filesChanged == 0)
          return (false,unitTest)

        return (true,unitTest)
      }
      case NoSuccess(msg,_) => {
        printToFile("./mylogs/errorlog.txt", str, NewFile(0))
        sys.error(str)
      }
    }

  }

  def parseAndPostProcess(str: String, parent: RevCommit, commit: RevCommit, funitT: String, fwithoutUnitT: String): Boolean = {
    var (res,unitTest) = parseAndPostProcessHelper(str)
    if(res && !unitTest.equals("")){
      MyScalaLib.appendFile(funitT, parent.name() + " " + commit.name() + " "+unitTest+ "\n")
      var len = Lib.readFile2List(funitT).size()
      var funitTFolder = (new File(funitT)).getParent()
      var diffFolder=funitTFolder+ File.separator + "withUnit_diff"
      var messFolder=funitTFolder+ File.separator + "withUnit_message"
      if(!Lib.folderExist(diffFolder))
        Lib.initializeFolder(diffFolder)
      Lib.writeText2File(str, new File(diffFolder+ File.separator+len+".diff"))
      if(!Lib.folderExist(messFolder))
        Lib.initializeFolder(messFolder)
      Lib.writeText2File(commit.getFullMessage, new File( messFolder+File.separator+len+".mess"))

    } else if(res && unitTest.equals("")){
      MyScalaLib.appendFile(fwithoutUnitT, parent.name() + " " + commit.name() + "\n")
      var len = Lib.readFile2List(fwithoutUnitT).size()
      var fWithoutunitTFolder = (new File(fwithoutUnitT)).getParent()
      var diffFolder=fWithoutunitTFolder+ File.separator + "withoutUnit_diff"
      var messFolder=fWithoutunitTFolder+ File.separator + "withoutUnit_message"
      if(!Lib.folderExist(diffFolder))
        Lib.initializeFolder(diffFolder)
      Lib.writeText2File(str, new File(diffFolder +File.separator +len+".diff"))
      if(!Lib.folderExist(messFolder))
        Lib.initializeFolder(messFolder)
      Lib.writeText2File(commit.getFullMessage, new File(messFolder+ File.separator + File.separator+len+".mess"))
    }
    return res
  }

  def printToFile(filename: String, content: String, fop: FileOperation) ={
    new File("./mylogs/").mkdir()
    fop match {
      case NewFile(_) =>{
        val writer = new PrintWriter(new File(filename))
        println("Logged!")
        writer.write(content)
        writer.close()
      }
      case UpdatedFile =>{
        val fw = new FileWriter(filename, true) ; 
        fw.write(content) ; 
        fw.close()
      } 
    }
  }
  
  def main(args: Array[String]) = {
   // parseFromFile("/home/dxble/Desktop/testres/test_parseDiff/testdiff")
//    val reader = {
//      if (args.length == 0) {
//        // read from stdin
//        Console.in
//      } else {
//        new java.io.FileReader(args(0))
//      }
//    }
      try{
        //parseFromFile
      }
//      catch{
//        
//      }
      
  }
}