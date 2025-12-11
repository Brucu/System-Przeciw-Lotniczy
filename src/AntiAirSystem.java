import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner; // Import potrzebny do obsługi klawiatury

/**
 * Główna klasa symulatora Systemu Przeciwlotniczego (Wersja Finalna + Menu).
 * Projekt zawiera:
 * - Zaawansowaną symulację wektorową 3D
 * - Logikę paliwa i czyszczenia pamięci
 */
public class AntiAirSystem {

    // --- STAŁE KONFIGURACYJNE ---
    private static final double RADAR_RANGE = 4000.0;
    private static final double MAX_WORLD_RANGE = 9000.0;
    private static final double MISSILE_SPEED = 550.0;
    private static final double EXPLOSION_RADIUS = 100.0;
    private static final int MISSILE_FUEL = 25;
    private static final int SIMULATION_DELAY = 300;

    public static void main(String[] args) {
        // Wyświetlenie menu i pobranie decyzji użytkownika
        boolean startSimulation = showMenu();

        if (startSimulation) {
            runSimulation();
        } else {
            System.out.println("Zamykanie systemu...");
        }
    }

    /**
     * Metoda odpowiedzialna za wyświetlenie menu i interakcję z użytkownikiem.
     */
    private static boolean showMenu() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("\n================================================");
        System.out.println("   SYSTEM OBRONY PRZECIWLOTNICZEJ 'AEGIS-PL'    ");
        System.out.println("             Wersja v1.0 (Student)              ");
        System.out.println("================================================");
        System.out.println(" AUTOR PROJEKTU:");
        System.out.println(" Imie i Nazwisko: [Jakub Bruc]      ");
        System.out.println(" Nr indeksu:      [127606]           ");
        System.out.println("================================================");
        System.out.println(" MENU GŁÓWNE:");
        System.out.println(" [1] Uruchom symulację bojową");
        System.out.println(" [2] Wyjdź z programu");
        System.out.println("================================================");
        System.out.print(" Twój wybór > ");

        String choice = scanner.nextLine();

