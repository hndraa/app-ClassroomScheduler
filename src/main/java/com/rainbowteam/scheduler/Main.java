package com.rainbowteam.scheduler;

import java.util.Scanner;

/** Main menu for static input, dynamic input, and timetable-style schedule viewing. */
public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("==============================================");
                System.out.println(" RAINBOW TEAM - CLASSROOM SCHEDULER");
                System.out.println("==============================================");
                System.out.println("1. Statis (data contoh sudah tersedia)");
                System.out.println("2. Dinamis (masukkan data sendiri)");
                System.out.println("3. Cek jadwal (tampilan tabel hari/ruang)");
                System.out.println("0. Keluar");
                System.out.print("Pilih menu: ");
                String choice = scanner.nextLine().trim();

                switch (choice) {
                    case "1" -> Statis.run();
                    case "2" -> Dinamis.run(scanner);
                    case "3" -> ScheduleRunner.showStoredSchedule(scanner);
                    case "0" -> {
                        System.out.println("Program selesai.");
                        return;
                    }
                    default -> System.out.println("Pilihan tidak valid. Masukkan 1, 2, 3, atau 0.");
                }
                System.out.println();
            }
        }
    }
}
