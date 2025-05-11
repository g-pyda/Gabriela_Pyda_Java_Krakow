package dataHandler.dataReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import dataHandler.dataSet.Orders;
import dataHandler.dataType.Order;

import java.io.File;
import java.io.IOException;

public class OrderReader extends Reader<Orders, Order> {
    public OrderReader(String fileName) {
        super(fileName);
    }

    public Order[] read(){
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(super.fileName), Order[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
