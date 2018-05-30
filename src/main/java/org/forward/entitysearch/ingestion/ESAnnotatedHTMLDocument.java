package org.forward.entitysearch.ingestion;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;

import java.util.ArrayList;
import java.util.List;

public class ESAnnotatedHTMLDocument extends Annotation {

    /*
     * Potentially buggy. Do not consider text section here.
     */
    public ESAnnotatedHTMLDocument(List<CoreLabel> allTokens) {
        super("");
        List<CoreLabel> tokens = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int cur = 0;
        for (CoreLabel token : allTokens) {
            String txt = token.get(CoreAnnotations.TextAnnotation.class);
            text.append(txt + " ");
            token.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, cur);
            token.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, cur + txt.length());
            cur += txt.length() + 1;
            tokens.add(token);
        }
        this.set(CoreAnnotations.TokensAnnotation.class, tokens);
        this.set(CoreAnnotations.TextAnnotation.class, text.toString());
    }

    public ESAnnotatedHTMLDocument() {
        super("");
    }

    public void loadFromTokens(List<List<CoreLabel>> allTokens) {
        List<CoreLabel> tokens = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        int cur = 0;
        for (List<CoreLabel> list : allTokens) {
            for (CoreLabel token : list) {
                String txt = token.get(CoreAnnotations.TextAnnotation.class);
                text.append(txt + " ");
                token.set(CoreAnnotations.CharacterOffsetBeginAnnotation.class, cur);
                token.set(CoreAnnotations.CharacterOffsetEndAnnotation.class, cur + txt.length());
                cur += txt.length() + 1;
                tokens.add(token);
            }
        }
        this.set(CoreAnnotations.TokensAnnotation.class, tokens);
        this.set(CoreAnnotations.TextAnnotation.class, text.toString());
    }

    public void setURL(String url) {
        this.url = url;
    }

    public String getURL() {
        return this.url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return this.title;
    }

    public void setHeight (Integer h) {
        this.height = h;
    }

    public Integer getHeight () {
        return this.height;
    }

    public void setWidth (Integer h) {
        this.width = h;
    }

    public Integer getWidth () {
        return this.width;
    }

    private String url = null;
    private String title = null;
    private Integer height = 0;
    private Integer width = 0;
}
