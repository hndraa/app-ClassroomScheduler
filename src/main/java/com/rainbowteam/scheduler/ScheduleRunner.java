package com.rainbowteam.scheduler;

import com.rainbowteam.scheduler.io.CsvService;
import com.rainbowteam.scheduler.model.*;
import com.rainbowteam.scheduler.service.ConflictDetector;
import com.rainbowteam.scheduler.service.ScheduleRepository;
import com.rainbowteam.scheduler.service.SchedulerService;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Shared scheduling flow: both modes read and update one persistent schedule. */
public final class ScheduleRunner {
    private ScheduleRunner() {
    }

    public static void run(String mode, List<Activity> activities, List<Room> rooms,
                           List<TimeSlot> timeSlots, String outputFile) {
        ScheduleRepository repository = new ScheduleRepository();
        System.out.println("\n--- Mode " + mode + " ---");
        List<ScheduleEntry> existing;
        try {
            existing = repository.findAll();
        } catch (IOException ex) {
            System.out.println("Gagal membaca jadwal bersama: " + ex.getMessage());
            return;
        }
        System.out.println("Jadwal yang sudah tersimpan: " + existing.size() + " aktivitas.");

        try {
            List<Room> allRooms = mergeRooms(existing, rooms);

            // Slot yang dimasukkan pada mode ini adalah pilihan waktu untuk aktivitas BARU.
            // Jangan gabungkan slot jadwal lama, karena solver dapat memindahkan aktivitas
            // baru ke hari/jam lama (misalnya Rabu menjadi Senin).
            Optional<List<ScheduleEntry>> result = new SchedulerService()
                    .createSchedule(activities, allRooms, timeSlots, existing);

            if (result.isEmpty()) {
                System.out.println("Jadwal baru tidak dapat ditambahkan tanpa melanggar jadwal yang sudah ada.");
                System.out.println("Jadwal lama tidak diubah.");
                return;
            }

            List<ScheduleEntry> fullSchedule = result.orElseThrow();
            repository.saveAll(fullSchedule);
            CsvService.exportSchedule(fullSchedule, Path.of(outputFile));
            System.out.println("Jadwal berhasil ditambahkan. Total tersimpan: " + fullSchedule.size() + " aktivitas.");
            System.out.println("Jadwal bersama: " + repository.path().toAbsolutePath());
            System.out.println("Salinan ekspor: " + Path.of(outputFile).toAbsolutePath());
            printSchedule(fullSchedule);
            List<String> conflicts = new ConflictDetector().findConflicts(fullSchedule);
            if (conflicts.isEmpty()) {
                System.out.println("Validasi: LULUS — tidak ada konflik ruang, waktu, dosen, kelompok, atau kapasitas.");
            } else {
                System.out.println("PERINGATAN: konflik terdeteksi:");
                conflicts.forEach(message -> System.out.println("- " + message));
            }
        } catch (IllegalArgumentException ex) {
            System.out.println("Jadwal tidak dapat ditambahkan: " + ex.getMessage());
            System.out.println("Jadwal yang sudah tersimpan tetap dipertahankan.");
        } catch (IOException ex) {
            System.out.println("Gagal menyimpan jadwal: " + ex.getMessage());
        }
    }

    private static List<Room> mergeRooms(List<ScheduleEntry> existing, List<Room> incoming) {
        // Hanya ruang yang dipilih pada input dinamis boleh dipakai untuk aktivitas baru.
        // Jika nama ruang sudah ada pada jadwal lama, gunakan ID ruang lama agar
        // pemeriksaan bentrok mengenali bahwa itu ruang fisik yang sama.
        Map<String, Room> existingByName = new LinkedHashMap<>();
        for (ScheduleEntry entry : existing) {
            existingByName.putIfAbsent(entry.room().name().trim().toLowerCase(Locale.ROOT), entry.room());
        }

        List<Room> selected = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (Room room : incoming) {
            String nameKey = room.name().trim().toLowerCase(Locale.ROOT);
            if (!seenNames.add(nameKey)) {
                throw new IllegalArgumentException("Ruang dipilih lebih dari sekali: " + room.name());
            }
            Room oldRoom = existingByName.get(nameKey);
            if (oldRoom != null) {
                selected.add(new Room(oldRoom.id(), room.name(), room.capacity()));
            } else {
                selected.add(room);
            }
        }
        return List.copyOf(selected);
    }

    private static List<TimeSlot> mergeSlots(List<ScheduleEntry> existing, List<TimeSlot> incoming) {
        Map<String, TimeSlot> merged = new LinkedHashMap<>();
        for (ScheduleEntry entry : existing) putSlot(merged, entry.timeSlot());
        for (TimeSlot slot : incoming) putSlot(merged, slot);
        return List.copyOf(merged.values());
    }

    private static void putSlot(Map<String, TimeSlot> slots, TimeSlot slot) {
        String key = slot.id().trim().toLowerCase(Locale.ROOT);
        TimeSlot previous = slots.putIfAbsent(key, slot);
        if (previous != null && (!previous.day().equalsIgnoreCase(slot.day())
                || !previous.start().equals(slot.start()) || !previous.end().equals(slot.end()))) {
            throw new IllegalArgumentException("ID slot " + slot.id()
                    + " sudah terdaftar dengan hari/jam berbeda.");
        }
    }

