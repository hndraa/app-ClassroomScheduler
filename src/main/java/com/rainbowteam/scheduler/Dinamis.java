package com.rainbowteam.scheduler;

import com.rainbowteam.scheduler.io.CsvService;
import com.rainbowteam.scheduler.model.Activity;
import com.rainbowteam.scheduler.model.Room;
import com.rainbowteam.scheduler.model.ScheduleEntry;
import com.rainbowteam.scheduler.model.TimeSlot;
import com.rainbowteam.scheduler.service.ConflictDetector;
import com.rainbowteam.scheduler.service.ScheduleRepository;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Input dinamis bertingkat:
 * pilih hari -> pilih ruang -> tentukan slot dan mata kuliah untuk tiap ruang.
 * Jadwal disimpan persis sesuai input, bukan dijadwalkan ulang oleh solver.
 */
public final class Dinamis {
    private static final String[] DAYS = {"Senin", "Selasa", "Rabu", "Kamis", "Jumat"};
    private static final RoomOption[] ROOM_OPTIONS = {
            new RoomOption("A3.01", 30), new RoomOption("A3.02", 30),
            new RoomOption("D2.01", 40), new RoomOption("D2.02", 40),
            new RoomOption("D3.03", 40), new RoomOption("D3.04", 40),
            new RoomOption("D4.01", 40), new RoomOption("D4.02", 40),
            new RoomOption("D4.03", 40), new RoomOption("D4.04", 40)
    };
    private static final Pattern CLASS_PATTERN = Pattern.compile("TI-[A-Z]");
    private Dinamis() {}

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            run(scanner);
        }
    }

    public static void run(Scanner scanner) {
        System.out.println("\n=== INPUT JADWAL DINAMIS ===");
        String day = chooseDay(scanner);
        List<Room> selectedRooms = chooseRooms(scanner);
        List<ScheduleEntry> newEntries = new ArrayList<>();
        Set<String> activityIds = new HashSet<>();
        Set<String> existingActivityIds = loadExistingActivityIds();

        for (Room room : selectedRooms) {
            System.out.println("\n----------------------------------------");
            System.out.println("RUANG " + room.name() + " | Kapasitas " + room.capacity());
            System.out.println("----------------------------------------");
            int slotCount = readIntInRange(scanner, "Jumlah slot/mata kuliah di ruang ini: ", 1, 30);

            for (int i = 1; i <= slotCount; i++) {
                System.out.println("\n  Slot ke-" + i + " di " + room.name());
                LocalTime start = readTime(scanner, "  Jam mulai (HH:mm): ");
                LocalTime end;
                while (true) {
                    end = readTime(scanner, "  Jam selesai (HH:mm): ");
                    if (start.isBefore(end)) break;
                    System.out.println("  Jam selesai harus setelah jam mulai.");
                }

                System.out.println("  Data mata kuliah:");
                String id = generateActivityId(existingActivityIds, activityIds);
                activityIds.add(id.toUpperCase(Locale.ROOT));
                System.out.println("    ID aktivitas otomatis: " + id);
                String course = readNonBlank(scanner, "    Nama mata kuliah: ");
                String instructor = readNonBlank(scanner, "    Nama dosen: ");
                String studentClass = readClass(scanner);
                int students;
                while (true) {
                    students = readIntInRange(scanner, "    Jumlah mahasiswa (maksimal "
                            + room.capacity() + "): ", 1, 1000);
                    if (students <= room.capacity()) {
                        break;
                    }
                    System.out.printf("    PERINGATAN: Kapasitas ruang %s hanya %d mahasiswa. "
                                    + "Jumlah yang dimasukkan %d. Silakan masukkan ulang jumlah mahasiswa "
                                    + "maksimal %d.%n",
                            room.name(), room.capacity(), students, room.capacity());
                }

                String slotId = room.name().replace(".", "") + "-" + day.substring(0, 3).toUpperCase(Locale.ROOT)
                        + "-" + i;
                TimeSlot slot = new TimeSlot(slotId, day, start, end);
                Activity activity = new Activity(id, course, instructor, studentClass, students);
                newEntries.add(new ScheduleEntry(activity, room, slot));
            }
        }

        saveIfValid(newEntries);
    }

    private static String chooseDay(Scanner scanner) {
        System.out.println("\nPilih hari penjadwalan:");
        for (int i = 0; i < DAYS.length; i++) {
            System.out.printf("%d. %s%n", i + 1, DAYS[i]);
        }
        return DAYS[readIntInRange(scanner, "Nomor hari: ", 1, DAYS.length) - 1];
    }

    private static List<Room> chooseRooms(Scanner scanner) {
        System.out.println("\nDaftar ruang yang tersedia:");
        for (int i = 0; i < ROOM_OPTIONS.length; i++) {
            RoomOption room = ROOM_OPTIONS[i];
            System.out.printf("%2d. %-7s | Kapasitas %d mahasiswa%n",
                    i + 1, room.name(), room.capacity());
        }

        int count = readIntInRange(scanner, "Berapa ruang yang ingin diisi pada hari ini (pilihan 1-" + ROOM_OPTIONS.length + "): ", 1, ROOM_OPTIONS.length);
        List<Room> selected = new ArrayList<>();
        Set<String> selectedNames = new HashSet<>();

        while (selected.size() < count) {
            int number = readIntInRange(scanner,
                    "Pilih nomor ruang ke-" + (selected.size() + 1) + ": ", 1, ROOM_OPTIONS.length);
            RoomOption option = ROOM_OPTIONS[number - 1];
            if (!selectedNames.add(option.name().toLowerCase(Locale.ROOT))) {
                System.out.println("Ruang sudah dipilih. Silakan pilih ruang lain.");
                continue;
            }
            String id = option.name().replace(".", "").toUpperCase(Locale.ROOT);
            selected.add(new Room(id, option.name(), option.capacity()));
        }
        return List.copyOf(selected);
    }

    private static String readClass(Scanner scanner) {
        while (true) {
            String value = readNonBlank(scanner, "    Kelas (format TI-A sampai TI-Z): ").toUpperCase(Locale.ROOT);
            if (CLASS_PATTERN.matcher(value).matches()) return value;
            System.out.println("    Format kelas tidak valid. Gunakan TI-A, TI-B, ... TI-Z.");
        }
    }

    /**
     * Mengambil ID aktivitas yang sudah tersimpan agar nomor otomatis tidak terpakai ulang.
     */
    private static Set<String> loadExistingActivityIds() {
        Set<String> ids = new HashSet<>();
        try {
            for (ScheduleEntry entry : new ScheduleRepository().findAll()) {
                ids.add(entry.activity().id().toUpperCase(Locale.ROOT));
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("Tidak dapat membaca ID aktivitas yang tersimpan: "
                    + ex.getMessage(), ex);
        }
        return ids;
    }

    /**
     * Membuat ID otomatis berurutan, misalnya MK001, MK002, dan seterusnya.
     * Jika ID tersebut sudah ada, program mencari nomor berikutnya yang masih kosong.
     */
    private static String generateActivityId(Set<String> existingIds, Set<String> currentIds) {
        int number = 1;
        while (true) {
            String candidate = String.format(Locale.ROOT, "MK%03d", number);
            String key = candidate.toUpperCase(Locale.ROOT);
            if (!existingIds.contains(key) && !currentIds.contains(key)) {
                return candidate;
            }
            number++;
        }
    }

    private static void saveIfValid(List<ScheduleEntry> newEntries) {
        ScheduleRepository repository = new ScheduleRepository();
        try {
            List<ScheduleEntry> existing = repository.findAll();
            Set<String> existingIds = new HashSet<>();
            for (ScheduleEntry entry : existing) {
                existingIds.add(entry.activity().id().toLowerCase(Locale.ROOT));
            }
            for (ScheduleEntry entry : newEntries) {
                if (!existingIds.add(entry.activity().id().toLowerCase(Locale.ROOT))) {
                    System.out.println("Gagal: ID aktivitas '" + entry.activity().id()
                            + "' sudah digunakan pada jadwal tersimpan.");
                    System.out.println("Jadwal lama tidak diubah.");
                    return;
                }
            }

            List<ScheduleEntry> combined = new ArrayList<>(existing);
            combined.addAll(newEntries);
            List<String> conflicts = new ConflictDetector().findConflicts(combined);
            if (!conflicts.isEmpty()) {
                System.out.println("\nJadwal tidak disimpan karena ditemukan konflik:");
                conflicts.forEach(message -> System.out.println("- " + message));
                System.out.println("Jadwal lama tetap dipertahankan.");
                return;
            }

            repository.saveAll(combined);
            CsvService.exportSchedule(combined, java.nio.file.Path.of("output", "schedule-dinamis.csv"));
            System.out.println("\nJadwal hari " + newEntries.getFirst().timeSlot().day()
                    + " berhasil disimpan.");
            System.out.println("Total jadwal tersimpan: " + combined.size() + " aktivitas.");
            System.out.println("File jadwal bersama: " + repository.path().toAbsolutePath());
            System.out.println("\nRingkasan jadwal yang baru dimasukkan (urut ruang lalu jam):");
            List<ScheduleEntry> orderedNewEntries = newEntries.stream()
                    .sorted(java.util.Comparator
                            .comparing((ScheduleEntry e) -> e.room().name(), String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(e -> e.timeSlot().start())
                            .thenComparing(e -> e.timeSlot().end()))
                    .toList();
            for (ScheduleEntry entry : orderedNewEntries) {
                System.out.printf("%-13s | %-7s | %-28s | %-5s | %s%n",
                        entry.timeSlot().start() + "-" + entry.timeSlot().end(),
                        entry.room().name(), entry.activity().courseName(),
                        entry.activity().studentGroup(), entry.activity().instructor());
            }
        } catch (IOException ex) {
            System.out.println("Gagal membaca/menyimpan jadwal: " + ex.getMessage());
        }
    }

    private static int readIntInRange(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) {
                // Tampilkan pesan validasi di bawah.
            }
            System.out.printf("Masukkan angka antara %d dan %d.%n", min, max);
        }
    }

    private static String readNonBlank(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            if (!value.isBlank()) return value;
            System.out.println("Input tidak boleh kosong.");
        }
    }

    private static LocalTime readTime(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = scanner.nextLine().trim();
            try {
                return LocalTime.parse(value);
            } catch (DateTimeParseException ex) {
                System.out.println("Format waktu salah. Gunakan HH:mm, contoh 08:00.");
            }
        }
    }

    private record RoomOption(String name, int capacity) {}
}
