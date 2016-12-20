package h1728645.jwdreader;


import android.content.Context;
import android.content.SharedPreferences;
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
    public static final String SETTINGS = "JWDREADER_SETTINGS";
    public static final String GLOBAL_FONT_SIZE = "GLOBAL_FONT_SIZE";
    public static final String FONT_PREFIX = "FONT_";
    public static final String LRP_PREFIX = "LRP_";//Last reading position prefix
    public static final String BOOK_FOLDER = "Book";
    public static final String DICT_FILE = "Book/dict";
    public static final float RUBY_RATIO = 0.4f;
    public static final float RUBY_MARGIN = 0.6f;
    public static final int INIT_FONT_SIZE = 65;
    public SharedPreferences settings;
    public SharedPreferences.Editor setting_editor;
    public File book_folder;
    public int font_size;
    public int last_reading_pos;
    //For ruby text
    public int ruby_margin;
    public int ruby_size;
    public int status_bar_height;
    public Point page_size;
    public Point dict_button_size;
    public Point text_matrix_size;
    public LinkedList<PageContent> page_list;
    public PageNavigator current_page;
    public String book_name;
    public Tokenizer tokenizer;
    public List<Token> book_content;

    public void readBook(File book){
        book_content = new LinkedList<>();
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
        page_list = new LinkedList<>();
        int start_index = 0, end_index = 0, start_pos =0;
        int current_column = -1, current_row = 0;
        for (Token token : book_content){
            if(token.getPosition() == 0){
                current_column++;
                current_row = 0;
            }
            current_column += (current_row + token.getSurfaceForm().length()) / text_matrix_size.y;
            current_row = (current_row + token.getSurfaceForm().length()) % text_matrix_size.y;
            while (current_column >= text_matrix_size.x){
                current_column -= text_matrix_size.x;
                //Current page contains the last token of previous page.
                page_list.add(new PageContent(start_pos, book_content.subList(start_index, end_index + 1)));
                start_index = end_index;
                start_pos = token.getSurfaceForm().length() - current_row - current_column * text_matrix_size.y;
            }
            end_index++;
        }
        //Last page
        if(start_index < end_index) {
            page_list.add(new PageContent(start_pos, book_content.subList(start_index, end_index)));
        }
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

    public void openBook(View view){
        JWDReader context = (JWDReader) view.getContext();
        book_name = ((Button) view).getText().toString();
        File book = new File(context.book_folder.getAbsolutePath()+ "/" + book_name);
        if(!book.exists()) {//TODO:Popup a warning or something
            return;
        }
        context.setContentView(R.layout.book_content);

        PageView page_view = (PageView) findViewById(R.id.book_page_view);
        page_view.getLayoutParams().width = page_size.x + font_size;
        page_view.getLayoutParams().height = page_size.y + font_size;
        page_view.setTextMatrixSize(text_matrix_size);
        page_view.requestLayout();
        page_view.setOnTouchListener(navigate_page);

        ToggleButton dict_button = (ToggleButton) findViewById(R.id.dict_button);
        dict_button.getLayoutParams().width = dict_button_size.y;
        dict_button.getLayoutParams().height = dict_button_size.y;
        dict_button.setTextSize(TypedValue.COMPLEX_UNIT_PX, Math.round(dict_button_size.y * 0.5f));
        dict_button.requestLayout();

        TextView dict_result_view = (TextView) findViewById(R.id.dict_result_view);
        dict_result_view.getLayoutParams().width = page_size.x;
        dict_result_view.getLayoutParams().height = page_size.y / 2;
        dict_result_view.setX(font_size / 2);
        dict_result_view.setBackgroundColor(Color.CYAN);
        dict_result_view.setTextSize(TypedValue.COMPLEX_UNIT_PX, font_size);
        dict_result_view.requestLayout();

        dict_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                ToggleButton dict_button = (ToggleButton) view;
                PageView page_view = (PageView) findViewById(R.id.book_page_view);
                if (dict_button.isChecked()) {
                    page_view.setDictMode(true);
                } else {
                    page_view.setDictMode(false);
                    if (page_view.isShowDictResult()) {
                        TextView dict_result_view = (TextView) findViewById(R.id.dict_result_view);
                        dict_result_view.setVisibility(View.INVISIBLE);
                        page_view.setShowDictResult(false);
                    }
                }
            }
        });

        page_view.setRubyMargin(context.ruby_margin);
        page_view.setRubySize(context.ruby_size);


        int last_reading_font_size = settings.getInt(FONT_PREFIX + book_name, font_size);
        last_reading_pos = settings.getInt(LRP_PREFIX + book_name, 0);
        if(last_reading_font_size != font_size){
            //TODO: Recalculate the reading position
        }
        context.readBook(book);
        context.pageBook();

        page_view.setRealTextSize(context.font_size);
        if(last_reading_pos >= page_list.size()){
            last_reading_pos = 0;
        }

        current_page = new PageNavigator(page_list, last_reading_pos);

        //Error in hasCurrentPage
        if (context.current_page.hasCurrentPage()) {
            page_view.setPageContent(context.current_page.currentPage());
        }
    }

    private View.OnClickListener book_selector = new View.OnClickListener() {
        public void onClick(View view) {
            JWDReader context = (JWDReader) view.getContext();
            context.openBook(view);
        }
    };

    public boolean navigatePage(View view, MotionEvent event) {
        JWDReader context = (JWDReader) view.getContext();
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
                    int start_pos = page_view.getPageContent().getStartPos();
                    int first_line_offset = 0;
                    if(token.getPosition() > 0){
                        if(start_pos > 0){
                            first_line_offset = token.getPosition() + start_pos;
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
                            if (pos >= start_pos && pos < start_pos + token.getSurfaceForm().length()) {
                                base_text = token.getBaseForm();
                                break;
                            }
                            end_column = (start_pos + token.getSurfaceForm().length() -1)
                                    /page_view.getTextMatrixSize().y;
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
                        last_reading_pos++;
                        page_view.setPageContent(context.current_page.currentPage());
                        page_view.invalidate();
                    }
                }
                else {
                    if (context.current_page.hasPreviousPage()) {
                        context.current_page.movePrevious();
                        last_reading_pos--;
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

    public void listBook(){

        this.setContentView(R.layout.book_list);

        LinearLayout book_list_layout = (LinearLayout)findViewById(R.id.book_list);
        book_folder = new File(android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + BOOK_FOLDER);

        if (android.os.Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
        {
            if( book_folder.exists() && book_folder.isDirectory() ) {
                String[] book_list = book_folder.list();
                if( book_list != null) {
                    for (String book_name : book_list) {
                        if(book_name.endsWith(".txt")){
                            Button book = new Button(this);
                            book.setText(book_name);
                            book.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT));
                            book.setOnClickListener(book_selector);
                            book_list_layout.addView(book);
                        }
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
    protected void onCreate(Bundle savedInstanceState) {
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE ;
        decorView.setSystemUiVisibility(uiOptions);

        status_bar_height = getStatusBarHeight();

        super.onCreate(savedInstanceState);

        Display display = getWindowManager().getDefaultDisplay();
        Point display_size = new Point();
        display.getSize(display_size);


        settings = getSharedPreferences(SETTINGS, Context.MODE_PRIVATE);
        setting_editor = settings.edit();
        font_size = settings.getInt(GLOBAL_FONT_SIZE, INIT_FONT_SIZE);

        dict_button_size = new Point(display_size.x, (display_size.y - status_bar_height)/10);
        page_size = new Point(display_size.x - font_size,
                display_size.y - status_bar_height - dict_button_size.y - font_size);

        ruby_margin = Math.round(font_size * RUBY_MARGIN);
        ruby_size = Math.round(font_size * RUBY_RATIO);

        text_matrix_size = new Point(page_size.x/(font_size + ruby_margin), page_size.y/font_size);

        tokenizer = Tokenizer.builder().build();

        listBook();

    }

    @Override
    protected void onResume(){
        View decorView = getWindow().getDecorView();
        // Hide the status bar.
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE ;
        decorView.setSystemUiVisibility(uiOptions);

        super.onResume();
    }

    @Override
    protected void onStop(){
        super.onStop();
        setting_editor.putInt(LRP_PREFIX + book_name, last_reading_pos);
        setting_editor.commit();
    }

    @Override
    public void onBackPressed(){
        listBook();
    }
}
