package main

import java.io.File

import parsers.{LineAdded, LineRemoved, GitDiffParser, JavaParser}
import utilities.{FindContextVisitor, Utilities}
/**
  * Created by xuanbach on 2/15/16.
  */
object Main {
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
                // And in this diff, the only change chunk is the first one. Thus, it is aDiff.chunks(0)
                val removedLine = aDiff.chunks(0).changeLines.filter(line => line.isInstanceOf[LineRemoved])
                /**
                  * For now we only consider removed line.
                  * What if there is no removed line, instead, there is only one added line?
                  */
                var lineOfInterest = 0;
                var remLineString = "";
                var addLineString = ""
                if(removedLine.length == 1){
                  remLineString = removedLine(0).asInstanceOf[LineRemoved].line
                  lineOfInterest = aDiff.chunks(0).rangeInformation.oldOffset
                }else{// removedLine.length == 0
                  addLineString = aDiff.chunks(0).changeLines(0).line // only one line, and it is the added line
                  lineOfInterest = aDiff.chunks(0).rangeInformation.newOffset
                }
                //val contextVisitor = new FindContextVisitor(lineOfInterest, compUnit)
                //compUnit.getRoot.accept(contextVisitor)
                if(remLineString != ""){
                  Utilities.writeFile2Folder(remLineString, "removedLine.txt", countFd.getAbsolutePath+File.separator+"removedLine")
                }
                if(addLineString != ""){
                  Utilities.writeFile2Folder(remLineString, "addedLine.txt", countFd.getAbsolutePath+File.separator+"addedLine")
                }
                val oldFile = old.listFiles()(0)// the is only one file, so the index is 0
                val (befCtx, aftCtx) = Utilities.getContext(oldFile.getAbsolutePath, lineOfInterest-1)
                Utilities.writeFile2Folder(befCtx, "befContext.txt", countFd.getAbsolutePath+File.separator+"befContext")
                Utilities.writeFile2Folder(aftCtx, "aftContext.txt", countFd.getAbsolutePath+File.separator+"aftContext")

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
    processInput("/home/xuanbach/Desktop/data/alldata_withUnit/test")
  }
}
