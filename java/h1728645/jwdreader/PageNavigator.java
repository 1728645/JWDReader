package h1728645.jwdreader;


import java.util.ListIterator;

/**
 * Created by haotian on 12/11/16.
 *
 */

class PageNavigator {

    private ListIterator<PageContent> current_page;

    PageNavigator(ListIterator<PageContent> current_page){
        this.current_page = current_page;
    }

    /*public PageNavigator(){
        //this.current_page = new Iterator<>();
    }*/

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
