package main

import java.io.File

import config.ConfigurationProperties
import parsers._
import utilities.{FindContextVisitor, Utilities}
/**
  * Created by xuanbach on 2/15/16.
  */
object Main {

  private def getSurroudingStubCode(): (String, String) ={
    return (ConfigurationProperties.getProperty("stubCodeBef"), ConfigurationProperties.getProperty("stubCodeAft"))
  }

  private def writeStubCode(changedLineFolder: String, line2Write: String, fileName: String) ={
    val changedLineWithStubFolder = changedLineFolder+"Stub"
    if(! new File(changedLineWithStubFolder).exists()){
      new File(changedLineWithStubFolder).mkdirs()
    }
    val (stubBef,stubAft) = getSurroudingStubCode()
    val str = new StringBuilder
    str.append(stubBef)
    str.append(line2Write+"\n")
    str.append(stubAft)
    myLib.Lib.writeText2File(str.toString(), new File(changedLineWithStubFolder + File.separator + fileName))
  }

  private def isComment(line: LineChange): Boolean = {
    if (line.line.trim().startsWith("/*") || line.line.trim().startsWith("*") || line.line.trim().startsWith("//")) {
      return true
    } else
      return false
  }

  def processInput(rootFolder: String): Unit ={
    val projectFolders = Utilities.listFolders(new File(rootFolder))
    println("Root: "+rootFolder+" "+projectFolders.length)

    projectFolders.map{
      prjFolder => {
        val modified = Utilities.modifiedFilesFolder(prjFolder)
        val countFolders = Utilities.listFolders(modified)

        countFolders.map{
          countFd => {
            val (_, old) = Utilities.fixOldFolders(countFd)
            val parser = new JavaParser
            parser.parseFilesInDir(old.getAbsolutePath)

            val diffFile = Utilities.diffFileWithCount(Utilities.withUnitDiffFolder(prjFolder), countFd.getName)
            val gitDiffs = GitDiffParser.parseFromFile(diffFile.getAbsolutePath)
            import scala.collection.JavaConversions._
            /**
              * In one diff file, there can be several git diff
              * Here we only get the diff that is for source code, not the diff for test case
              */
            gitDiffs.filter(diff => Utilities.isNotTestFile(new File(diff.oldFile)) && Utilities.isNotTestFile(new File(diff.newFile))).map{
              aDiff =>{
                val compUnit = parser.getCompilationUnit(new File(aDiff.oldFile).getName)
                // A git diff that is not for a test case, it should only be the change we need to look at
                // And in this diff, there is only one change chunk. Thus, it is aDiff.chunks(0)
                val removedLine = aDiff.chunks(0).changeLines.filter(line => line.isInstanceOf[LineRemoved] && !isComment(line))
                /**
                  * Basically, we consider removed line.
                  * If there is no removed line, instead, there is only one added line then we take the added line
                  */
                var lineOfInterest = 0;
                var remLineString = "";
                var addLineString = ""
                if(removedLine.length == 1){
                  remLineString = removedLine(0).asInstanceOf[LineRemoved].line
                  lineOfInterest = aDiff.chunks(0).rangeInformation.oldOffset
                }else{// removedLine.length == 0
                  val addLine = aDiff.chunks(0).changeLines.filter(line => line.isInstanceOf[LineAdded] && !isComment(line))
                  addLineString = addLine(0).line // only one line, and it is the added line
                  lineOfInterest = aDiff.chunks(0).rangeInformation.newOffset
                }
                //val contextVisitor = new FindContextVisitor(lineOfInterest, compUnit)
                //compUnit.getRoot.accept(contextVisitor)
                val changedLineFolder = countFd.getAbsolutePath+File.separator+"ChangedLine"
                if(remLineString != ""){
                  Utilities.writeFile2Folder(remLineString, "removedLine.java", changedLineFolder)
                  writeStubCode(changedLineFolder, remLineString, "removedLineWithStub.java")
                } //else if(addLineString != ""){
                  //Utilities.writeFile2Folder(addLineString, "addedLine.java", changedLineFolder)
                  //writeStubCode(changedLineFolder, addLineString, "addedLineWithStub.java")
                //}

                val oldFile = old.listFiles()(0)// the is only one file, so the index is 0
                val (befCtx, aftCtx) = Utilities.getContext(oldFile.getAbsolutePath, lineOfInterest-1)
                Utilities.writeFile2Folder(befCtx, "befContext.java", countFd.getAbsolutePath+File.separator+"befContext")
                Utilities.writeFile2Folder(aftCtx, "aftContext.java", countFd.getAbsolutePath+File.separator+"aftContext")

              }
            }
          }
        }
      }
    }
  }
  def main (args: Array[String]) {
    /**
      * Input is the root folder
      * From input folder, we scan (iterate) through each project
      */
    val configFile = "/home/xuanbach/workspace/fse-patchclone/resources/configuration.properties"
    if(new File(configFile).exists())
      ConfigurationProperties.load(configFile)
    else
      ConfigurationProperties.load(args(0))// read from command line argument

    val rootFolder = ConfigurationProperties.getProperty("rootFolder")
    processInput(rootFolder)
  }
}
