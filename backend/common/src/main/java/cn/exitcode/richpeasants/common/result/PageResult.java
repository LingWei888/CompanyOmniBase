package cn.exitcode.richpeasants.common.result;

import org.springframework.data.domain.Page;

import java.util.List;

public class PageResult<T> {

    private List<T> records;
    private long total;
    private int page;
    private int size;
    private int totalPages;

    public static <T> PageResult<T> from(Page<T> pageData) {
        PageResult<T> result = new PageResult<>();
        result.records = pageData.getContent();
        result.total = pageData.getTotalElements();
        result.page = pageData.getNumber() + 1;
        result.size = pageData.getSize();
        result.totalPages = pageData.getTotalPages();
        return result;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
