package config

import java.io.File
import java.util

import myLib.MyjavaLib

object RepairOptions {

  var homeFolder: String = ""
  var pkgInstrument: String = ""
  var sourceFolder: String = ""
  var filterFaultScore: Double = 0.5
  var root: String =  "/home/xuanbach/" //"/Users/larcuser" //"/home/xledinh" //  //
  var dependencies: String =root+"/workspace/historicalfixv2/src;"+root+"/workspace/historicalfixv2/allLibs/junit-4.11.jar;./" //;allLibs/hamcrest-core-1.3.jar
  var localLibs: String = ""
  var testFolder: String = ""
  var maxSolution: Int = 10//5
  //var nameFaultFile: String = ""
  //var nameFaultMethod: String = ""
  //var lowerBound: Int = 0
  //var upperBound: Int = 0
  var timeout: Int = 25
  var bugName: String ="m22"
  var libs = Array("/usr/lib/jvm/java-8-oracle/jre/lib/rt.jar") // /usr/java/jdk1.8.0_51/jre/lib/rt.jar

  var minedPatternFile = root+"/workspace/historicalfixv2/allLibs/workingdata/test.lg"
  var totalGraphs = 1
  val usage = """
  Usage: parser [-v] [-f file] [-s sopt] ...
  Where: -v   Run verbosely
       -f F Set input file to F
       -s S Set Show option to S
  """
  val JAVA_LANG: String = "java"


  val poolSize = 5 //10 // this is to multiply with the fault size. E.g., there are 5 buggy statements => size of pool = poolSize * 5 = 10 * 5 = 50
  val tournamentSize = 2 //3
  var variantOutputDir: String = root+"/workspace/historicalfixv2/tempOutput"
  var defaultVariantNumber = "default" //temp1
  var appClassDir = "target/classes" //(*math, closure*) "build/classes" //
  var testClassDir = "target/tests" //"target/test-classes" //"build/tests" //
  var thresholdFL = 0.005

  var testInvoker = "junit" //"maven" // bash, ant
  var testFilterStrat = "neg" // all
  private var dependenciesList: java.util.ArrayList[String] = null


  def testClassAbsoluteDir(): String = homeFolder+ File.separator + testClassDir
  def appClassAbsoluteDir(): String = homeFolder+ File.separator + appClassDir

  def getDependenciesList(): java.util.ArrayList[String] = {
    if(dependenciesList != null)
      return dependenciesList

    dependenciesList = dependencies.split(";").foldLeft(new java.util.ArrayList[String]()) {
      (res, dp) => {res.add(dp); res}
    }

    val depFromLocalLibs=localLibs.split(";").foldLeft(new java.util.ArrayList[String]()){
      (res, dp) =>{
        val foundJars = new util.ArrayList[File]()
        MyjavaLib.walk(dp,".jar",foundJars)
        import scala.collection.JavaConversions._
        for(aJar <- foundJars){
          res.add(aJar.getAbsolutePath)
        }
        res
      }
    }

    dependenciesList.addAll(depFromLocalLibs)
    dependenciesList.addAll(libs.foldLeft(new util.ArrayList[String]()){(res, path)=>{res.add(path);res}})
    println(dependenciesList)
    return dependenciesList
  }
}