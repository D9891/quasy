package de.tu.dresden.quasy.run

import java.io.{FileWriter, PrintWriter, File}
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.cluster.{SimpleBooleanClustering}
import de.tu.dresden.quasy.dependency.{DepNode, DependencyTree}
import de.tu.dresden.quasy.model.annotation.Sentence
import collection.mutable._
import collection.mutable
import de.tu.dresden.quasy.model.AnnotatedText

/**
 * @author dirk
 * Date: 4/19/13
 * Time: 2:54 PM
 */
object RunAnnotatedTextClustering {

    main(Array("/home/dirk/workspace/bioasq/annotations/question_corpus1000_stanford",
        "5",
        "/home/dirk/workspace/bioasq/clusters/question_corpus1000_stanford",
        "50"))

    def main(args:Array[String]) {
        val corpusDir = new File(args(0))
        val nrClusters = args(1).toInt
        val outputDir = new File(args(2))
        outputDir.mkdirs()
        val nrOfExamples = args(3).toInt

        val corpus = corpusDir.listFiles.map(file =>
            Xmlizer.fromFile[AnnotatedText](file).getAnnotations[Sentence].head).toList


        corpus.groupBy(_.getTokens.head.lemma).foreach(partialCorpus => {
            val alg =
                new SimpleBooleanClustering[Sentence]((sentence1:Sentence,sentence2:Sentence) => DependencyTree.checkEquality(sentence1.getDependencyTree,sentence2.getDependencyTree,DepNode.equalDependencyTag(_,_),1))

            val clusters = alg.cluster(partialCorpus._2, nrClusters)

            clusters.foreach(cluster => {
                val partialOutputDir = new File(outputDir,partialCorpus._1)
                partialOutputDir.mkdirs()
                var pw = new PrintWriter(new FileWriter(new File(partialOutputDir,"cluster_"+cluster.id+".tsv")))

                pw.println(cluster.id)
                pw.println(cluster.center.context.id +"\t"+cluster.center.coveredText)
                pw.println(cluster.variance)
                val candidates = alg.posteriors.filter(_._2.maxBy(_._2)._1.equals(cluster)).toSeq.sortBy(- _._2(cluster))
                pw.println(candidates.size)

                pw.println
                pw.println(cluster.center.getDependencyTree.prettyPrint)
                pw.println

                if (nrOfExamples > 0) {
                    candidates.take(nrOfExamples).foreach(example => {
                        val exampleTree = example._1
                        pw.println(example._2(cluster)+"\t"+exampleTree.coveredText)
                    })
                }

                pw.close()

                pw = new PrintWriter(new FileWriter(new File(partialOutputDir,"clustercenter_"+cluster.id+".xml")))
                pw.println(Xmlizer.toXml(cluster.center.context))

                pw.close
            })
        })

    }

}
