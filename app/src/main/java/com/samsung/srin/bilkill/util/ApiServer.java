package com.samsung.srin.bilkill.util;

/**
 * Created by sonpx on 1/4/2018.
 */

public class ApiServer {
    private int status;

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private String name;


    public ApiServer( String name, int status) {
        this.status = status;
        this.name = name;
    }


}
