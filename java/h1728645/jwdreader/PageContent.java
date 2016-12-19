package h1728645.jwdreader;


/**
 * Created by haotian on 12/10/16.
 *
 */

import java.util.List;

import org.atilika.kuromoji.Token;

class PageContent {

    private int start_pos;
    private List<Token> tokens;

    int getStartPos() {
        return start_pos;
    }

    void setStartPos(int start_pos) {
        this.start_pos = start_pos;
    }

    List<Token> getTokens() {
        return tokens;
    }

    void setTokens(List<Token> tokens) {
        this.tokens = tokens;
    }

    PageContent(int start_pos, List<Token> tokens){
        this.start_pos = start_pos;
        this.tokens = tokens;
    }
}
