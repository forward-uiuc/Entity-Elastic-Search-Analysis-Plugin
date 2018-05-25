package org.elasticsearch.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.elasticsearch.Version;
import org.elasticsearch.cluster.metadata.IndexMetaData;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.Index;
import org.elasticsearch.test.ESTestCase;
import org.forward.entitysearch.entitylayoutanalysis.AnalysisEntityLayoutPlugin;
import org.forward.entitysearch.entitylayoutanalysis.EntityLayoutAnalyzer;
import org.forward.entitysearch.entitylayoutanalysis.EntityLayoutTokenizer;
import org.forward.entitysearch.entitylayoutanalysis.EntityLayoutTokenizerFactory;

import java.io.IOException;
import java.io.StringReader;

import static org.hamcrest.Matchers.*;
import static org.apache.lucene.analysis.BaseTokenStreamTestCase.assertTokenStreamContents;

/**
 * Created by duydo on 2/19/17.
 */
public class VietnameseAnalysisTest extends ESTestCase {

    public void testSimpleVietnameseAnalysis() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        assertNotNull(analysis);

        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("vi_tokenizer");
        assertNotNull(tokenizerFactory);
        assertThat(tokenizerFactory, instanceOf(EntityLayoutTokenizerFactory.class));

        NamedAnalyzer analyzer = analysis.indexAnalyzers.get("vi_analyzer");
        assertNotNull(analyzer);
        assertThat(analyzer.analyzer(), instanceOf(EntityLayoutAnalyzer.class));

        analyzer = analysis.indexAnalyzers.get("my_analyzer");
        assertNotNull(analyzer);
        assertThat(analyzer.analyzer(), instanceOf(CustomAnalyzer.class));
        assertThat(analyzer.analyzer().tokenStream(null, new StringReader("")), instanceOf(EntityLayoutTokenizer.class));

    }


    public void testVietnameseTokenizer() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        TokenizerFactory tokenizerFactory = analysis.tokenizer.get("vi_tokenizer");
        assertNotNull(tokenizerFactory);

        Tokenizer tokenizer = tokenizerFactory.create();
        assertNotNull(tokenizer);

        tokenizer.setReader(new StringReader("Công nghệ thông tin Việt Nam"));
        assertTokenStreamContents(tokenizer, new String[]{"Công nghệ thông tin", "Việt Nam"});
    }

    public void testVietnameseAnalyzer() throws IOException {
        TestAnalysis analysis = createTestAnalysis();
        NamedAnalyzer analyzer = analysis.indexAnalyzers.get("vi_analyzer");
        assertNotNull(analyzer);

        TokenStream ts = analyzer.analyzer().tokenStream("test", "Công nghệ thông tin Việt Nam");
        CharTermAttribute term = ts.addAttribute(CharTermAttribute.class);
        ts.reset();
        for (String expected : new String[]{"công nghệ thông tin", "việt nam"}) {
            assertThat(ts.incrementToken(), equalTo(true));
            assertThat(term.toString(), equalTo(expected));
        }
        assertThat(ts.incrementToken(), equalTo(false));
    }

    public TestAnalysis createTestAnalysis() throws IOException {
        String json = "/org/elasticsearch/index/analysis/vi_analysis.json";
        Settings settings = Settings.builder()
                .loadFromStream(json, VietnameseAnalysisTest.class.getResourceAsStream(json))
                .put(IndexMetaData.SETTING_VERSION_CREATED, Version.CURRENT)
                .build();
        Settings nodeSettings = Settings.builder().put(Environment.PATH_HOME_SETTING.getKey(), createTempDir()).build();
        return createTestAnalysis(new Index("test", "_na_"), nodeSettings, settings, new AnalysisEntityLayoutPlugin());
    }
}
