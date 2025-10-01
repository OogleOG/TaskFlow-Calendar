package org.oogle.calender;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public class EventStorage {
    private static final Path FILE = Paths.get("events.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static List<Event> loadEvents() {
        if (!Files.exists(FILE)) return new ArrayList<>();
        try (Reader reader = Files.newBufferedReader(FILE)) {
            Type listType = new TypeToken<List<Event>>() {}.getType();
            return gson.fromJson(reader, listType);
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveEvents(List<Event> events) {
        try (Writer writer = Files.newBufferedWriter(FILE)) {
            gson.toJson(events, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

