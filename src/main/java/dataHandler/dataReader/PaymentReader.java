package dataHandler.dataReader;

import com.fasterxml.jackson.databind.ObjectMapper;
import dataHandler.dataSet.PaymentMethods;
import dataHandler.dataType.PayMeth;

import java.io.File;
import java.io.IOException;

public class PaymentReader extends Reader<PaymentMethods, PayMeth> {
    public PaymentReader(String fileName) {
        super(fileName);
    }

    public PayMeth[] read(){
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(super.fileName), PayMeth[].class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
