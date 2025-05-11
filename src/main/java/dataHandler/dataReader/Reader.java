package dataHandler.dataReader;

import dataHandler.dataSet.DataSet;

import java.io.IOException;

public abstract class Reader <T extends DataSet<S>, S>{
    protected String fileName;

    public Reader() {};

    protected Reader(String fileName) {
        this.fileName = fileName;
    }

    public abstract S[] read() throws IOException;
}
