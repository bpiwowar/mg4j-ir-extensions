package net.bpiwowar.mg4j.extensions.tasks;

import bpiwowar.argparser.ArgParser;
import bpiwowar.argparser.StringScanException;
import bpiwowar.argparser.StringScanner;
import bpiwowar.argparser.handlers.GenericObjectsHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringReader;
import java.lang.reflect.Field;

/**
 * Created by bpiwowar on 1/10/14.
 */
public class JsonChooser extends GenericObjectsHandler {
    Class<?> objectClass;

    public JsonChooser(Object object, Field field, Class<?> aClass) {
        super(object, field, aClass);
        this.objectClass = aClass;
    }

    @Override
    protected Object process(ArgParser argParser, StringScanner scanner)
            throws StringScanException {
        String name = scanner.getString();

        final Gson gson = new GsonBuilder()
                .create();

        JsonParser parser = new JsonParser();
        File file = new File(name);
        final JsonElement jsonElement;

        if (file.isFile()) {
            try {
                jsonElement = parser.parse(new FileReader(file));
            } catch (FileNotFoundException e) {
                throw new StringScanException(e);
            }
        } else {
            jsonElement = parser.parse(new StringReader(name));
        }

        if (jsonElement.isJsonObject()) {

        } else if (jsonElement.isJsonPrimitive()) {
            jsonElement.getAsString();
        }


        addValue(object);
        return object;


    }
}
