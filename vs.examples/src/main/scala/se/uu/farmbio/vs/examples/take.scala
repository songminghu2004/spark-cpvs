package se.uu.farmbio.vs.examples

import org.apache.spark.Logging
import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import scopt.OptionParser
import se.uu.farmbio.vs.SBVSPipeline
import se.uu.farmbio.vs.PosePipeline
import java.io.PrintWriter

/**
 * @author laeeq
 */

object Take extends Logging {

  case class Arglist(
    master: String = null,
    conformersFile: String = null,
    sdfPath: String = null)

  def main(args: Array[String]) {
    val defaultParams = Arglist()
    val parser = new OptionParser[Arglist]("Take") {
      head("Counts number of molecules in conformer file")
      opt[String]("master")
        .text("spark master")
        .action((x, c) => c.copy(master = x))
      arg[String]("<conformers-file>")
        .required()
        .text("path to input SDF conformers file")
        .action((x, c) => c.copy(conformersFile = x))
      arg[String]("<sdf-Path>")
        .required()
        .text("path to subset SDF file")
        .action((x, c) => c.copy(sdfPath = x))
    }

    parser.parse(args, defaultParams).map { params =>
      run(params)
    } getOrElse {
      sys.exit(1)
    }
    System.exit(0)
  }

  def run(params: Arglist) {

    //Init Spark
    val conf = new SparkConf()
      .setAppName("Take")

    if (params.master != null) {
      conf.setMaster(params.master)
    }
    val sc = new SparkContext(conf)

    val mols = new SBVSPipeline(sc)
      .readConformerFile(params.conformersFile)
      .getMolecules
      .flatMap { mol => SBVSPipeline.splitSDFmolecules(mol.toString) }
    val parseScoreRDD = mols.map(PosePipeline.parseScore).cache
    val parseScoreHistogram = parseScoreRDD.histogram(10) // _.1 countains Range and _.2 contains Number of items in range
    
    val pw = new PrintWriter(params.sdfPath)
    parseScoreHistogram._2.foreach(pw.println(_))
    pw.close

    sc.stop()

  }

}

