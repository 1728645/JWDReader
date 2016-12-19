package h1728645.jwdreader;


import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.atilika.kuromoji.Tokenizer;
import org.atilika.kuromoji.Token;


public class JWDReader extends AppCompatActivity {

    public static final String BOOK_FOLDER = "Book";
    public static final String DICT_FILE = "Book/dict";
    public static final float RUBY_RATIO = 0.4f;
    public static final float RUBY_MARGIN = 0.6f;
    public static final int INIT_FONT_SIZE = 65;
    public File book_folder;
    public int font_size;
    //For ruby text
    public int ruby_margin;
    public int ruby_size;
    public int status_bar_height;
    public Point page_size;
    public Point dict_button_size;
    public Point text_matrix_size;
    //public PageContent page_content;
    public LinkedList<PageContent> page_list;
    public PageNavigator current_page;
    public Tokenizer tokenizer;
    public List<Token> book_content;

    public void readBook(File book){
        try {
            BufferedReader br = new BufferedReader(new FileReader(book.getAbsolutePath()));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                if(line.equals("")){
                    book_content.addAll(tokenizer.tokenize(" "));
                }
                else{
                    book_content.addAll(tokenizer.tokenize(line));
                }
            }
        }
        catch (FileNotFoundException e){
            //TODO: Handle this exception
        }
        catch (IOException e){
            //TODO: Handle this exception
        }
    }

    public void pageBook(){
        int start_index = 0, end_index = 0, start_pos =0;
        int current_column = -1, current_row = 0;
        for (Token token : book_content){
            if(token.getPosition() == 0){
                current_column++;
                current_row = 0;
            }
            current_column += (current_row + token.getSurfaceForm().length()) / text_matrix_size.y;
            current_row = (current_row + token.getSurfaceForm().length()) % text_matrix_size.y;
            if (current_column >= text_matrix_size.x){
                page_list.add(new PageContent(start_pos, book_content.subList(start_index, end_index + 1)));
                if (current_row == 0){
                    start_index = end_index + 1;
                }
                else{
                    start_index = end_index;
                    start_pos = token.getSurfaceForm().length() - current_row;
                }
                current_column = current_column%text_matrix_size.x;
            }
            end_index++;
        }
        if(start_index < end_index) {
            page_list.add(new PageContent(start_pos, book_content.subList(start_index, end_index)));
        }
        current_page = new PageNavigator(page_list.listIterator());
    }

    public String lookUpDict(String word){
        File dict = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + DICT_FILE);
        if (dict.exists()){
            /*try {
                BufferedReader br = new BufferedReader(new FileReader(dict.getAbsolutePath()));
                for(String line = br.readLine(); line != null; line = br.readLine()){
                    if(line.equals("<keb>" + word + "</keb>")){
                        String result = "";
                        result += line + "\n";
                        line = br.readLine();
                        while(!(line == null) && !line.equals("</entry>")){
                            if (line.contains("<reb>") || line.contains("<gloss>")){
                                result += line + "\n";
                            }
                            line = br.readLine();
                        }
                        br.close();
                        return result;
                    }
                }
            }
            catch(Exception e){
                return "Exception:" + e.toString();
            }*/
            return "Word <" + word + "> not found";
        }
        else{
            return "Dict not found";
        }
    }

    public void selectBook(View view){
        JWDReader context = (JWDReader) view.getContext();
        context.setContentView(R.layout.book_content);

        PageView page_view = (PageView)findViewById(R.id.book_page_view);
        page_view.getLayoutParams().width = page_size.x + font_size;
        page_view.getLayoutParams().height = page_size.y + font_size;
        page_view.setTextMatrixSize(text_matrix_size);
        page_view.requestLayout();
        page_view.setOnTouchListener(navigate_page);

        ToggleButton dict_button = (ToggleButton)findViewById(R.id.dict_button);
        dict_button.getLayoutParams().width = dict_button_size.y;
        dict_button.getLayoutParams().height = dict_button_size.y;
        dict_button.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.round(dict_button_size.y * 0.5f));
        dict_button.requestLayout();

        TextView dict_result_view = (TextView)findViewById(R.id.dict_result_view);
        dict_result_view.getLayoutParams().width = page_size.x;
        dict_result_view.getLayoutParams().height = page_size.y/2;
        dict_result_view.setX(font_size/2);
        dict_result_view.setBackgroundColor(Color.CYAN);
        dict_result_view.setTextSize(TypedValue.COMPLEX_UNIT_PX, font_size);
        dict_result_view.requestLayout();

        dict_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ToggleButton dict_button = (ToggleButton) view;
                PageView page_view = (PageView)findViewById(R.id.book_page_view);
                if (dict_button.isChecked()){
                    page_view.setDictMode(true);
                }
                else{
                    page_view.setDictMode(false);
                    if(page_view.isShowDictResult()){
                        TextView dict_result_view = (TextView)findViewById(R.id.dict_result_view);
                        dict_result_view.setVisibility(View.INVISIBLE);
                        page_view.setShowDictResult(false);
                    }
                }
            }
        });

        page_view.setRubyMargin(context.ruby_margin);
        page_view.setRubySize(context.ruby_size);
        CharSequence file_name = ((Button) view).getText();
        File book = new File(context.book_folder.getAbsolutePath()+ "/" + file_name);
        if(book.exists()) {
            context.readBook(book);
            context.pageBook();
            page_view.setRealTextSize(context.font_size);
            //Error in has current page
            if (context.current_page.hasCurrentPage()){
                page_view.setPageContent(context.current_page.currentPage());
            }
        }
    }

    private View.OnClickListener book_selector = new View.OnClickListener() {
        public void onClick(View view) {
            JWDReader context = (JWDReader) view.getContext();
            context.selectBook(view);
        }
    };

    public boolean navigatePage(View view, MotionEvent event) {
        JWDReader context = (JWDReader) view.getContext();
        //context.setContentView(R.layout.book_page);
        PageView page_view = (PageView)findViewById(R.id.book_page_view);
        if(event.getAction() == MotionEvent.ACTION_UP){
            if(page_view.isDictMode()) {
                TextView dict_result_view = (TextView)findViewById(R.id.dict_result_view);
                if(page_view.isShowDictResult()){
                    dict_result_view.setVisibility(View.INVISIBLE);
                    page_view.setShowDictResult(false);
                }
                else{
                    int pos = (page_view.getRight() - page_view.getRealTextSize()/2 - Math.round(event.getX()))/
                            (page_view.getRealTextSize() + page_view.getRubyMargin()) * page_view.getTextMatrixSize().y +
                            (Math.round(event.getY()) - page_view.getRealTextSize()/2)/page_view.getRealTextSize();
                    String base_text = null;

                    ListIterator<Token> iterator = page_view.getPageContent().getTokens().listIterator();

                    if(!iterator.hasNext()){
                        //SHOULD NEVER HAPPEN
                        return true;
                    }

                    //Handle first token
                    Token token = iterator.next();

                    int start_column = 0;
                    int end_column = 0;
                    int end_pos = 0;
                    int start_pos = page_view.getPageContent().getStartPos();
                    int first_line_offset = 0;
                    if(token.getPosition() > 0){
                        if(start_pos > 0){
                            first_line_offset = token.getPosition() + token.getSurfaceForm().length() - start_pos;
                        }
                        else{
                            first_line_offset = token.getPosition();
                        }
                    }
                    //First token
                    if (pos >= 0 && pos < token.getSurfaceForm().length() - start_pos){
                        base_text = token.getBaseForm();
                    }
                    else {//Other token
                        while (iterator.hasNext()) {
                            token = iterator.next();
                            start_pos = token.getPosition();
                            if (start_pos == 0) {
                                start_column = end_column + 1;
                                first_line_offset = 0;
                            }
                            start_pos -= first_line_offset;
                            start_pos += start_column * page_view.getTextMatrixSize().y;
                            end_pos = start_pos + token.getSurfaceForm().length();
                            if (pos >= start_pos && pos < end_pos) {
                                base_text = token.getBaseForm();
                                break;
                            }
                            end_column = end_pos/page_view.getTextMatrixSize().y;
                        }
                    }

                    if(base_text != null){
                        dict_result_view.setText(context.lookUpDict(base_text));
                        if (event.getY() < context.page_size.y/2) {
                            dict_result_view.setY(context.page_size.y/2);
                            dict_result_view.requestLayout();
                        }
                        else{
                            dict_result_view.setY(0);
                            dict_result_view.requestLayout();
                        }
                        dict_result_view.setVisibility(View.VISIBLE);
                        page_view.setShowDictResult(true);
                    }
                }

            }
            else{
                if (event.getX() < context.page_size.x / 2) {
                    if (context.current_page.hasNextPage()) {
                        context.current_page.moveNext();
                        page_view.setPageContent(context.current_page.currentPage());
                        page_view.invalidate();
                    }
                }
                else {
                    if (context.current_page.hasPreviousPage()) {
                        context.current_page.movePrevious();
                        page_view.setPageContent(context.current_page.currentPage());
                        page_view.invalidate();
                    }
                }
            }
        }
        return true;
    }

    private View.OnTouchListener navigate_page = new View.OnTouchListener() {
        public boolean onTouch(View view, MotionEvent event) {
            JWDReader context = (JWDReader) view.getContext();
            return context.navigatePage(view, event);
        }
    };

    public int getStatusBarHeight() {
        Resources resources = this.getResources();
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    public int getNavigationBarHeight() {
        Resources resources = this.getResources();
        int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE ;
        decorView.setSystemUiVisibility(uiOptions);

        status_bar_height = getStatusBarHeight();

        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.book_list);

        Display display = getWindowManager().getDefaultDisplay();
        Point display_size = new Point();
        display.getSize(display_size);

        page_list = new LinkedList<>();
        book_content = new LinkedList<>();
        current_page = new PageNavigator(page_list.listIterator());

        font_size = INIT_FONT_SIZE;
        dict_button_size = new Point(display_size.x, (display_size.y - status_bar_height)/10);
        page_size = new Point(display_size.x - font_size,
                display_size.y - status_bar_height - dict_button_size.y - font_size);

        ruby_margin = Math.round(font_size * RUBY_MARGIN);
        ruby_size = Math.round(font_size * RUBY_RATIO);

        text_matrix_size = new Point(page_size.x/(font_size + ruby_margin), page_size.y/font_size);


        tokenizer = Tokenizer.builder().build();

        LinearLayout book_list_layout = (LinearLayout)findViewById(R.id.book_list);

        book_folder = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + BOOK_FOLDER);
        String out_msg = "";
        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
        {
            if( book_folder.exists() && book_folder.isDirectory() ) {
                String[] filesAndDirectories = book_folder.list();
                if( filesAndDirectories != null) {
                    for (String fileOrDirectory : filesAndDirectories) {
                        out_msg = out_msg + fileOrDirectory + "\n";
                        Button btn = new Button(this);
                        btn.setText(fileOrDirectory);
                        btn.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT));
                        btn.setOnClickListener(book_selector);
                        book_list_layout.addView(btn);
                    }
                }
                else {
                    //TODO: Request permission
                    TextView textView = new TextView(this);
                    textView.setTextSize(20);
                    textView.setText(book_folder.getAbsolutePath());
                    book_list_layout.addView(textView);
                }
            }
        }
    }

    @Override
    protected void onResume(){
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE ;
        decorView.setSystemUiVisibility(uiOptions);

        super.onResume();
    }

}
