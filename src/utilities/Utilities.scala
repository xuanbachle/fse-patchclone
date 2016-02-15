package utilities

import java.io.File

import myLib.Lib
import utilities.Utilities

import scala.collection.mutable.ArrayBuffer

/**
  * Created by xuanbach on 2/15/16.
  */
object Utilities {

  def writeFile2Folder(content: String, fileName: String, folder: String) ={
    val fd = new File(folder)
    if(! fd.exists()) {
      fd.mkdirs()
    }
    myLib.Lib.writeText2File(content, new File(fd.getAbsolutePath+File.separator+fileName))
  }

  def listFolders(rootFolder: File): Array[File] = {
    val directories: Array[File] = rootFolder.listFiles().filter(file => file.isDirectory)
    return directories
  }

  def fixOldFolders(countFolder: File): (File, File) ={
    val arr = countFolder.listFiles().filter(file => file.isDirectory).foldLeft(new Array[File](2)){
      (res, afile) =>{
        if(afile.getAbsolutePath.contains("fix")){
          res(0) = afile
        }else if(afile.getAbsolutePath.contains("old")){
          res(1) = afile
        }
        res
      }
    }
    return (arr(0), arr(1))// fix and old folders
  }

  def modifiedFilesFolder(projectFolder: File): File ={
    return projectFolder.listFiles().filter(file => file.isDirectory).filter(file => file.getAbsolutePath.contains("modifiedFiles"))(0)
  }

  def withUnitDiffFolder(projectFolder: File): File ={
    val allFolders = projectFolder.listFiles().filter(file => file.isDirectory)
    //filter(file => file.getAbsolutePath.contains("withUnit\\_diff"))(0)
    return allFolders.filter(folder =>  folder.getName.contains("withUnit") && folder.getName.contains("diff"))(0)
  }

  def diffFileWithCount(withUnitDiffFolder: File, count: String): File ={
     return withUnitDiffFolder.listFiles().filter(file => file.getName.compareTo(count+".diff") == 0)(0)
  }

  def isNotTestFile(f: File, ext: String =".java"): Boolean ={
    return !f.getName.endsWith("TestPermutations"+ext) &&
      !f.getName.endsWith("Test"+ext) && !f.getName.endsWith("TestCase"+ext) &&
      !f.getName.endsWith("AbstractTest"+ext) && f.getName.endsWith(ext) && !f.getName.endsWith("~") &&
      !f.getName.endsWith("Tests"+ext) && !f.getName.startsWith("Test")
  }

  def getContext(inputFile: String, lineNum: Int): (String, String) ={
    val lines = myLib.Lib.readFile2Lines(new File(inputFile))
    var i = -1;
    var befContext = ""
    var aftContext = ""
    lines.map { line => {
        i += 1
        if (i >= lineNum - 3 && i < lineNum) {
          befContext += lines(i) +"\n"
        } else if (i > lineNum && i <= lineNum + 3) {
          aftContext += lines(i)+ "\n"
        }
      }
    }
    return (befContext, aftContext)
  }
}