        // Prosta walidacja wyboru
        if (choice.equals("1")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Główna logika symulacji, oddzielona od menu dla czytelności.
     */
    private static void runSimulation() {
        System.out.println("\n>> INICJALIZACJA SYSTEMÓW BOJOWYCH...");

        CommandCenter commandCenter = new CommandCenter();

        // SCENARIUSZE (możesz tu dodawać więcej celów)
        // 1. Dron blisko bazy
        commandCenter.spawnEnemy(new Vector3(2000, 2000, 1000), new Vector3(-50, -50, -20), TargetType.DRON_KAMIKAZE);
        // 2. Myśliwiec dalekiego zasięgu
        commandCenter.spawnEnemy(new Vector3(-4000, 3000, 2000), new Vector3(100, -20, 10), TargetType.MYSLIWIEC);
        // 3. Rakieta manewrująca (szybka)
        commandCenter.spawnEnemy(new Vector3(1000, 5000, 5000), new Vector3(0, -200, -100), TargetType.RAKIETA);

        int step = 1;

        // PĘTLA CZASU RZECZYWISTEGO
        while (commandCenter.hasActiveEntities()) {
            System.out.println("\n--- TURA SYMULACJI " + step + " ---");

            commandCenter.updateSystem();
            step++;

            try {
                Thread.sleep(SIMULATION_DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n================================================");
        System.out.println(" RAPORT KOŃCOWY: Wszystkie cele zneutralizowane.");
        System.out.println(" Symulacja zakończona powodzeniem.");
        System.out.println("================================================");
    }

    // ==========================================
    // KLASY POMOCNICZE (MATEMATYKA WEKTOROWA)
    // ==========================================

    static class Vector3 {
        double x, y, z;

        public Vector3(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }

        public double distanceTo(Vector3 other) {
            return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2) + Math.pow(this.z - other.z, 2));
        }

        public void add(Vector3 other) {
            this.x += other.x; this.y += other.y; this.z += other.z;
        }

        public Vector3 normalize() {
            double length = Math.sqrt(x*x + y*y + z*z);
            if (length == 0) return new Vector3(0,0,0);
            return new Vector3(x/length, y/length, z/length);
        }

        public Vector3 multiply(double scalar) {
            return new Vector3(x * scalar, y * scalar, z * scalar);
        }

        @Override
        public String toString() {
            return String.format("[x:%d y:%d z:%d]", (int)x, (int)y, (int)z);
        }
    }

    enum TargetType { MYSLIWIEC, DRON_KAMIKAZE, RAKIETA }

    // ==========================================
    // KLASY OBIEKTÓW LATAJĄCYCH
    // ==========================================

    abstract static class AerialEntity {
        protected Vector3 position;
        protected Vector3 velocity;
        protected boolean isActive = true;

        public AerialEntity(Vector3 position, Vector3 velocity) {
            this.position = position;
            this.velocity = velocity;
        }

        public void updatePhysics() {
            if (isActive) position.add(velocity);
        }

        public Vector3 getPosition() { return position; }
        public boolean isActive() { return isActive; }
        public void destroy() { isActive = false; }
    }

    static class EnemyTarget extends AerialEntity {
        private final int id;
        private final TargetType type;

        public EnemyTarget(int id, Vector3 position, Vector3 velocity, TargetType type) {
            super(position, velocity);
            this.id = id; this.type = type;
        }

        @Override
        public String toString() {
            return "WRÓG #" + id + " (" + type + ") " + position;
        }
    }

    static class Interceptor extends AerialEntity {
        private final EnemyTarget target;
        private int fuel;

        public Interceptor(Vector3 position, EnemyTarget target) {
            super(position, new Vector3(0,0,0));
            this.target = target;
            this.fuel = MISSILE_FUEL;
        }

        public void updateGuidance() {
            if (!isActive) return;

            // Utrata celu
            if (!target.isActive()) {
                System.out.println(">> [AUTO-DESTRUCT] Cel zniknął. Rakieta ulega samozniszczeniu.");
                this.destroy();
                return;
            }

            // Brak paliwa
            fuel--;
            if (fuel <= 0) {
                System.out.println(">> [FAIL] Wyczerpano paliwo. Rakieta spada.");
                this.destroy();
                return;
            }

            // Obliczanie trajektorii pościgu
            Vector3 direction = new Vector3(
                    target.getPosition().x - this.position.x,
                    target.getPosition().y - this.position.y,
                    target.getPosition().z - this.position.z
            );
            this.velocity = direction.normalize().multiply(MISSILE_SPEED);

            // Detekcja trafienia
            if (this.position.distanceTo(target.getPosition()) < EXPLOSION_RADIUS) {
                System.out.println(">> [BOOM] TRAFIENIE! Zniszczono: " + target);
                target.destroy();
                this.destroy();
            } else {
                System.out.println(" -> Rakieta goni cel " + target.id + " (Paliwo: " + fuel + ")");
            }
        }
    }

    // ==========================================
    // CENTRUM DOWODZENIA
    // ==========================================

    static class CommandCenter {
        private List<EnemyTarget> enemies = new ArrayList<>();
        private List<Interceptor> missiles = new ArrayList<>();
        private int enemyIdCounter = 1;

        public void spawnEnemy(Vector3 pos, Vector3 vel, TargetType type) {
            enemies.add(new EnemyTarget(enemyIdCounter++, pos, vel, type));
        }

        public boolean hasActiveEntities() {
            return !enemies.isEmpty() || !missiles.isEmpty();
        }

        public void updateSystem() {
            // 1. Ruch wrogów i sprawdzanie granic świata
            for (EnemyTarget enemy : enemies) {
                if (enemy.isActive()) {
                    enemy.updatePhysics();
                    if (enemy.getPosition().distanceTo(new Vector3(0,0,0)) > MAX_WORLD_RANGE) {
                        System.out.println("[RADAR] Cel #" + enemy.id + " opuścił strefę działań.");
                        enemy.destroy();
                    }
                }
            }

            // 2. Radar i decyzja o ogniu
            scanAndEngage();

            // 3. Ruch rakiet
            for (Interceptor missile : missiles) {
                if (missile.isActive()) {
                    missile.updateGuidance();
                    missile.updatePhysics();
                }
            }

            // 4. Usuwanie martwych obiektów (Garbage Collection symulacji)
            enemies.removeIf(e -> !e.isActive());
            missiles.removeIf(m -> !m.isActive());
        }

        private void scanAndEngage() {
            for (EnemyTarget enemy : enemies) {
                if (!enemy.isActive()) continue;

                double dist = enemy.getPosition().distanceTo(new Vector3(0,0,0));
                if (dist <= RADAR_RANGE) {
                    if (!isTargetEngaged(enemy)) {
                        System.out.println("[WYRZUTNIA] Wykryto zagrożenie! Odpalanie rakiety do: " + enemy);
                        missiles.add(new Interceptor(new Vector3(0,0,0), enemy));
                    }
                }
            }
        }

        private boolean isTargetEngaged(EnemyTarget target) {
            for (Interceptor m : missiles) {
                if (m.isActive() && m.target == target) return true;
            }
            return false;
        }
    }
}