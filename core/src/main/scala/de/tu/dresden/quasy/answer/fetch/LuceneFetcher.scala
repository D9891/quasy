package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention, Question}
import org.apache.lucene.util.Version
import org.apache.lucene.queryParser.QueryParser
import de.tu.dresden.quasy.enhancer.EnhancementPipeline
import de.tu.dresden.quasy.model.AnnotatedText
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.KeywordAnalyzer
import java.text.Normalizer
import org.apache.lucene.search.Query
import de.tu.dresden.quasy.model.db.LuceneIndex

/**
 * @author dirk
 * Date: 5/22/13
 * Time: 10:45 AM
 */
class LuceneFetcher(luceneIndex:LuceneIndex) extends CandidateFetcher {

    private val LOG = LogFactory.getLog(this.getClass)

    def fetch(question: Question, docCount: Int, pipeline:EnhancementPipeline = null) = {
        val query: String =
            question.context.getAnnotationsBetween[OntologyEntityMention](question.begin,question.end).
                map(oem => "\""+oem.coveredText+"\"").mkString(" ")
        val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).parse(query)
        fetchWithQuery(q, docCount*2, question, pipeline)
    }                     //there are duplicates


    private def fetchWithQuery(q: Query, docCount: Int, question: Question, pipeline: EnhancementPipeline) = {
        LOG.info("Query becomes: " + q)
        val docs = luceneIndex.searcher.search(q, null, docCount)

        val texts = docs.scoreDocs.map(scoreDoc => {
            val doc = luceneIndex.searcher.doc(scoreDoc.doc)
            var text = doc.get("title") + "\n" + doc.get("contents")
            text = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", " ")
            new AnnotatedText(doc.get("pmid"), text)
        }).groupBy(_.id).map(_._2.head).take(docCount).toArray

        extractAnswerCandidates(texts, question, pipeline)
    }

    def fetchByPmid(question: Question, pmids:List[Int], pipeline:EnhancementPipeline = null) = {
        val query: String = pmids.map(_.toString).mkString(" ")
        val q = new QueryParser(Version.LUCENE_36, "pmid", new KeywordAnalyzer).parse(query)
        fetchWithQuery(q, Int.MaxValue, question, pipeline)
    }
}