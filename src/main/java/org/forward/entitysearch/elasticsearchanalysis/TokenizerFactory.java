package org.forward.entitysearch.elasticsearchanalysis;

import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.elasticsearch.SpecialPermission;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;

public class TokenizerFactory {

    private TokenizerFactory() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize");
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            // unprivileged code such as scripts do not have SpecialPermission
            sm.checkPermission(new SpecialPermission());
        }

        tokenizer = AccessController.doPrivileged(
                (PrivilegedAction<Annotator>) () -> new StanfordCoreNLP(props));
    }

    public static TokenizerFactory getInstance() {
        if (instance == null) {
            instance = new TokenizerFactory();
        }
        return instance;
    }

    public Annotator getTokenizer() {
        return tokenizer;
    }

    private static TokenizerFactory instance = null;
    private Annotator tokenizer;
}