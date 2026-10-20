package com.rainbowteam.scheduler.service;

import com.rainbowteam.scheduler.io.CsvService;
import com.rainbowteam.scheduler.model.ScheduleEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** Persistent shared schedule used by both static and dynamic modes. */
public final class ScheduleRepository {
    public static final Path DEFAULT_PATH = Path.of("data", "schedule.csv");
    private final Path path;

    public ScheduleRepository() {
        this(DEFAULT_PATH);
    }

    public ScheduleRepository(Path path) {
        this.path = path;
    }

    public List<ScheduleEntry> findAll() throws IOException {
        return CsvService.importSchedule(path);
    }

    public void saveAll(List<ScheduleEntry> schedule) throws IOException {
        CsvService.exportSchedule(schedule, path);
    }

    public Path path() {
        return path;
    }
}
