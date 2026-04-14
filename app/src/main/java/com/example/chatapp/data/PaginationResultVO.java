package com.example.chatapp.data;

import java.util.List;

public class PaginationResultVO<T> {
    private Integer count;
    private Integer pageSize;
    private Integer pageNo;
    private Integer pageTotal;
    private List<T> list;

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }

    public Integer getPageNo() { return pageNo; }
    public void setPageNo(Integer pageNo) { this.pageNo = pageNo; }

    public Integer getPageTotal() { return pageTotal; }
    public void setPageTotal(Integer pageTotal) { this.pageTotal = pageTotal; }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
}
