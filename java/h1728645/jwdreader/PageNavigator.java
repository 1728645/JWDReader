package h1728645.jwdreader;


import java.util.List;
import java.util.ListIterator;

/**
 * Created by haotian on 12/11/16.
 *
 */

class PageNavigator {

    private ListIterator<PageContent> current_page;

    PageNavigator(List<PageContent> page_list, int last_reading_pos){
        current_page = page_list.listIterator(last_reading_pos);
    }

    PageContent currentPage(){
        PageContent display_page = current_page.next();
        current_page.previous();
        return display_page;
    }

    boolean hasCurrentPage(){
        return current_page.hasNext();
    }

    void moveNext(){
        current_page.next();
    }

    boolean hasNextPage(){
        current_page.next();
        boolean has_next_page = current_page.hasNext();
        current_page.previous();
        return has_next_page;
    }

    void movePrevious(){
        current_page.previous();
    }

    boolean hasPreviousPage(){
        return current_page.hasPrevious();
    }

}