    /** Displays a timetable matrix with one group of columns per room. */
    public static void showStoredSchedule(Scanner scanner) {
        ScheduleRepository repository = new ScheduleRepository();
        try {
            List<ScheduleEntry> schedule = repository.findAll();
            System.out.println("\n=== CEK JADWAL TERSIMPAN ===");
            System.out.println("Sumber data: " + repository.path().toAbsolutePath());
            if (schedule.isEmpty()) {
                System.out.println("Belum ada jadwal yang tersimpan.");
                return;
            }

            List<String> days = orderedDays(schedule);
            System.out.println("Total aktivitas: " + schedule.size());
            System.out.println("Pilih tampilan:");
            System.out.println("1. Semua hari");
            for (int i = 0; i < days.size(); i++) {
                System.out.printf("%d. %s%n", i + 2, days.get(i));
            }
            System.out.println("0. Kembali ke menu utama");
            System.out.print("Pilihan: ");
            String choice = scanner.nextLine().trim();

            if (choice.equals("0")) return;
            if (choice.equals("1")) {
                for (String day : days) printDayGrid(day, schedule, allRooms(schedule));
            } else {
                try {
                    int selected = Integer.parseInt(choice) - 2;
                    if (selected < 0 || selected >= days.size()) {
                        System.out.println("Pilihan hari tidak tersedia.");
                        return;
                    }
                    printDayGrid(days.get(selected), schedule, allRooms(schedule));
                } catch (NumberFormatException ex) {
                    System.out.println("Pilihan tidak valid.");
                    return;
                }
            }

            List<String> conflicts = new ConflictDetector().findConflicts(schedule);
            if (conflicts.isEmpty()) {
                System.out.println("\nStatus validasi: LULUS — tidak ada konflik.");
            } else {
                System.out.println("\nStatus validasi: ADA KONFLIK");
                conflicts.forEach(message -> System.out.println("- " + message));
            }
        } catch (IOException ex) {
            System.out.println("Gagal membaca jadwal: " + ex.getMessage());
        }
    }

    private static List<String> orderedDays(List<ScheduleEntry> schedule) {
        List<String> days = new ArrayList<>();
        for (ScheduleEntry entry : schedule) {
            String day = entry.timeSlot().day();
            if (days.stream().noneMatch(existing -> existing.equalsIgnoreCase(day))) days.add(day);
        }
        List<String> weekdayOrder = List.of("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Jum'at",
                "Sabtu", "Minggu", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        days.sort(Comparator.comparingInt((String day) -> {
            for (int i = 0; i < weekdayOrder.size(); i++) {
                if (weekdayOrder.get(i).equalsIgnoreCase(day)) return i;
            }
            return 100;
        }).thenComparing(String::compareToIgnoreCase));
        return days;
    }

    private static List<Room> allRooms(List<ScheduleEntry> schedule) {
        return schedule.stream()
                .map(ScheduleEntry::room)
                .collect(java.util.stream.Collectors.toMap(
                        room -> room.id().toLowerCase(Locale.ROOT), room -> room, (first, second) -> first,
                        TreeMap::new))
                .values().stream()
                .sorted(Comparator.comparing(Room::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Compact timetable view: one row per scheduled activity instead of
     * repeating three columns for every room. This stays readable in a terminal.
     */
    private static void printDayGrid(String day, List<ScheduleEntry> schedule, List<Room> rooms) {
        List<ScheduleEntry> dayEntries = schedule.stream()
                .filter(entry -> entry.timeSlot().day().equalsIgnoreCase(day))
                .sorted(Comparator.comparing((ScheduleEntry e) -> e.room().name(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(e -> e.timeSlot().start())
                        .thenComparing(e -> e.timeSlot().end())
                        .thenComparing(e -> e.activity().courseName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (dayEntries.isEmpty()) return;

        System.out.println("\n" + "=".repeat(100));
        System.out.println("JADWAL HARI " + day.toUpperCase(Locale.ROOT));
        System.out.println("Satu baris menunjukkan satu aktivitas; '-' berarti tidak ada data.");
        System.out.println("=".repeat(100));
        System.out.printf("%-13s | %-14s | %-36s | %-10s | %-20s%n",
                "WAKTU", "RUANG", "MATA KULIAH", "KELAS", "DOSEN");
        System.out.println("-".repeat(100));

        String previousTime = "";
        for (ScheduleEntry entry : dayEntries) {
            String time = entry.timeSlot().start() + "-" + entry.timeSlot().end();
            // Repeating time makes it easy to compare which rooms are occupied simultaneously.
            System.out.printf("%-13s | %-14s | %-36s | %-10s | %-20s%n",
                    time, entry.room().name() + " (" + entry.room().capacity() + ")",
                    entry.activity().courseName(), entry.activity().studentGroup(),
                    entry.activity().instructor());
            previousTime = time;
        }
        System.out.println("-".repeat(100));
        System.out.printf("Total aktivitas hari %s: %d%n", day, dayEntries.size());
    }

    private static void printSchedule(List<ScheduleEntry> schedule) {
        System.out.println("\nJadwal:");
        System.out.printf("%-8s | %-25s | %-18s | %-14s | %-12s | %-8s | %s%n",
                "ID", "Mata Kuliah", "Dosen", "Kelas", "Ruang", "Kapasitas", "Waktu");
        System.out.println("-".repeat(125));
        for (ScheduleEntry entry : schedule) {
            System.out.printf("%-8s | %-25s | %-18s | %-14s | %-12s | %-8d | %s%n",
                    shorten(entry.activity().id(), 8), shorten(entry.activity().courseName(), 25),
                    shorten(entry.activity().instructor(), 18), shorten(entry.activity().studentGroup(), 14),
                    shorten(entry.room().name(), 12), entry.room().capacity(), entry.timeSlot().displayTime());
        }
    }

    private static String shorten(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }
}
