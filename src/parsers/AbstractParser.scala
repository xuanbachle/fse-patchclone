package parsers

import java.io.{BufferedReader, File, FileReader, IOException}
import java.util
import java.util.Hashtable

import utilities.Utilities

import scala.collection.mutable.ArrayBuffer


/**
 * Created by xuanbach32bit on 5/17/15.
 */
abstract class AbstractParser[T] {
  //keep ASTs of all files being parsed
  val globalASTs: util.Hashtable[String, T] = new Hashtable[String, T]

  def parse[B >: T](fileContent:String, filePath: String): T

  @throws(classOf[IOException])
  protected def readFileToString(filePath: String): String = {
    val fileData: StringBuilder = new StringBuilder(1000)
    val reader: BufferedReader = new BufferedReader(new FileReader(filePath))
    var buf: Array[Char] = new Array[Char](10)
    var numRead: Int = 0
    while ((({
      numRead = reader.read(buf); numRead
    })) != -1) {
      //System.out.println(numRead)
      val readData: String = String.valueOf(buf, 0, numRead)
      fileData.append(readData)
      buf = new Array[Char](1024)
    }
    reader.close()

    return fileData.toString
  }

  @throws(classOf[IOException])
  def ParseFiles(files: util.List[File]) = {
    import scala.collection.JavaConversions._
    for(f <- files){
      val filePath = f.getAbsolutePath
      if (f.isFile) {
        if(Utilities.isNotTestFile(f)) {
          //System.out.println("Parsing: " + filePath)
          val cu = parse(readFileToString(filePath), filePath)
          globalASTs.put(f.getName, cu)
        }
      }
    }
  }

}
