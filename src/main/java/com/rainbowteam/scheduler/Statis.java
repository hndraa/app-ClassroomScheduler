package com.rainbowteam.scheduler;

import com.rainbowteam.scheduler.model.Activity;
import com.rainbowteam.scheduler.model.Room;
import com.rainbowteam.scheduler.model.TimeSlot;

import java.time.LocalTime;
import java.util.List;

/** Runs the scheduler using a fixed sample dataset, similar to the old Statis.java. */
public final class Statis {
    private Statis() {
    }

    public static void main(String[] args) {
        run();
    }

    public static void run() {
        List<Room> rooms = List.of(
                new Room("A301", "A3.01", 30),
                new Room("A302", "A3.02", 30),
                new Room("D201", "D2.01", 40),
                new Room("D202", "D2.02", 40),
                new Room("D303", "D3.03", 40),
                new Room("D304", "D3.04", 40),
                new Room("D401", "D4.01", 40),
                new Room("D402", "D4.02", 40),
                new Room("D403", "D4.03", 40),
                new Room("D404", "D4.04", 40)
        );

        List<TimeSlot> timeSlots = List.of(
                new TimeSlot("MON-1", "Senin", LocalTime.of(8, 0), LocalTime.of(10, 0)),
                new TimeSlot("MON-2", "Senin", LocalTime.of(10, 15), LocalTime.of(12, 15)),
                new TimeSlot("MON-3", "Senin", LocalTime.of(13, 0), LocalTime.of(15, 0))
        );

        List<Activity> activities = List.of(
                new Activity("IF101", "Algoritma", "Dr. Andi", "TI-A", 35),
                new Activity("IF102", "Struktur Data", "Dr. Andi", "TI-B", 28),
                new Activity("IF103", "Basis Data", "Dr. Budi", "TI-A", 35),
                new Activity("IF104", "Jaringan Komputer", "Dr. Citra", "TI-C", 25),
                new Activity("IF105", "Pemrograman Berorientasi Objek", "Dr. Dedi", "TI-B", 30),
                new Activity("IF106", "Pengolahan Citra Digital", "Dr. Budi", "TI-D", 38)
        );

        ScheduleRunner.run("STATIS", activities, rooms, timeSlots, "output/schedule-statis.csv");
    }
}
