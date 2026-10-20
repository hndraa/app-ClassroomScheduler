package com.rainbowteam.scheduler.io;

import com.rainbowteam.scheduler.model.Activity;
import com.rainbowteam.scheduler.model.Room;
import com.rainbowteam.scheduler.model.ScheduleEntry;
import com.rainbowteam.scheduler.model.TimeSlot;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/** Reads and writes the shared schedule as a CSV file. */
public final class CsvService {
    private CsvService() {
    }

    public static void exportSchedule(List<ScheduleEntry> schedule, Path output) throws IOException {
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);

        StringBuilder csv = new StringBuilder(
                "activity_id,course,instructor,student_group,required_capacity,room_id,room,room_capacity,time_slot_id,day,start,end\n");
        for (ScheduleEntry entry : schedule) {
            csv.append(cell(entry.activity().id())).append(',')
                    .append(cell(entry.activity().courseName())).append(',')
                    .append(cell(entry.activity().instructor())).append(',')
                    .append(cell(entry.activity().studentGroup())).append(',')
                    .append(entry.activity().requiredCapacity()).append(',')
                    .append(cell(entry.room().id())).append(',')
                    .append(cell(entry.room().name())).append(',')
                    .append(entry.room().capacity()).append(',')
                    .append(cell(entry.timeSlot().id())).append(',')
                    .append(cell(entry.timeSlot().day())).append(',')
                    .append(entry.timeSlot().start()).append(',')
                    .append(entry.timeSlot().end()).append('\n');
        }
        Files.writeString(output, csv.toString(), StandardCharsets.UTF_8);
    }

    public static List<ScheduleEntry> importSchedule(Path input) throws IOException {
        if (!Files.exists(input)) return List.of();
        List<String> lines = Files.readAllLines(input, StandardCharsets.UTF_8);
        if (lines.isEmpty()) return List.of();
        List<ScheduleEntry> result = new ArrayList<>();
        for (int lineNumber = 1; lineNumber < lines.size(); lineNumber++) {
            String line = lines.get(lineNumber);
            if (line.isBlank()) continue;
            List<String> fields = parseCsvLine(line);
            if (fields.size() != 12) {
                throw new IOException("Format CSV tidak valid pada baris " + (lineNumber + 1)
                        + ". Diperlukan 12 kolom; silakan periksa " + input + ".");
            }
            try {
                Activity activity = new Activity(fields.get(0), fields.get(1), fields.get(2),
                        fields.get(3), Integer.parseInt(fields.get(4)));
                Room room = new Room(fields.get(5), fields.get(6), Integer.parseInt(fields.get(7)));
                TimeSlot slot = new TimeSlot(fields.get(8), fields.get(9),
                        LocalTime.parse(fields.get(10)), LocalTime.parse(fields.get(11)));
                result.add(new ScheduleEntry(activity, room, slot));
            } catch (RuntimeException ex) {
                throw new IOException("Data CSV tidak valid pada baris " + (lineNumber + 1)
                        + ": " + ex.getMessage(), ex);
            }
        }
        return List.copyOf(result);
    }

    private static String cell(String value) {
        String escaped = value.replace("\"", "\"\"");
        return escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")
                || escaped.contains("\r") ? "\"" + escaped + "\"" : escaped;
    }

    /** Handles commas and escaped double quotes in quoted CSV fields. */
    private static List<String> parseCsvLine(String line) throws IOException {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == ',' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(ch);
            }
        }
        if (quoted) throw new IOException("Tanda kutip CSV tidak berpasangan.");
        fields.add(field.toString());
        return fields;
    }
}
