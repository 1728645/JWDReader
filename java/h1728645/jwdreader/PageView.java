package h1728645.jwdreader;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.widget.TextView;

import org.atilika.kuromoji.Token;

import com.mariten.kanatools.KanaConverter;

import java.util.ListIterator;

/**
 * Created by haotian on 12/10/16.
 *
 */

public class PageView extends TextView {

    //Why real?
    private int real_text_size = 0;
    private boolean dict_mode = false;
    private boolean show_dict_result = false;
    //Columns and rows of text
    private Point text_matrix_size = new Point(0,0);
    private int ruby_margin = 0;
    private int ruby_size = 0;
    private int page_num = 0;
    private PageContent page_content;

    public boolean isDictMode() {
        return dict_mode;
    }

    public void setDictMode(boolean dict_mode) {
        this.dict_mode = dict_mode;
        invalidate();
    }

    public boolean isShowDictResult() {
        return show_dict_result;
    }

    public void setShowDictResult(boolean show_dict_result) {
        this.show_dict_result = show_dict_result;
    }

    public PageContent getPageContent() {
        return page_content;
    }

    public void setPageContent(PageContent page_content) {
        this.page_content = page_content;
    }

    public int getRubyMargin() {
        return ruby_margin;
    }

    public void setRubyMargin(int ruby_margin) {
        this.ruby_margin = ruby_margin;
    }

    public int getRubySize() {
        return ruby_size;
    }

    public void setRubySize(int ruby_size) {
        this.ruby_size = ruby_size;
    }

    public int getRealTextSize() {
        return real_text_size;
    }

    public void setRealTextSize(int realSize){
        real_text_size = realSize;
    }

    public Point getTextMatrixSize() {
        return text_matrix_size;
    }

    public void setTextMatrixSize(Point text_matrix_size) {
        this.text_matrix_size = text_matrix_size;
    }

    public int getPageNum() {
        return page_num;
    }

    public void setPageNum(int page_num) {
        this.page_num = page_num;
    }

    public PageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public PageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PageView(Context context) {
        super(context);
    }



    @Override
    protected void onDraw(Canvas canvas) {
        final TextPaint paint = getPaint();
        paint.setTextSize(real_text_size/2);
        canvas.drawText(" " + page_content.getPageNum() + " / " + page_num, 0, this.getBottom(), paint);

        paint.setTextSize(real_text_size);
        canvas.translate(this.getRight() - real_text_size - ruby_margin,
                real_text_size);
        if (page_content == null) {
            return;
        }

        ListIterator<Token> iterator = page_content.getTokens().listIterator();

        if(!iterator.hasNext()){
            //SHOULD NEVER HAPPEN
            return;
        }

        //Handle first token
        Token token = iterator.next();

        int start_row = 0, start_column = 0;
        int current_row = 0, current_column = 0;
        String text = token.getSurfaceForm();
        String katagana = token.getReading();
        String hiragana;
        int start_pos = page_content.getStartPos();
        int first_line_offset = 0;
        if(token.getPosition() > 0){
            if(start_pos > 0){
                first_line_offset = token.getPosition() + start_pos;
            }
            else{
                first_line_offset = token.getPosition();
            }
        }
        for(int j = start_pos; j < text.length(); j++){
            current_column = (start_row + j - start_pos) / text_matrix_size.y + start_column;
            if(current_column >= text_matrix_size.x){
                break;
            }
            current_row = (start_row + j - start_pos) % text_matrix_size.y;
            canvas.drawText(text.charAt(j) + "", - current_column * (real_text_size + ruby_margin),
                    current_row * real_text_size, paint);
        }
        int ruby_x, ruby_y, ruby_height;
        if(dict_mode && katagana != null){
            hiragana = KanaConverter.convertKana(katagana, KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA);
            if (hiragana != null && !text.equals(hiragana)) {
                paint.setTextSize(ruby_size);
                canvas.translate(real_text_size + ruby_margin, -real_text_size);
                ruby_height = (text.length() * real_text_size + ruby_size) / (hiragana.length() + 1);
                for (int j = 0; j < hiragana.length(); j++) {
                    if (start_pos == 0) {
                        ruby_y = (j + 1) * ruby_height;
                    } else {
                        ruby_y = (start_pos - text.length()) * real_text_size + (j + 1) * ruby_height;
                    }
                    ruby_x = (-real_text_size - ruby_margin) *
                            (ruby_y / (text_matrix_size.y * real_text_size)) - ruby_margin;
                    if (ruby_x >= (-real_text_size - ruby_margin) * text_matrix_size.x) {
                        ruby_y = ruby_y % (text_matrix_size.y * real_text_size);
                        canvas.drawText(hiragana.charAt(j) + "", ruby_x, ruby_y, paint);
                    }
                }
                paint.setTextSize(real_text_size);
                canvas.translate(-real_text_size - ruby_margin, real_text_size);
            }
        }

        //Other token
        while(iterator.hasNext()){
            token = iterator.next();
            text = token.getSurfaceForm();
            katagana = token.getReading();
            start_row = token.getPosition();
            if (start_row == 0){
                start_column = current_column + 1;
                first_line_offset = 0;
            }
            start_row -= first_line_offset;
            for(int j = 0; j < text.length(); j++){
                current_column = (start_row + j) / text_matrix_size.y + start_column;
                if(current_column >= text_matrix_size.x){
                    break;
                }
                current_row = (start_row + j) % text_matrix_size.y;
                canvas.drawText(text.charAt(j) + "", - current_column * (real_text_size + ruby_margin),
                        current_row * real_text_size, paint);
            }

            if(dict_mode && katagana != null){
                hiragana = KanaConverter.convertKana(katagana, KanaConverter.OP_ZEN_KATA_TO_ZEN_HIRA);
                if (hiragana != null && !text.equals(hiragana)) {
                    paint.setTextSize(ruby_size);
                    canvas.translate(real_text_size + ruby_margin, -real_text_size);
                    ruby_height = (text.length() * real_text_size + ruby_size) / (hiragana.length() + 1);
                    for (int j = 0; j < hiragana.length(); j++) {
                        ruby_y = start_row * real_text_size + (j + 1) * ruby_height;
                        ruby_x = (-real_text_size - ruby_margin) *
                                (start_column + ruby_y / (text_matrix_size.y * real_text_size)) - ruby_margin;
                        if (ruby_x >= (-real_text_size - ruby_margin) * text_matrix_size.x) {
                            ruby_y = ruby_y % (text_matrix_size.y * real_text_size);
                            canvas.drawText(hiragana.charAt(j) + "", ruby_x, ruby_y, paint);
                        }
                    }
                    paint.setTextSize(real_text_size);
                    canvas.translate(-real_text_size - ruby_margin, real_text_size);
                }
            }
        }
    }
}
